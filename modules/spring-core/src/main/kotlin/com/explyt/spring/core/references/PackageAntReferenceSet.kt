/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPackage
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.CommonProcessors
import com.intellij.util.PatternUtil
import java.util.regex.Pattern


class PackageAntReferenceSet(
    packageName: String,
    element: PsiElement,
    startInElement: Int,
    private val scope: GlobalSearchScope = element.resolveScope
) :
    PackageReferenceSet(packageName, element, startInElement, scope) {

    override fun createReference(range: TextRange?, index: Int) =
        PackageAntReference(this, range, index, scope)
}

class PackageAntReference(
    set: PackageReferenceSet,
    range: TextRange?,
    index: Int,
    private val scope: GlobalSearchScope
) : PsiPackageReference(set, range, index) {

    override fun doMultiResolve(): Array<out ResolveResult> {
        val packages: MutableCollection<PsiPackage> = LinkedHashSet()
        for (parentPackage in context) {
            packages.addAll(resolvePackages(parentPackage))
        }
        return PsiElementResolveResult.createResults(packages)
    }

    private fun resolvePackages(context: PsiPackage?): Collection<PsiPackage> {
        if (context == null) return emptySet()

        val packageName = value
        val processor = getProcessor(packageName)

        if (packageName == "*") {
            for (subPackage in context.getSubPackages(scope)) {
                if (!processor.process(subPackage)) break
            }
            return processor.results
        }

        if (myIndex > 0 && referenceSet.getReference(myIndex - 1).value == ("**"))
            return getSubPackagesDeepCached(context, processor)
        if (packageName == "**")
            return getSubPackagesDeepCached(context, processor)

        if (packageName.contains("*") || packageName.contains("?")) {
            for (subPackage in context.getSubPackages(scope)) {
                processor.process(subPackage)
            }
            return processor.results
        }

        return referenceSet.resolvePackageName(context, packageName)
    }

    private fun getProcessor(packageName: String): CommonProcessors.CollectProcessor<PsiPackage> {
        val pattern: Pattern = PatternUtil.fromMask(packageName)
        return object : CommonProcessors.CollectProcessor<PsiPackage>(LinkedHashSet()) {
            override fun accept(psiPackage: PsiPackage): Boolean {
                val name = psiPackage.name
                return name != null && pattern.matcher(name).matches()
            }
        }
    }

    private fun getSubPackagesDeepCached(
        context: PsiPackage,
        processor: CommonProcessors.CollectProcessor<PsiPackage>,
    ): Collection<PsiPackage> {
        return CachedValuesManager.getManager(context.project)
            .getCachedValue(context) {
                CachedValueProvider.Result(
                    getSubPackagesDeep(context, processor),
                    ModificationTrackerManager.getInstance(context.project).getUastModelAndLibraryTracker()
                )
            }
    }

    private fun getSubPackagesDeep(
        context: PsiPackage,
        processor: CommonProcessors.CollectProcessor<PsiPackage>,
        maxDeep: Int = -1
    ): Collection<PsiPackage> {
        processSubPackages(context, processor, maxDeep)
        return processor.results
    }

    private fun processSubPackages(
        psiPackage: PsiPackage,
        processor: CommonProcessors.CollectProcessor<PsiPackage>,
        maxDeep: Int
    ) {
        // The check is not for <= on purpose so that you can set "infinite maximum depth via -1"
        if (maxDeep == 0) return

        for (subPackage in psiPackage.getSubPackages(scope)) {
            processor.process(subPackage)
            processSubPackages(subPackage, processor, maxDeep - 1)
        }
    }

}