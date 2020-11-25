package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyNamedParameter
import com.jetbrains.python.psi.PyPossibleClassMember
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.search.PySuperMethodsSearch
import kotlin.test.fail

class ElementAnonymizer {
    private val allRenames: MutableList<Pair<PsiElement, String>> = mutableListOf()
    private val elementToNewName: MutableMap<PsiElement, String?> = mutableMapOf()
    private val parentToKindCounter: MutableMap<PsiElement?, NamedEntityKindCounter> = mutableMapOf()

    fun registerElement(element: PsiElement) {
        if (isDefinition(element)) {
            elementToNewName.getOrPut(element) {
                computeNewNameForElement(element)?.also { newName ->
                    allRenames.add(element to newName)
                }
            }
        }
    }

    fun getAllRenames(): List<Pair<PsiElement, String>> = allRenames

    private fun getNewNameForElement(element: PsiElement): String? =
        elementToNewName.getOrPut(element) { computeNewNameForElement(element) }

    private fun getScopeName(element: PsiElement): String =
        getNewNameForElement(element) ?: (element as PyElement).name!!.removePrefix("__")

    private fun computeNewNameForElement(element: PsiElement): String? {
        if (!isDefinition(element)) {
            return when (element) {
                is PyLambdaExpression -> assembleNewFullName(computeParentOfDefinition(element), NamedEntityKind.Lambda)
                is PyFunction -> getNewNameForElement(PySuperMethodsSearch.findDeepestSuperMethod(element))
                else -> fail("A new name for a non-definition requested")
            }
        }
        if (!shouldRenameDefinition(element)) return null
        val parent = computeParentOfDefinition(element)
        val definitionKind = NamedEntityKind.getElementKind(element) ?: return null
        return assembleNewFullName(parent, definitionKind).also { newName ->
            for (reference in ReferencesSearch.search(element, element.useScope)) {
                elementToNewName[reference.element] = newName
            }
        }
    }

    private fun assembleNewFullName(parent: PsiElement?, kind: NamedEntityKind): String {
        val prefix = parent?.let { getScopeName(it) + "_" } ?: ""
        val kindCount = parentToKindCounter.getOrPut(parent) { NamedEntityKindCounter() }.next(kind)
        return "$prefix${kind.prefix}$kindCount"
    }

    private fun shouldRenameDefinition(definition: PsiElement): Boolean {
        fun isMethod(function: PyFunction?): Boolean = function?.containingClass != null

        val name = (definition as PyElement).name?.also {
            // Ignore all names starting with two underscores
            if (it.startsWith("__")) return false
        } ?: return false
        // Do not rename method parameters with special names
        if (definition is PyNamedParameter &&
            isMethod(definition.parentOfType()) &&
            name in listOf("self", "cls")
        ) {
            return false
        }

        return true
    }

    private fun isElementGlobal(element: PsiElement): Boolean =
        (element as? PyPossibleClassMember)?.containingClass == null &&
            element !is PyNamedParameter &&
            element.useScope !is LocalSearchScope &&
            element.parentOfType<PyLambdaExpression>() == null

    private fun computeParentOfDefinition(definition: PsiElement): PsiElement? =
        if (!isElementGlobal(definition)) ScopeUtil.getScopeOwner(definition) else null

    private fun isDefinition(element: PsiElement): Boolean {
        return element is PyClass ||
            // Only consider the base method the definition
            element is PyFunction && PySuperMethodsSearch.findDeepestSuperMethod(element) == element ||
            element is PyNamedParameter ||
            element is PyTargetExpression
    }
}
