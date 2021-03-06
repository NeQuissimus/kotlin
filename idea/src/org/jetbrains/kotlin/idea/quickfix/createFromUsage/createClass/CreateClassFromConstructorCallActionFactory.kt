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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeInfoForTypeArguments
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

public object CreateClassFromConstructorCallActionFactory: CreateClassFromUsageFactory<KtCallExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
        val diagElement = diagnostic.psiElement
        if (diagElement.getNonStrictParentOfType<KtTypeReference>() != null) return null

        val callExpr = diagElement.parent as? KtCallExpression ?: return null
        return if (callExpr.calleeExpression == diagElement) callExpr else null
    }

    override fun getPossibleClassKinds(element: KtCallExpression, diagnostic: Diagnostic): List<ClassKind> {
        val inAnnotationEntry = diagnostic.psiElement.getNonStrictParentOfType<KtAnnotationEntry>() != null

        val (context, moduleDescriptor) = element.analyzeFullyAndGetResult()
        val file = element.containingFile as? KtFile ?: return emptyList()
        val call = element.getCall(context) ?: return emptyList()
        val targetParent = getTargetParentByCall(call, file, context) ?: return emptyList()

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS
        val fullCallExpr = element.getQualifiedExpressionForSelectorOrThis()
        if (!fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent).second(classKind)) return emptyList()

        return classKind.singletonList()
    }

    override fun extractFixData(element: KtCallExpression, diagnostic: Diagnostic): ClassInfo? {
        val diagElement = diagnostic.psiElement
        if (diagElement.getNonStrictParentOfType<KtTypeReference>() != null) return null

        val inAnnotationEntry = diagElement.getNonStrictParentOfType<KtAnnotationEntry>() != null

        val callExpr = diagElement.parent as? KtCallExpression ?: return null
        if (callExpr.calleeExpression != diagElement) return null

        val calleeExpr = callExpr.calleeExpression as? KtSimpleNameExpression ?: return null

        val name = calleeExpr.getReferencedName()
        if (!inAnnotationEntry && !name.checkClassName()) return null

        val callParent = callExpr.parent
        val fullCallExpr =
                if (callParent is KtQualifiedExpression && callParent.selectorExpression == callExpr) callParent else callExpr

        val file = fullCallExpr.containingFile as? KtFile ?: return null

        val (context, moduleDescriptor) = callExpr.analyzeFullyAndGetResult()

        val call = callExpr.getCall(context) ?: return null
        val targetParent = getTargetParentByCall(call, file, context) ?: return null
        val inner = isInnerClassExpected(call)

        val valueArguments = callExpr.valueArguments
        val defaultParamName = if (inAnnotationEntry && valueArguments.size == 1) "value" else null
        val anyType = moduleDescriptor.builtIns.nullableAnyType
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.referenceExpression?.getReferencedName() ?: defaultParamName
            )
        }

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS

        val (expectedTypeInfo, filter) = fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent)
        if (!filter(classKind)) return null

        val typeArgumentInfos = if (inAnnotationEntry) Collections.emptyList() else callExpr.getTypeInfoForTypeArguments()

        return ClassInfo(
                name = name,
                targetParent = targetParent,
                expectedTypeInfo = expectedTypeInfo,
                inner = inner,
                typeArguments = typeArgumentInfos,
                parameterInfos = parameterInfos
        )
    }
}
