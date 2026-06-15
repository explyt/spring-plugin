/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.rd.util.getOrCreate
import java.nio.file.Path
import kotlin.io.path.absolutePathString


@Service(Service.Level.APP)
@State(name = "Explyt.Http.State", category = SettingsCategory.PLUGINS, storages = [Storage("explyt.http.xml")])
class HttpFileStateService : SimplePersistentStateComponent<HttpFilesHolder>(HttpFilesHolder()) {

    @Transient
    fun getOrCreateState(file: Path): HttpFileState {
        return state.stateByFilePath.getOrCreate(file.absolutePathString()) { HttpFileState() }
    }

    companion object {
        fun getInstance(): HttpFileStateService = service()
    }
}

class HttpFilesHolder : BaseState() {
    var stateByFilePath by map<String, HttpFileState>()
}

class HttpFileState {
    var filesPathByName = mutableMapOf<String, String>()
    var selectedFileName = ""
    var selectedEnv = ""
    var additionalArgs = ""
}