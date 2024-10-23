package com.esprito.spring.web.editor.openapi

import com.esprito.spring.web.util.SpringWebUtil.OPEN_API
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.childrenOfType
import com.jetbrains.rd.util.concurrentMapOf
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import java.nio.file.Path
import java.util.*

object OpenApiUtils {
    private val cache = concurrentMapOf<UUID, String>()

    fun cacheFile(uuid: UUID, file: VirtualFile) {
        file.canonicalPath?.let {
            cache[uuid] = it
        }
    }

    fun getFile(uuid: UUID): VirtualFile? {
        return cache.getOrDefault(uuid, null)?.let {
            return VfsUtil.findFile(Path.of(it), true)
        }
    }

    fun isOpenApiFile(project: Project, file: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false

        return when (psiFile) {
            is YAMLFile -> isOpenApi(psiFile)
            is JsonFile -> isOpenApi(psiFile)
            else -> false
        }
    }

    fun isOpenApi(yamlFile: YAMLFile): Boolean {
        return YAMLUtil
            .getTopLevelKeys(yamlFile)
            .any { it.keyText == OPEN_API }
    }

    fun isOpenApi(jsonFile: JsonFile): Boolean {
        return jsonFile
            .topLevelValue
            ?.childrenOfType<JsonProperty>()
            ?.any { it.name == OPEN_API }
            ?: false
    }

    fun resourceUrl(specKey: UUID): String =
        "http://localhost:${BuiltInServerManager.getInstance().port}/__explyt-openapi?key=${specKey}&resource"

    fun resourceUrl(specKey: UUID, resource: String): String =
        "${resourceUrl(specKey)}=$resource"

    fun proxyUrl(): String =
        "http://localhost:${BuiltInServerManager.getInstance().port}$OPENAPI_INTERNAL_CORS"

    fun createPreviewAction(tag: String, operationId: String): AnAction {
        return object : AnAction({ "Open in UI" }, AllIcons.RunConfigurations.TestState.Run) {
            override fun actionPerformed(e: AnActionEvent) {
                val editor = PlatformDataKeys.FILE_EDITOR.getData(e.dataContext)
                    ?.let { OpenApiUIEditor.from(it) } ?: return
                editor.showPreviewFor(tag, operationId)
            }

        }
    }

    const val OPENAPI_INTERNAL_CORS = "/__explyt-openapi_internal-cors"
    const val OPENAPI_ORIGINAL_URL = "__explyt-openapi_original-url"
    const val OPENAPI_EDITOR_TYPE_ID = "esprito.web.openapi.ui.editor"
}
