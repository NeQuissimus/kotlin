/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public fun KtFunctionLiteral.findLabelAndCall(): Pair<Name?, KtCallExpression?> {
    val literalParent = (this.getParent() as KtLambdaExpression).getParent()

    fun KtValueArgument.callExpression(): KtCallExpression? {
        val parent = getParent()
        return (if (parent is KtValueArgumentList) parent else this).getParent() as? KtCallExpression
    }

    when (literalParent) {
        is KtLabeledExpression -> {
            val callExpression = (literalParent.getParent() as? KtValueArgument)?.callExpression()
            return Pair(literalParent.getLabelNameAsName(), callExpression)
        }

        is KtValueArgument -> {
            val callExpression = literalParent.callExpression()
            val label = (callExpression?.getCalleeExpression() as? KtSimpleNameExpression)?.getReferencedNameAsName()
            return Pair(label, callExpression)
        }

        else -> {
            return Pair(null, null)
        }
    }
}
