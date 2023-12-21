package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.providers.FileReferenceProvider
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.SIMPLE_VALUE
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.VALUE_LOCATIONS
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.VALUE_SCRIPTS
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider
import org.jetbrains.uast.UExpression
import org.jetbrains.yaml.YAMLFileType

class FileReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()

        val allFileProvider = FileReferenceProvider()
        val providerYmlProperties = FileReferenceProvider(arrayOf(YAMLFileType.YML, PropertiesFileType.INSTANCE))
        val providerXml = FileReferenceProvider(arrayOf(YAMLFileType.YML, PropertiesFileType.INSTANCE))

        addAnnotationValueContributor(
            registrar, injection, SpringCoreClasses.PROPERTY_SOURCE, providerYmlProperties, SIMPLE_VALUE
        )
        addAnnotationValueContributor(
            registrar, injection, SpringCoreClasses.TEST_PROPERTY_SOURCE, providerYmlProperties, VALUE_LOCATIONS
        )
        SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_XML
            .forEach { addAnnotationValueContributor(registrar, injection, it, providerXml, VALUE_LOCATIONS) }
        SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_SQL
            .forEach { addAnnotationValueContributor(registrar, injection, it, allFileProvider, VALUE_SCRIPTS) }
        addAnnotationValueContributor(
            registrar, injection, SpringCoreClasses.VALUE, allFileProvider, SIMPLE_VALUE
        )

        val resourceMethodPattern = PsiJavaPatterns.psiMethod().withName("getResource")
            .definedInClass(SpringCoreClasses.RESOURCE_LOADER)
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(0, resourceMethodPattern, true),
            allFileProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )

        val resourcesMethodPattern = PsiJavaPatterns.psiMethod().withName("getResources")
            .definedInClass(SpringCoreClasses.RESOURCE_LOADER_RESOLVER)
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(0, resourcesMethodPattern, true),
            allFileProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
        val resourceUtilsMethodPattern = PsiJavaPatterns.psiMethod()
            .definedInClass(SpringCoreClasses.RESOURCE_UTILS)
            .withParameters(String::class.java.name)
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(0, resourceUtilsMethodPattern, true),
            allFileProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
    }

    private fun addAnnotationValueContributor(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        className: String,
        provider: FileReferenceProvider,
        parameterNames: List<String>
    ) {
        parameterNames.forEach {
            registrar.registerUastReferenceProvider(
                injection.annotationParam(className, it), provider, PsiReferenceRegistrar.LOWER_PRIORITY
            )
        }
    }

    object AnnotationParamsConstants {
        val SIMPLE_VALUE = listOf("value")
        val VALUE_LOCATIONS = listOf("value", "locations")
        val VALUE_SCRIPTS = listOf("value", "scripts")
    }

}