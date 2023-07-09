package com.esprito.jpql

import com.intellij.lang.Language

class JpqlLanguage : Language("JPQL") {

    companion object {
        @JvmField
        val INSTANCE = JpqlLanguage();
    }
}