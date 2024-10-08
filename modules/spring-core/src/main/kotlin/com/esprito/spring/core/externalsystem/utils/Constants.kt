package com.esprito.spring.core.externalsystem.utils

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import java.util.*

object Constants {
    const val SPRING_BOOT_NATIVE_ID = "ExplytSpringBoot"
    val SYSTEM_ID = ProjectSystemId(SPRING_BOOT_NATIVE_ID.uppercase(Locale.getDefault()), SPRING_BOOT_NATIVE_ID)
}