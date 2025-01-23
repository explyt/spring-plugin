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

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.intellij.codeInsight.daemon.GutterMark
import junit.framework.TestCase
import org.intellij.lang.annotations.Language

class ConfigurationPropertyLineMarkerProviderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    fun testNoGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.wrongName=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private String value;
    
                public String getValue() {
                    return value;
                }
    
                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        TestCase.assertEquals(
            listOf<GutterMark>(),
            gutterMarks
        )
    }

    fun testConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.value=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private String value;
    
                public String getValue() {
                    return value;
                }
    
                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"),
            gutterTargetsStrings
        )
    }

    fun testNestedInConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.nested.value=2"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
                private NestedProperties nested;
                
                public NestedProperties getNested() {
                    return nested;
                }

                public void setNested(final NestedProperties nested) {
                    this.nested = nested;
                }
            }
            
            class NestedProperties {
                private String value;

                public String getValue() {
                    return value;
                }

                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.nested.value"),
            gutterTargetsStrings
        )
    }

    fun testNestedInBeanProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.value=3"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class MainPropertiesConfiguration {
                @ConfigurationProperties(prefix="configuration")
                @Bean
                public NestedProperties getNested() {
                    return new NestedProperties();
                }
            }
            
            class NestedProperties {
                private String value;

                public String getValue() {
                    return value;
                }

                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"),
            gutterTargetsStrings
        )
    }

    fun testMapKeyGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value.key.one=1
               configuration.value.key-two=2""".trimMargin()
        )

        @Language("JAVA")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.Map;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private Map<String, String> value;
    
                public Map<String, String> getValue() {
                    return value;
                }
    
                public void setValue(Map<String, String> value) {
                    this.value = value;
                }
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value.key.one", "configuration.value.key-two"),
            gutterTargetsStrings
        )
    }

    fun testPropertiesGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value.key.one=1
               configuration.value.key-two=2""".trimMargin()
        )

        @Language("JAVA")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.Properties;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private Properties value;
    
                public Properties getValue() {
                    return value;
                }
    
                public void setValue(Properties value) {
                    this.value = value;
                }
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value.key.one", "configuration.value.key-two"),
            gutterTargetsStrings
        )
    }

    fun testListGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value1[0]=1
               configuration.value1[1]=2
               configuration.value2=1,2,3""".trimMargin()
        )

        @Language("JAVA")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.List;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private List<String> value1;
                private List<String> value2;
    
                public List<String> getValue1() {
                    return value1;
                }
    
                public void setValue1(List<String> value) {
                    this.value1 = value;
                }
                
                public List<String> getValue2() {
                    return value2;
                }
    
                public void setValue2(List<String> value) {
                    this.value2 = value;
                }
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(
            setOf("configuration.value1[0]", "configuration.value1[1]", "configuration.value2"),
            gutterTargetsStrings.toSet()
        )
    }

    fun testArrayGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value1[0]=1
               configuration.value1[1]=2
               configuration.value2=1,2,3""".trimMargin()
        )

        @Language("JAVA")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.List;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private Integer[] value1;
                private Integer[] value2;
    
                public Integer[] getValue1() {
                    return value1;
                }
    
                public void setValue1(Integer[] value) {
                    this.value1 = value;
                }
                
                public Integer[] getValue2() {
                    return value2;
                }
    
                public void setValue2(Integer[] value) {
                    this.value2 = value;
                }
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(
            setOf("configuration.value1[0]", "configuration.value1[1]", "configuration.value2"),
            gutterTargetsStrings.toSet()
        )
    }

    fun testConfigurationPropertiesBean() {
        myFixture.configureByText(
            "ConfigurationPropertiesBean.java",
            """
                @org.springframework.boot.context.properties.ConfigurationProperties
                public class ConfigurationPropertiesBean {                    
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        TestCase.assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testMapInConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "main.contexts.sample1=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.Map;

            @ConfigurationProperties(prefix="main")
            @Configuration
            public class MainPropertiesConfiguration {
                private Map<String, Integer> contexts;
                
                public Map<String, Integer> getContexts() {
                    return contexts;
                }

                public void setContexts(Map<String, Integer> contexts) {
                    this.contexts = contexts;
                }
            }
            """.trimIndent()
        )

        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(gutterTargetsStrings.filter { it == "main.contexts.sample1" }.size, 2)
    }

    fun testMapInConfigurationProperties_yaml() {
        myFixture.addFileToProject(
            APPLICATION_YAML_FILE_NAME,
            """
main:
  contexts:
    sample1: 11                
            """.trimIndent()
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;
            import java.util.Map;

            @ConfigurationProperties(prefix="main")
            @Configuration
            public class MainPropertiesConfiguration {
                private Map<String, Integer> contexts;
                
                public Map<String, Integer> getContexts() {
                    return contexts;
                }

                public void setContexts(Map<String, Integer> contexts) {
                    this.contexts = contexts;
                }
            }
            """.trimIndent()
        )

        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(gutterTargetsStrings.filter { it == "11" }.size, 2)
    }

    fun testHintInConfigurationProperties() {
        myFixture.addFileToProject(
            ADDITIONAL_METADATA_FILE_NAME,
            """
{
  "hints": [
    {
      "name": "main.mime-type",
      "providers": [
        {
          "name": "handle-as",
          "parameters": {
            "target": "org.springframework.util.MimeType"
          }
        }
      ]
    }
  ]
}
            """.trimIndent()
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="main")
            @Configuration
            public class MainPropertiesConfiguration {
                private String mimeType;
                
                public String getMimeType() {
                    return mimeType;
                }

                 public void setMimeType(String mimeType) {
                    this.mimeType = mimeType;
                }
            }
            """.trimIndent()
        )

        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(gutterTargetsStrings.filter { it == "\"main.mime-type\"" }.size, 2)
    }

    fun testConfigurationPropertiesInRecordJava() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            """
dgis.api-keys.test=text
dgis.default-api-key-name=key
            """.trimIndent()
        )

        myFixture.configureByText(
            "DgisConfigurationProperties.java",
            """
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties("dgis")
public record DgisConfigurationProperties(
        Map<String, String> apiKeys,
        String defaultApiKeyName
) {}
            """.trimIndent()
        )
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringSetting)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString
                .flatMap { gutter -> gutter.filter { it == "dgis.api-keys.test" } }.size,
            1
        )
        assertEquals(
            gutterTargetString
                .flatMap { gutter -> gutter.filter { it == "dgis.default-api-key-name" } }.size,
            1
        )
    }

    companion object {
        const val APPLICATION_PROPERTIES_FILE_NAME = "application.properties"
        const val APPLICATION_YAML_FILE_NAME = "application.yaml"
        const val ADDITIONAL_METADATA_FILE_NAME = "META-INF/additional-spring-configuration-metadata.json"
    }

}