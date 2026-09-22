/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.beans.BeanApplicationResolver
import com.explyt.spring.core.service.beans.BeanLookupSelector
import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanQueryProblem
import com.explyt.spring.core.service.beans.ScopedBeanInjectionResolver
import com.explyt.spring.core.service.beans.ScopedBeanMatcher
import com.explyt.spring.core.service.beans.SpringInjectionPoint
import com.explyt.spring.core.service.beans.SpringInjectionPointResolver
import com.explyt.spring.ai.mcp.McpProjectChoice
import com.explyt.spring.ai.mcp.McpProjectResolver
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs one bean query end to end and hands back the finished JSON.
 *
 * Everything that touches PSI - the snapshot, the match, the projection of each candidate the page actually
 * serves - happens inside a single smart read action, and only a String leaves it. A projection deferred past
 * that boundary would dereference PSI the platform is free to have invalidated by then.
 *
 * Only an expected selection problem is turned into an error document. Cancellation and infrastructure failures
 * propagate: reporting them as an empty result would read as "this application has no such bean".
 */
class BeanLookupService(private val project: Project) {

    suspend fun query(request: BeanLookupRequest, page: BeanPageRequest): String {
        val writer = BoundedBeanResponseWriter()
        return try {
            request.validate()
            withContext(Dispatchers.IO) {
                smartReadAction(project) { answer(request, page, writer) }
            }
        } catch (e: BeanQueryException) {
            writer.writeError(e.problem, page.maxChars.coerceIn(MIN_ERROR_BUDGET, MAX_ERROR_BUDGET))
        }
    }

    private fun answer(request: BeanLookupRequest, page: BeanPageRequest, writer: BoundedBeanResponseWriter): String {
        val injectionFile = request.filePath?.let { resolveFile(it) }
        val application = BeanApplicationResolver(project).resolve(request.applicationClassName, injectionFile)
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, request.source(), request.contextId, injectionFile)

        val injection = injectionFile?.let {
            SpringInjectionPointResolver(project).resolve(it, request.line!!, request.column)
        }
        val selection = when (injection) {
            null -> ScopedBeanMatcher(project).lookup(snapshot, request.selector())
            else -> ScopedBeanInjectionResolver(project).resolve(snapshot, injection)
        }

        val content = BeanResponseMapper(project)
            .map(snapshot, selection, request.selector(injection), injection, request.includeDetails)
        val revision = BeanQueryRevision.compute(snapshot.modelStamp, request.normalizedQuery())
        return writer.write(content, revision, page)
    }

    private fun BeanLookupRequest.selector() = BeanLookupSelector(typeFqn, beanName)

    /** An injection answer has no lookup selector, and passing one would label a candidate with a name nobody asked for. */
    private fun BeanLookupRequest.selector(injection: SpringInjectionPoint?) =
        if (injection == null) selector() else null

    /**
     * Resolves a project-relative path without following it out of the project.
     *
     * The roots of the project are what the path is resolved against, not its base path on disk: a project's
     * content may live on another file system than its base directory does, and a path resolved through the
     * local file system alone would miss it. Each candidate is then required to sit under the root it came
     * from, so a path climbing out with `..` resolves to nothing rather than to a file the caller was never
     * scoped to.
     */
    private fun resolveFile(filePath: String): PsiFile {
        val relative = filePath.trim().removePrefix("/")
        if (relative.isEmpty()) throw fileNotFound("'$filePath' names no file.")

        val file = project.roots()
            .firstNotNullOfOrNull { root ->
                root.findFileByRelativePath(relative)?.takeIf { VfsUtilCore.isAncestor(root, it, false) }
            }
            ?: throw fileNotFound("No file '$filePath' in the project.")

        return PsiManager.getInstance(project).findFile(file)
            ?: throw fileNotFound("'$filePath' is not a source file the IDE can read.")
    }

    /** The base directory first, so a path written against the project root wins over a same-named module path. */
    private fun Project.roots(): List<VirtualFile> = buildList {
        basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }?.let(::add)
        addAll(ProjectRootManager.getInstance(this@roots).contentRoots)
    }

    private fun fileNotFound(message: String) = BeanQueryException(BeanQueryProblem(FILE_NOT_FOUND, message))

    companion object {
        const val PROJECT_NOT_FOUND = "PROJECT_NOT_FOUND"
        const val FILE_NOT_FOUND = "FILE_NOT_FOUND"

        private const val MIN_ERROR_BUDGET = BoundedBeanResponseWriter.FALLBACK_BUDGET
        private const val MAX_ERROR_BUDGET = BoundedBeanResponseWriter.MAX_CHARS

        fun getInstance(project: Project): BeanLookupService = BeanLookupService(project)

        /**
         * The project this query is about, reported through the tool's own error channel.
         *
         * The rule itself lives in [McpProjectResolver] so that every tool applies the same one; only the
         * shape of the refusal is this tool's own.
         */
        fun resolveProject(projectPath: String?): Project =
            when (val choice = McpProjectResolver.resolve(projectPath)) {
                is McpProjectChoice.Resolved -> choice.project
                is McpProjectChoice.NotFound -> throw projectNotFound(choice.message)
                is McpProjectChoice.Ambiguous -> throw projectNotFound(choice.message)
            }

        private fun projectNotFound(message: String) =
            BeanQueryException(BeanQueryProblem(PROJECT_NOT_FOUND, message))
    }
}
