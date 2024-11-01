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

package com.explyt.spring.core.service

import com.explyt.spring.core.profile.SpringProfilesService
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.util.*

@Service(Service.Level.PROJECT)
class ProfilesService(private val project: Project) {
    @Volatile
    var activeProfilesFromRunConfiguration: Set<String> = setOf()

    @Volatile
    var currentMainClass: String? = null

    private fun isActive(profile: String) = getActiveProfiles().contains(profile)

    fun updateFromConfiguration(settings: RunnerAndConfigurationSettings?): Boolean {
        val profiles = RunConfigurationUtil.getProfiles(settings)
        val profileChanged = profiles != activeProfilesFromRunConfiguration
        activeProfilesFromRunConfiguration = profiles

        val mainClassName = RunConfigurationUtil.getRunClassName(settings?.configuration)
        val mainClassChanged = mainClassName != currentMainClass
        currentMainClass = mainClassName

        return profileChanged || mainClassChanged
    }

    private fun getActiveProfiles(): Set<String> {
        if (activeProfilesFromRunConfiguration.isNotEmpty()) return activeProfilesFromRunConfiguration

        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getActiveProfilesFromCodeAndConfig(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getActiveProfilesFromCodeAndConfig(): Set<String> {
        val springProfilesService = SpringProfilesService.getInstance(project)
        return project.modules
            .flatMapTo(mutableSetOf()) { springProfilesService.loadActiveProfiles(it) }
            .ifEmpty { DEFAULT_PROFILES }
    }

    /**
     * Parses expression, with current [ProfilesService.activeProfiles] computes result
     */
    fun compute(profilesExpression: String): Boolean {
        val tokens = tokenize(profilesExpression) ?: return false
        val postfixExpression = toPostfix(tokens)

        return evaluate(postfixExpression) ?: false
    }

    private fun tokenize(expression: String): Collection<Token>? {
        val tokens = mutableListOf<Token>()

        val profileCharacters = mutableListOf<Char>()
        for (char in expression.replace(" ", "")) {
            if (char in SEPARATORS) {
                if (profileCharacters.isNotEmpty()) {
                    tokens.add(Profile(profileCharacters.joinToString("")))
                    profileCharacters.clear()
                }

                tokens.add(
                    when (char) {
                        '!' -> Operation.NOT
                        '&' -> Operation.AND
                        '|' -> Operation.OR
                        '(' -> Paren.LPAREN
                        ')' -> Paren.RPAREN
                        else -> return null
                    }
                )

            } else {
                profileCharacters += char
            }
        }
        if (profileCharacters.isNotEmpty()) {
            tokens.add(Profile(profileCharacters.joinToString("")))
            profileCharacters.clear()
        }

        return tokens
    }

    private fun toPostfix(tokens: Collection<Token>): Collection<Token> {
        val result = mutableListOf<Token>()
        val operationStack = LinkedList<Token>()

        for (token in tokens) {
            when (token) {
                is Profile -> result.add(token)
                is LParen -> operationStack.push(token)
                is RParen -> {
                    while (!operationStack.isEmpty() && operationStack.peek() !is LParen) {
                        result.add(operationStack.pop())
                    }
                    if (operationStack.isEmpty()) return emptyList() // no matching bracket
                    operationStack.pop()
                }

                is Not, is Or, is And -> {
                    val tokenPriority = getPriority(token)
                    var topElement = operationStack.peek()
                    while (!operationStack.isEmpty()
                        && (getPriority(topElement) >= tokenPriority)
                    ) {
                        result.add(operationStack.pop())
                        topElement = operationStack.peek()
                    }
                    operationStack.push(token)
                }

            }
        }
        while (!operationStack.isEmpty()) {
            result.add(operationStack.pop())
        }

        return result
    }

    private fun getPriority(token: Token): Int {
        return when (token) {
            is Not -> 2
            is And, is Or -> 1
            else -> 0
        }
    }

    private fun evaluate(postfixExpression: Collection<Token>): Boolean? {
        val computation = LinkedList<Boolean>()

        for (token in postfixExpression) {
            when (token) {
                is Profile ->
                    computation.push(isActive(token.name))

                is Not -> {
                    if (computation.isEmpty()) return null
                    computation.push(!computation.pop())
                }

                is And -> {
                    if (computation.size < 2) return null
                    val op1 = computation.pop()
                    val op2 = computation.pop()
                    computation.push(op1 && op2)
                }

                is Or -> {
                    if (computation.size < 2) return null
                    val op1 = computation.pop()
                    val op2 = computation.pop()
                    computation.push(op1 || op2)
                }

            }
        }

        if (computation.size != 1) return false
        return computation.pop()
    }

    interface Token
    interface Paren : Token {
        companion object {
            val LPAREN = LParen()
            val RPAREN = RParen()
        }
    }

    interface Operation : Token {
        companion object {
            val NOT = Not()
            val AND = And()
            val OR = Or()
        }
    }

    class Profile(val name: String) : Token
    class LParen : Paren
    class RParen : Paren
    class And : Operation
    class Or : Operation
    class Not : Operation

    companion object {
        private const val DEFAULT = "default"
        private val SEPARATORS = setOf('(', ')', '&', '|', '!')
        private val DEFAULT_PROFILES = setOf(DEFAULT)

        fun getInstance(project: Project): ProfilesService = project.service()
    }

}