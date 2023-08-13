package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.providers.FileReferenceInAnnotationProvider
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import org.jetbrains.yaml.YAMLFileType

class FileReferenceInAnnotationContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            FileReferenceInAnnotationProvider(
                SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES,
                arrayOf(YAMLFileType.YML, PropertiesFileType.INSTANCE)
            )
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            FileReferenceInAnnotationProvider(
                SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_XML,
                arrayOf(XmlFileType.INSTANCE)
            )
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            FileReferenceInAnnotationProvider(
                SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_SQL,
                arrayOf()
            )
        )
    }
}