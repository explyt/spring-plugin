package com.explyt.spring.core.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

val SpringToolNotificationGroup: NotificationGroup by lazy {
    NotificationGroupManager.getInstance().getNotificationGroup("com.explyt.spring.notification")
}