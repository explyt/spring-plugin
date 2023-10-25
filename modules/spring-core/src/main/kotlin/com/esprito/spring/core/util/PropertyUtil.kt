package com.esprito.spring.core.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage

object PropertyUtil {
    const val DOT = "."

    data class InfoPackage (
        val psiPackage: PsiPackage?,
        val range: TextRange,
        val name: String
    )

    fun getClassNameByQualifiedName(qualifiedNameClass: String?): String {
        return qualifiedNameClass?.substringAfterLast(DOT) ?: ""
    }

    fun getPackageNameByQualifiedName(qualifiedNameClass: String?): String {
        return qualifiedNameClass?.substringBeforeLast(DOT) ?: ""
    }

    fun getPackages(module: Module, qualifiedNameClass: String?): List<InfoPackage> {
        if (qualifiedNameClass.isNullOrBlank()) {
            return emptyList()
        }
        val infoPackages = mutableListOf<InfoPackage>()
        val qualifiedNameList = qualifiedNameClass.split(DOT)
        var qualifiedNamePackage = ""
        for (index in qualifiedNameList.indices) {
            if (index == qualifiedNameList.size - 1) continue
            if (qualifiedNamePackage.isNotBlank()) {
                qualifiedNamePackage += DOT
            }
            qualifiedNamePackage += qualifiedNameList[index]
            val psiPackage = JavaPsiFacade.getInstance(module.project).findPackage(qualifiedNamePackage)

            val firstIndex = qualifiedNameClass.indexOf(qualifiedNameList[index])
            val range = TextRange(firstIndex, firstIndex + qualifiedNameList[index].length)

            infoPackages += InfoPackage(psiPackage, range, qualifiedNameList[index])
        }
        return infoPackages
    }

}
