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

-keep class kotlin.**
-keep class kotlinx.**
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

-dontwarn kotlin.**
-dontwarn kotlinx.**


-keep class com.explyt.jpa.ql.JpqlFileType$Companion { *; }
-keep class com.explyt.spring.core.language.profiles.ProfilesFileType$Companion { *; }
-keepclassmembers class * { public static ** INSTANCE; }

# otherwise fileTemplates folders will be removed
-keepdirectories

# keep attributes
#-keepattributes InnerClasses,LineNumberTable,*Annotation*,SourceFile,Signature,EnclosingMethod
# preserve all possible attributes
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,LocalVariable*Table,*Annotation*,Synthetic,EnclosingMethod

# we potentially could have desktop related logic with createUI method, which have to be saved
-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}

# Entry point to the app.
# all classes we can obfuscate
-keep,allowobfuscation class com.explyt.** { *; }

# here is list of extensionPoint classes, which we have to keep
# jpa
-keep class com.explyt.jpa.ql.reference.InputParameterReferenceResolver
# spring-core
-keep class com.explyt.spring.core.completion.properties.ConfigurationPropertiesLoader
-keep class com.explyt.spring.core.completion.properties.ConfigurationFactoriesNamesLoader
-keep class com.explyt.spring.core.profile.ProfileSearcher
-keep class com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
# spring-web
-keep class com.explyt.spring.web.loader.SpringWebEndpointsLoader

# here is list of class that persistence state to file (data mapping is broken)
-keep class com.explyt.spring.core.runconfiguration.SpringBootConfigurationOptions { *; }
-keep class com.explyt.spring.core.externalsystem.setting.** { *; }
-keep class com.explyt.spring.core.externalsystem.model.** { *; }

# serialized from JSON:
-keep class com.explyt.spring.core.externalsystem.process.BeanInfo { *; }
-keep class com.explyt.spring.core.externalsystem.process.AspectInfo { *; }

# plugin inspections will not work otherwise
-keepclassmembers class ** extends com.intellij.codeInspection.LocalInspectionTool {
   <fields>;
}

# classes which run in user context
-keep class com.explyt.spring.** { *; }
-dontwarn com.explyt.spring.**

# to reflect obfuscated class refs in plugin.xml
-adaptresourcefilecontents **.xml

# for stacktraces: todo: add mappings from retrace tool to mapping.txt file
#-printmapping build/mapping.txt
#-printmapping out.map

-dontwarn com.intellij.ui.mac.**
-dontwarn com.jetbrains.performancePlugin.**
-dontwarn com.jetbrains.codeInspection.**
-dontwarn com.jetbrains.rd.**
-dontwarn com.networknt.**
-dontwarn com.sun.tools.attach.**
-dontwarn io.kinference.core.operators.ml.trees.KICoreTreeEnsemble
-dontwarn kotlin.coroutines.**
-dontwarn org.gradle.internal.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.junit.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn reactor.blockhound.**
-dontwarn training.**

-keep class org.slf4j.**
-dontwarn org.slf4j.**

-keep class org.yaml.snakeyaml.** {
   public org.yaml.snakeyaml.** <init>(long);
}

# slf4j
-assumenosideeffects class * implements org.slf4j.Logger {
    public *** trace(...);
    public *** debug(...);
    public *** info(...);
    public *** warn(...);
    public *** error(...);
}
