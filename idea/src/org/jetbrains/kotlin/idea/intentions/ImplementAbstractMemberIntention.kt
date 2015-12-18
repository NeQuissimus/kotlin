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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.ui.components.JBList
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.refactoring.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*
import javax.swing.ListSelectionModel

class ImplementAbstractMemberIntention :
        SelfTargetingRangeIntention<KtNamedDeclaration>(KtNamedDeclaration::class.java, "", "Implement abstract member") {
    companion object {
        private val LOG = Logger.getInstance("#${ImplementAbstractMemberIntention::class.java.canonicalName}")
    }

    private fun isAbstract(element: KtNamedDeclaration): Boolean {
        if (element.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return true
        if (!(element.containingClassOrObject?.isInterfaceClass() ?: false)) return false
        return when (element) {
            is KtProperty -> element.initializer == null && element.delegate == null && element.accessors.isEmpty()
            is KtNamedFunction -> !element.hasBody()
            else -> false
        }
    }

    private fun findExistingImplementation(
            subClass: ClassDescriptor,
            superMember: CallableMemberDescriptor
    ): CallableMemberDescriptor? {
        val superClass = superMember.containingDeclaration as? ClassDescriptor ?: return null
        val substitutor = getTypeSubstitutor(superClass.defaultType, subClass.defaultType) ?: TypeSubstitutor.EMPTY
        val subMember = subClass.findCallableMemberBySignature(superMember.substitute(substitutor) as CallableMemberDescriptor)
        if (subMember?.kind?.isReal ?: false) return subMember else return null
    }

    private fun findClassesToProcess(member: KtNamedDeclaration): Sequence<KtClassOrObject> {
        val baseClass = member.containingClassOrObject as? KtClass ?: return emptySequence()
        val memberDescriptor = member.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptySequence()

        fun acceptSubClass(classOrObject: KtClassOrObject): Boolean {
            val classDescriptor = classOrObject.resolveToDescriptorIfAny() as? ClassDescriptor ?: return false
            return classDescriptor.kind != ClassKind.INTERFACE && findExistingImplementation(classDescriptor, memberDescriptor) == null
        }

        if (baseClass.isEnum()) {
            return baseClass.declarations
                    .asSequence()
                    .filterIsInstance<KtEnumEntry>()
                    .filter(::acceptSubClass)
        }

        return HierarchySearchRequest(baseClass, baseClass.useScope, false)
                .searchInheritors()
                .asSequence()
                .mapNotNull { (it as? KtLightClassForExplicitDeclaration)?.getOrigin() }
                .filter(::acceptSubClass)
    }

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (!isAbstract(element)) return null

        text = when(element) {
            is KtProperty -> "Implement abstract property"
            is KtNamedFunction -> "Implement abstract function"
            else -> return null
        }

        if (!findClassesToProcess(element).any()) return null

        return element.nameIdentifier?.textRange
    }

    private fun implementInClass(member: KtNamedDeclaration, targetClasses: List<PsiElement>) {
        val project = member.project
        project.executeCommand(CodeInsightBundle.message("intention.implement.abstract.method.command.name")) {
            if (!FileModificationService.getInstance().preparePsiElementsForWrite(targetClasses)) return@executeCommand
            runWriteAction {
                for (targetClass in targetClasses) {
                    try {
                        val subClass = (targetClass as? KtLightClass)?.getOrigin() ?: targetClass as? KtClassOrObject ?: continue
                        val subClassDescriptor = subClass.resolveToDescriptorIfAny() as? ClassDescriptor ?: continue
                        val superMemberDescriptor = member.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: continue
                        val superClassDescriptor = superMemberDescriptor.containingDeclaration as? ClassDescriptor ?: continue
                        val substitutor = getTypeSubstitutor(superClassDescriptor.defaultType, subClassDescriptor.defaultType)
                                          ?: TypeSubstitutor.EMPTY
                        val descriptorToImplement = superMemberDescriptor.substitute(substitutor) as CallableMemberDescriptor
                        val chooserObject = OverrideMemberChooserObject.create(project,
                                                                               descriptorToImplement,
                                                                               descriptorToImplement,
                                                                               OverrideMemberChooserObject.BodyType.EMPTY)
                        OverrideImplementMembersHandler.generateMembers(null, subClass, chooserObject.singletonList())
                    }
                    catch(e: IncorrectOperationException) {
                        LOG.error(e)
                    }
                }
            }
        }
    }

    private class ClassRenderer : PsiElementListCellRenderer<PsiElement>() {
        private val psiClassRenderer = PsiClassListCellRenderer()

        override fun getComparator(): Comparator<PsiElement> {
            val baseComparator = psiClassRenderer.comparator
            return Comparator { o1, o2 ->
                when {
                    o1 is KtEnumEntry && o2 is KtEnumEntry -> o1.name!!.compareTo(o2.name!!)
                    o1 is KtEnumEntry -> -1
                    o2 is KtEnumEntry -> 1
                    o1 is PsiClass && o2 is PsiClass -> baseComparator.compare(o1 as PsiClass, o2 as PsiClass)
                    else -> 0
                }
            }
        }

        override fun getIconFlags() = 0

        override fun getElementText(element: PsiElement?): String? {
            return when (element) {
                is KtEnumEntry -> element.name
                is PsiClass -> psiClassRenderer.getElementText(element)
                else -> null
            }
        }

        override fun getContainerText(element: PsiElement?, name: String?): String? {
            return when (element) {
                is KtEnumEntry -> element.containingClassOrObject?.fqName?.asString()
                is PsiClass -> PsiClassListCellRenderer.getContainerTextStatic(element)
                else -> null
            }
        }
    }

    override fun applyTo(element: KtNamedDeclaration, editor: Editor) {
        val project = element.project

        val classesToProcess = project.runSynchronouslyWithProgress(
                CodeInsightBundle.message("intention.implement.abstract.method.searching.for.descendants.progress"),
                true
        ) { findClassesToProcess(element).map { it.toLightClass() ?: it }.toList() } ?: return
        if (classesToProcess.isEmpty()) return

        classesToProcess.singleOrNull()?.let { return implementInClass(element, it.singletonList()) }

        if (ApplicationManager.getApplication().isUnitTestMode) return implementInClass(element, classesToProcess)

        val renderer = ClassRenderer()
        val list = JBList(classesToProcess.sortedWith(renderer.comparator)).apply {
            selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            cellRenderer = renderer
        }
        val builder = PopupChooserBuilder(list)
        renderer.installSpeedSearch(builder)
        builder
                .setTitle(CodeInsightBundle.message("intention.implement.abstract.method.class.chooser.title"))
                .setItemChoosenCallback {
                    val index = list.selectedIndex
                    if (index < 0) return@setItemChoosenCallback
                    @Suppress("UNCHECKED_CAST")
                    implementInClass(element, list.selectedValues.toList() as List<KtClassOrObject>)
                }
                .createPopup()
                .showInBestPositionFor(editor)
    }
}