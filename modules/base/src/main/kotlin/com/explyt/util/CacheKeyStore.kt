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

package com.explyt.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ConcurrentFactoryMap
import java.util.*

@Service(Service.Level.PROJECT)
class CacheKeyStore : Disposable {

    companion object {
        /**
         * You can use `CacheKeyStore.cacheReset.incModificationCount()` in debugger to reset caches, that stores keys with [getKeys] method.
         */
        @JvmField
        val cacheReset: SimpleModificationTracker = SimpleModificationTracker()

        @JvmStatic
        fun getInstance(project: Project): CacheKeyStore = project.getService(CacheKeyStore::class.java)

        /**
         * Returns the map of Key<CachedValue> that will be recreated after every project sync.
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> getKeys(project: Project, dataHolder: UserDataHolder): MutableMap<String, Key<CachedValue<T>>> {
            val keyStore = getInstance(project)

            return CachedValuesManager
                .getManager(project)
                .getCachedValue(dataHolder, keyStore.metaKey as Key<CachedValue<MutableMap<String, Key<CachedValue<T>>>>>, {
                    val dependencies = mutableListOf<Any>()
                    dependencies.add(cacheReset)
                    dependencies.addAll(keyStore.getAdditionalCachedProviderDependencies())
                    CachedValueProvider.Result.create(ConcurrentFactoryMap.createMap { Key.create(it) }, dependencies)
                }, false)
        }
    }

    private val singleKeyStore: MutableMap<Any, Key<CachedValue<*>>> = WeakHashMap()

    private val metaKey: Key<CachedValue<*>> = Key.create(javaClass.canonicalName)

    fun <T> getKey(value: Any): Key<CachedValue<T>> {
        synchronized(this) {
            val key = singleKeyStore.getOrPut(value) {
                Key.create(value.toString())
            }

            @Suppress("UNCHECKED_CAST")
            return key as Key<CachedValue<T>>
        }
    }

    fun getAdditionalCachedProviderDependencies(): Collection<Any> = listOf()

    override fun dispose() {
        singleKeyStore.clear()
    }
}