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