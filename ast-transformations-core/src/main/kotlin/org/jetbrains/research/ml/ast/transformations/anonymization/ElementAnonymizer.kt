package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.search.PySuperMethodsSearch
import kotlin.test.fail

class ElementAnonymizer {
    private val allRenames: MutableMap<PyElement, String> = mutableMapOf()
    private val elementToNewName: MutableMap<PyElement, String?> = mutableMapOf()
    private val parentToKindCounter: MutableMap<PyElement?, NamedEntityKindCounter> = mutableMapOf()

    fun registerIfNeeded(element: PyElement) {
        if (element.isDefinition()) {
            elementToNewName.getOrPut(element) {
                computeNewNameForElement(element)?.also { newName ->
                    allRenames[element] = newName
                }
            }
        }
    }

    fun getAllRenames(): Map<PyElement, String> = allRenames

    private fun getNewNameForElement(element: PyElement): String? =
        elementToNewName.getOrPut(element) { computeNewNameForElement(element) }

    private fun getScopeName(element: PyElement): String =
        getNewNameForElement(element) ?: (element as? PyElement)?.name?.removePrefix("__") ?: ""

    private fun computeNewNameForElement(element: PyElement): String? {
        if (!element.isDefinition()) {
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
                elementToNewName[reference.element as PyElement] = newName
            }
        }
    }

    private fun assembleNewFullName(parent: PyElement?, kind: NamedEntityKind): String {
        val prefix = parent?.let { getScopeName(it) + "_" } ?: ""
        val kindCount = parentToKindCounter.getOrPut(parent) {
            NamedEntityKindCounter()
        }.next(kind)
        return "$prefix${kind.prefix}$kindCount"
    }

    private fun shouldRenameDefinition(definition: PyElement): Boolean {
        fun isMethod(function: PyFunction?): Boolean = function?.containingClass != null

        val name = definition.name?.also {
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

    private fun isElementGlobal(element: PyElement): Boolean =
        (element as? PyPossibleClassMember)?.containingClass == null &&
            element !is PyNamedParameter &&
            element.useScope !is LocalSearchScope &&
            element.parentOfType<PyLambdaExpression>() == null

    private fun computeParentOfDefinition(definition: PyElement): PyElement? =
        if (!isElementGlobal(definition)) ScopeUtil.getScopeOwner(definition) else null
}

internal fun PyElement.isDefinition(): Boolean {
    return this is PyClass ||
        // Only consider the base method the definition
        this is PyFunction && PySuperMethodsSearch.findDeepestSuperMethod(this) == this ||
        this is PyNamedParameter ||
        this is PyTargetExpression
}
