/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

/**
 * Sticky group for the one-time feedback nudge.
 *
 * A regular `BALLOON` group expires after ten seconds; the nudge is shown once per installation, so it must stay
 * on screen until the user acts on it or closes it.
 */
val SpringFeedbackNotificationGroup: NotificationGroup by lazy {
    NotificationGroupManager.getInstance().getNotificationGroup("com.explyt.spring.feedback")
}
