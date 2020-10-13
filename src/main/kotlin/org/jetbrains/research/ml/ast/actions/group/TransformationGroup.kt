package org.jetbrains.research.ml.ast.actions.group

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

class TransformationGroup: DefaultActionGroup() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}