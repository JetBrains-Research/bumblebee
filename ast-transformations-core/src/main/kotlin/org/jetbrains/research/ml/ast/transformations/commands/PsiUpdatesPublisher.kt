package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.psi.PsiElement

// Todo: make PsiUpdatesPublisher per PsiFile
object PsiUpdatesPublisher {
    data class UpdatedPsi(val oldPsi: PsiElement, val newPsi: PsiElement)

    /**
     * Stores all actions need to be performed when some Psi has changes from old to new
     */
    private val subscribedPsi: MutableMap<PsiElement, MutableList<(UpdatedPsi) -> Unit>> = hashMapOf()

    /**
     * Subscribes on [psiElement] update event, performs [onUpdate] once it happens
     */
    fun subscribe(psiElement: PsiElement, onUpdate: (UpdatedPsi) -> Unit) {
        subscribedPsi.getOrPut(psiElement, { arrayListOf() }).add(onUpdate)
    }

    /**
     * Notify all subscribers that some Psi is updated, so all stored onUpdates are called
     */
    fun notify(updatedPsi: UpdatedPsi) {
        subscribedPsi[updatedPsi.oldPsi]?.let {
            it.forEach { onUpdate -> onUpdate(updatedPsi) }
//          Change key from oldPsi to newPsi
            subscribedPsi[updatedPsi.newPsi] = it
            subscribedPsi.remove(updatedPsi.oldPsi)
        }
    }
}
