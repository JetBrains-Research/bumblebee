package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.PyTokenTypes.*
import com.jetbrains.python.psi.*

object PyUtils {
    fun createPyIfElsePart(ifElsePart: PyIfPart): PyIfPart {
        require(ifElsePart.isElif) { "Illegal if part. Only `elif` part supported." }
        val generator = PyElementGenerator.getInstance(ifElsePart.project)
        val ifStatement = generator.createFromText(
            LanguageLevel.getDefault(),
            PyIfStatement::class.java,
            "if ${ifElsePart.condition?.text ?: ""}:\n\t${ifElsePart.statementList.text}"
        )
        return ifStatement.ifPart
    }

    fun braceExpression(expression: PyExpression): PyExpression {
        val generator = PyElementGenerator.getInstance(expression.project)
        return generator.createExpressionFromText(LanguageLevel.getDefault(), "(${expression.text})")
    }

    fun createAssignment(target: PsiElement, value: PsiElement): PyAssignmentStatement {
        val generator = PyElementGenerator.getInstance(target.project)
        return generator.createFromText(
            LanguageLevel.getDefault(),
            PyAssignmentStatement::class.java,
            "${target.text} = ${value.text}"
        )
    }

    fun createAssignment(assignment: PyAugAssignmentStatement): PyAssignmentStatement {
        val generator = PyElementGenerator.getInstance(assignment.project)
        val assignmentTargetText = assignment.target.text
        val augOperation =
            assignment.operation as? LeafPsiElement ?: throw IllegalArgumentException("Operation is required")
        val operation = createOperation(augOperation)
        var value = assignment.value ?: throw IllegalArgumentException("Value is required")
        value = braceValueIfNeeded(value)
        return generator.createFromText(
            LanguageLevel.getDefault(),
            PyAssignmentStatement::class.java,
            "$assignmentTargetText = $assignmentTargetText ${operation.text} ${value.text}"
        )
    }

    private fun braceValueIfNeeded(operation: PyExpression): PyExpression {
        val operationLeafElement = PsiTreeUtils.findFirstChildrenOrNull(operation) { element ->
            val leafElement = element as? LeafPsiElement ?: return@findFirstChildrenOrNull false
            OPERATIONS.contains(leafElement.elementType)
        }
        if (operationLeafElement != null) {
            val generator = PyElementGenerator.getInstance(operation.project)
            return generator.createExpressionFromText(LanguageLevel.getDefault(), "(${operation.text})")
        }
        return operation
    }

    private fun createOperation(operation: LeafPsiElement): LeafPsiElement {
        val (newOperation, text) = when (operation.elementType) {
            PLUSEQ -> PLUS to "+"
            MINUSEQ -> MINUS to "-"
            MULTEQ -> MULT to "*"
            ATEQ -> AT to "@"
            DIVEQ -> DIV to "/"
            PERCEQ -> PERC to "%"
            EXPEQ -> EXP to "**"
            GTGTEQ -> GTGT to ">>"
            LTLTEQ -> LTLT to "<<"
            ANDEQ -> AND to "&"
            OREQ -> OR to "|"
            XOREQ -> XOR to "^"
            FLOORDIVEQ -> FLOORDIV to "//"
            else -> throw IllegalArgumentException("Illegal operation. Only augment operations accepted")
        }

        return LeafPsiElement(newOperation, text)
    }
}
