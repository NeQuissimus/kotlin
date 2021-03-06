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

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

private val NO_INFER_ANNOTATION_FQ_NAME = FqName("kotlin.internal.NoInfer")
private val EXACT_ANNOTATION_FQ_NAME = FqName("kotlin.internal.Exact")

public fun KotlinType.hasNoInferAnnotation(): Boolean = annotations.hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME)

public fun KotlinType.hasExactAnnotation(): Boolean = annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

public fun Annotations.hasInternalAnnotationForResolve(): Boolean =
        hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME) || hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

public fun FqName.isInternalAnnotationForResolve() = this == NO_INFER_ANNOTATION_FQ_NAME || this == EXACT_ANNOTATION_FQ_NAME

private val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME = FqName("kotlin.internal.LowPriorityInOverloadResolution")

public fun CallableDescriptor.hasLowPriorityInOverloadResolution(): Boolean = annotations.hasAnnotation(LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME)

private val ONLY_INPUT_TYPES_FQ_NAME = FqName("kotlin.internal.OnlyInputTypes")

public fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)

public fun getExactInAnnotations(): Annotations = AnnotationsWithOnly(EXACT_ANNOTATION_FQ_NAME)

private class AnnotationsWithOnly(val presentAnnotation: FqName): Annotations {
    override fun iterator(): Iterator<AnnotationDescriptor> = listOf<AnnotationDescriptor>().iterator()

    override fun isEmpty(): Boolean = false

    override fun hasAnnotation(fqName: FqName): Boolean = fqName == this.presentAnnotation

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? = null

    override fun findExternalAnnotation(fqName: FqName): AnnotationDescriptor? = null

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    override fun getAllAnnotations(): List<AnnotationWithTarget> = emptyList()
}