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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

fun DeclarationDescriptorWithVisibility.isVisible(from: DeclarationDescriptor): Boolean {
    return isVisible(from, null)
}

fun DeclarationDescriptorWithVisibility.isVisible(
        context: PsiElement,
        receiverExpression: KtExpression?,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade
): Boolean {
    val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)
    val from = resolutionScope.ownerDescriptor
    return isVisible(from, receiverExpression, bindingContext, resolutionScope)
}

private fun DeclarationDescriptorWithVisibility.isVisible(
        from: DeclarationDescriptor,
        receiverExpression: KtExpression?,
        bindingContext: BindingContext? = null,
        resolutionScope: LexicalScope? = null
): Boolean {
    if (Visibilities.isVisibleWithIrrelevantReceiver(this, from)) return true

    if (bindingContext == null || resolutionScope == null) return false

    if (receiverExpression != null) {
        val receiverType = bindingContext.getType(receiverExpression) ?: return false
        val explicitReceiver = ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
        return Visibilities.isVisible(explicitReceiver, this, from)
    }
    else {
        return resolutionScope.getImplicitReceiversHierarchy().any {
            Visibilities.isVisible(it.value, this, from)
        }
    }
}

private fun compareDescriptorsText(project: Project, d1: DeclarationDescriptor, d2: DeclarationDescriptor): Boolean {
    if (d1 == d2) return true
    if (d1.name != d2.name) return false

    val renderedD1 = IdeDescriptorRenderers.SOURCE_CODE.render(d1)
    val renderedD2 = IdeDescriptorRenderers.SOURCE_CODE.render(d2)
    if (renderedD1 == renderedD2) return true

    val declarations1 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d1)
    val declarations2 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d2)
    if (declarations1 == declarations2 && declarations1.isNotEmpty()) return true

    return false
}

public fun compareDescriptors(project: Project, currentDescriptor: DeclarationDescriptor?, originalDescriptor: DeclarationDescriptor?): Boolean {
    if (currentDescriptor == originalDescriptor) return true
    if (currentDescriptor == null || originalDescriptor == null) return false

    if (currentDescriptor.name != originalDescriptor.name) return false

    if (originalDescriptor is SyntheticJavaPropertyDescriptor && currentDescriptor is SyntheticJavaPropertyDescriptor) {
        return compareDescriptors(project, currentDescriptor.getMethod, originalDescriptor.getMethod)
    }

    if (compareDescriptorsText(project, currentDescriptor, originalDescriptor)) return true

    if (originalDescriptor is CallableDescriptor && currentDescriptor is CallableDescriptor) {
        val overriddenOriginalDescriptor = originalDescriptor.findOriginalTopMostOverriddenDescriptors()
        val overriddenCurrentDescriptor = currentDescriptor.findOriginalTopMostOverriddenDescriptors()

        if (overriddenOriginalDescriptor.size != overriddenCurrentDescriptor.size) return false
        return overriddenCurrentDescriptor.zip(overriddenOriginalDescriptor).all {
            compareDescriptorsText(project, it.first, it.second)
        }
    }

    return false
}

public fun Visibility.toKeywordToken(): KtModifierKeywordToken {
    val normalized = normalize()
    when (normalized) {
        Visibilities.PUBLIC -> return KtTokens.PUBLIC_KEYWORD
        Visibilities.PROTECTED -> return KtTokens.PROTECTED_KEYWORD
        Visibilities.INTERNAL -> return KtTokens.INTERNAL_KEYWORD
        else -> {
            if (Visibilities.isPrivate(normalized)) {
                return KtTokens.PRIVATE_KEYWORD
            }
            error("Unexpected visibility '$normalized'")
        }
    }
}
