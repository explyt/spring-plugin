package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.CLASS_PATH_RESOURCE
import com.esprito.spring.core.SpringCoreClasses.FILE_URL_RESOURCE
import com.esprito.spring.core.SpringCoreClasses.RESOURCE_CLASS
import com.esprito.spring.core.SpringCoreClasses.RESOURCE_CLASS_LOADER
import com.esprito.spring.core.SpringCoreClasses.RESOURCE_LOADER
import com.esprito.spring.core.SpringCoreClasses.RESOURCE_LOADER_RESOLVER
import com.esprito.spring.core.SpringCoreClasses.URL_RESOURCE
import com.esprito.spring.core.SpringProperties.GET_RESOURCE
import com.esprito.spring.core.SpringProperties.GET_RESOURCES
import com.esprito.spring.core.providers.FileReferenceProvider
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.SIMPLE_VALUE
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.VALUE_LOCATIONS
import com.esprito.spring.core.references.contributors.FileReferenceContributor.AnnotationParamsConstants.VALUE_SCRIPTS
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.patterns.uast.uExpression
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

        registerResource(registrar, injection, allFileProvider)
    }

    private fun registerResource(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        allFileProvider: FileReferenceProvider
    ) {
        registerMethodResource(registrar, injection, allFileProvider, GET_RESOURCE, RESOURCE_LOADER)
        registerMethodResource(registrar, injection, allFileProvider, GET_RESOURCE, RESOURCE_CLASS)
        registerMethodResource(registrar, injection, allFileProvider, GET_RESOURCE, RESOURCE_CLASS_LOADER)

        registerMethodResource(registrar, injection, allFileProvider, GET_RESOURCES, RESOURCE_LOADER_RESOLVER)

        registerConstructorResource(registrar, allFileProvider, CLASS_PATH_RESOURCE)
        registerConstructorResource(registrar, allFileProvider, FILE_URL_RESOURCE)
        registerConstructorResource(registrar, allFileProvider, URL_RESOURCE)

        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .definedInClass(SpringCoreClasses.RESOURCE_UTILS)
                    .withParameters(String::class.java.name),
                true
            ),
            allFileProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
    }

    private fun registerMethodResource(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        allFileProvider: FileReferenceProvider,
        methodName: String,
        className: String
    ) {
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .withName(methodName)
                    .definedInClass(className),
                true
            ),
            allFileProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
    }

    private fun registerConstructorResource(
        registrar: PsiReferenceRegistrar,
        allFileProvider: FileReferenceProvider,
        className: String
    ) {
        registrar.registerUastReferenceProvider(
            uExpression().constructorParameter(0, className),
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