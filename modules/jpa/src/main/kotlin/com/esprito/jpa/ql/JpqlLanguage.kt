package com.esprito.jpa.ql

import com.intellij.lang.Language

class JpqlLanguage : Language("JPQL") {

    companion object {
        @JvmField
        val INSTANCE = JpqlLanguage()
    }
}