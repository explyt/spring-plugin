package com.explyt.spring.core.action

import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test

class Properties2YamlActionTest {
    @Test
    fun testSimpleYml2Prop() {
        @Language("YAML") val yamlString = """
spring:
  profiles:
    active: enterprise-prod

  aop:
    auto: true
    proxy-target-class: false
    custom: sample
        """.trimIndent()

        testYml2Prop(yamlString)
    }

    @Test
    fun testArrayYml2Prop() {
        @Language("YAML") val yamlString = """
roles_and_permissions:
  - role: admin
    permissions:
      - read
      - write
      - delete
  - role: user
    permissions: [read, write]
        """.trimIndent()

        testYml2Prop(yamlString)
    }

    @Test
    fun testSimpleProp2Yaml() {
        @Language("Properties") val propString = """
spring.profiles.active=enterprise-prod
spring.aop.auto=true
spring.aop.proxy-target-class=false
spring.aop.custom=sample
        """.trimIndent()
        testProp2Yaml(propString)
    }

    @Test
    fun testArrayProp2Yaml() {
        @Language("Properties") val propString = """
roles_and_permissions[0].role=admin
roles_and_permissions[0].permissions[0]=read
roles_and_permissions[0].permissions[1]=write
roles_and_permissions[0].permissions[2]=delete
roles_and_permissions[1].role=user
roles_and_permissions[1].permissions[0]=read
roles_and_permissions[1].permissions[1]=write
        """.trimIndent()
        testProp2Yaml(propString)
    }

    private fun testYml2Prop(content: String) {
        val yamlMap = Yaml2PropertiesAction.readYaml(null, content)
        Assert.assertFalse(yamlMap.isEmpty())
        val propertyString = Yaml2PropertiesAction.mapToProperty(null, yamlMap)
        val propertyMap = Properties2YamlAction.readProperty(null, propertyString)
        Assert.assertEquals(toHashMap(yamlMap), toHashMap(propertyMap))
    }

    private fun testProp2Yaml(content: String) {
        val propertyMap = Properties2YamlAction.readProperty(null, content)
        Assert.assertFalse(propertyMap.isEmpty())
        val propertyString = Properties2YamlAction.mapToYaml(null, propertyMap)
        val yamlMap = Yaml2PropertiesAction.readYaml(null, propertyString)
        Assert.assertEquals(toHashMap(yamlMap), toHashMap(propertyMap))
    }

    private fun toHashMap(nestedMap: Map<String, Any?>): HashMap<String, Any?> {
        val result = HashMap<String, Any?>()
        toHashMap(nestedMap, "", result)
        return result
    }

    private fun toHashMap(
        nestedMap: Map<String, Any?>,
        prefix: String,
        result: java.util.HashMap<String, Any?>
    ) {
        nestedMap.forEach { (key: Any?, value: Any?) ->
            val fullKey = (if (prefix.isEmpty()) key else "$prefix.$key") as String
            if (value is Map<*, *>) {
                toHashMap(value as Map<String, Any?>, fullKey, result)
            } else if (value is List<*>) {
                for (each in value) {
                    if (each is Map<*, *>) {
                        toHashMap(each as Map<String, Any?>, fullKey, result)
                    } else {
                        result[fullKey] = each.toString()
                    }
                }
            } else {
                result[fullKey] = value.toString()
            }
        }
    }
}