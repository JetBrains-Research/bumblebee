package org.jetbrains.research.ml.ast.gumtree.diff

import com.github.gumtreediff.actions.ActionGenerator
import com.github.gumtreediff.actions.model.Action
import com.github.gumtreediff.matchers.Matcher
import com.github.gumtreediff.matchers.Matchers
import com.github.gumtreediff.tree.TreeContext

class Matcher(
    private val srcContext: TreeContext,
    private val dstContext: TreeContext
) {
    fun getEditActions(): List<Action> {
        val matcher = matchTrees()
        val generator = ActionGenerator(srcContext.root, dstContext.root, matcher.mappings)
        generator.generate()
        return generator.actions
    }

    private fun matchTrees(): Matcher {
        val matchers = Matchers.getInstance()
        val matcher = matchers.getMatcher(srcContext.root, dstContext.root)
        matcher.match()
        srcContext.importTypeLabels(dstContext)
        return matcher
    }
}
