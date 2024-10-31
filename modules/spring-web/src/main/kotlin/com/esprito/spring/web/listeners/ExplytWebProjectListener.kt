package com.esprito.spring.web.listeners

import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class ExplytWebProjectListener : ProjectManagerListener {
    override fun projectClosing(project: Project) {
        SpringWebUtil.dropRegexesByUri()
    }
}