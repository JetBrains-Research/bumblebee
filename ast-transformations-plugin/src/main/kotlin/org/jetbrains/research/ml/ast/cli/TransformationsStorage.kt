package org.jetbrains.research.ml.ast.cli

import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.augmentedAssignment.AugmentedAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.commentsRemoval.CommentsRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.comparisonUnification.ComparisonUnificationTransformation
import org.jetbrains.research.ml.ast.transformations.constantfolding.ConstantFoldingTransformation
import org.jetbrains.research.ml.ast.transformations.deadcode.DeadCodeRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.emptyLinesRemoval.EmptyLinesRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.expressionUnification.ExpressionUnificationTransformation
import org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval.IfRedundantLinesRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison.MultipleOperatorComparisonTransformation
import org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment.MultipleTargetAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.outerNotElimination.OuterNotEliminationTransformation
import java.util.*

internal object TransformationsStorage {
    // TODO: use Reflekt compiler plugin
    private val allTransformationsMap = getListOfAllTransformations()
        .associateBy { it.key.lowercase(Locale.getDefault()) }

    fun getListOfAllTransformations(): List<Transformation> {
        return listOf(
            AnonymizationTransformation,
            AugmentedAssignmentTransformation,
            CommentsRemovalTransformation,
            ComparisonUnificationTransformation,
            ConstantFoldingTransformation,
            DeadCodeRemovalTransformation,
            EmptyLinesRemovalTransformation,
            ExpressionUnificationTransformation,
            IfRedundantLinesRemovalTransformation,
            MultipleOperatorComparisonTransformation,
            MultipleTargetAssignmentTransformation,
            OuterNotEliminationTransformation,
        )
    }

    fun getTransformationByKey(key: String): Transformation? = allTransformationsMap[key.lowercase(Locale.getDefault())]
}
