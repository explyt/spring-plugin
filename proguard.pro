-dontshrink
-dontoptimize
#-dontpreverify

-verbose

# required for kotlin
-keep class kotlin.Metadata
-keep class kotlin.reflect.**
-keep class kotlin.reflect.jvm.internal.** { *; }
-keepclassmembers enum * { public *; }
-keep class **$DefaultImpls { *; }

-dontwarn kotlin.**
-dontwarn kotlinx.**


-keep class com.esprito.jpa.ql.JpqlFileType$Companion { *; }
-keep class com.esprito.spring.core.language.profiles.ProfilesFileType$Companion { *; }
-keepclassmembers class * { public static ** INSTANCE; }

# otherwise fileTemplates folders will be removed
-keepdirectories

# keep attributes
-keepattributes InnerClasses,LineNumberTable,*Annotation*,SourceFile,Signature,EnclosingMethod

# we potentially could have desktop related logic with createUI method, which have to be saved
-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}

# Entry point to the app.
#-keep class com.esprito.** { *; }

# all classes we can obfuscate
-keep,allowobfuscation class com.esprito.** { *; }

# here is list of extensionPoint classes, which we have to keep
# jpa
-keep class com.esprito.jpa.ql.reference.InputParameterReferenceResolver
# spring-core
-keep class com.esprito.spring.core.completion.properties.ConfigurationPropertiesLoader
-keep class com.esprito.spring.core.completion.properties.ConfigurationFactoriesNamesLoader
-keep class com.esprito.spring.core.profile.ProfileSearcher
-keep class com.esprito.spring.core.service.beans.discoverer.StaticBeansDiscoverer
# spring-data
-keep class com.esprito.jpa.ql.reference.InputParameterReferenceResolver
#spring-web
-keep class com.esprito.spring.web.references.contributors.SpringOpenApiUrlEndpointReferenceContributor

# plugin inspections will not work otherwise
-keepclassmembers class ** extends com.intellij.codeInspection.LocalInspectionTool {
   <fields>;
}

# to reflect obfuscated class refs in plugin.xml
-adaptresourcefilecontents **.xml

# for stacktraces: todo: add mappings from retrace tool to mapping.txt file
#-printmapping build/mapping.txt
#-printmapping out.map

-dontwarn com.intellij.ui.mac.**
-dontwarn com.jetbrains.performancePlugin.**
-dontwarn com.jetbrains.rd.**
-dontwarn io.kinference.core.operators.ml.trees.KICoreTreeEnsemble
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.junit.**
-dontwarn reactor.blockhound.**
-dontwarn training.**
