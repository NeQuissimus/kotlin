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

package org.jetbrains.kotlin.backend.common.bridges

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized

public fun <Signature> generateBridgesForFunctionDescriptor(
        descriptor: FunctionDescriptor,
        signature: (FunctionDescriptor) -> Signature
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandle(descriptor), { signature(it.descriptor) })
}

/**
 * An implementation of FunctionHandle based on descriptors.
 *
 * This implementation workarounds a minor inconvenience in descriptor hierarchy regarding traits with implementations.
 * Consider the following hierarchy:
 *
 * trait A { fun foo() = 42 }
 * class B : A
 *
 * In terms of descriptors, we'll have a declaration in trait A with modality=OPEN and a fake override in class B with modality=OPEN.
 * For the purposes of bridge generation though, it's much easier to "move" all implementations out of traits into their child classes,
 * i.e. treat the function in A as a declaration with modality=ABSTRACT and a function in B as a _declaration_ with modality=OPEN.
 *
 * This provides us with the nice invariant that all implementations (concrete declarations) are always in classes. This means we _always_
 * can generate a bridge near an implementation (of course, in case it has a super-declaration with a different signature). Ultimately this
 * eases the process of determining what bridges are already generated in our supertypes and need to be inherited, not regenerated.
 */
public data class DescriptorBasedFunctionHandle(val descriptor: FunctionDescriptor) : FunctionHandle {
    private val overridden = descriptor.getOverriddenDescriptors().map { DescriptorBasedFunctionHandle(it.getOriginal()) }

    override val isDeclaration: Boolean =
            descriptor.getKind().isReal() ||
            findTraitImplementation(descriptor) != null

    override val isAbstract: Boolean =
            descriptor.getModality() == Modality.ABSTRACT ||
            DescriptorUtils.isInterface(descriptor.getContainingDeclaration())

    override fun getOverridden() = overridden
}


/**
 * Given a fake override in a class, returns an overridden declaration with implementation in trait, such that a method delegating to that
 * trait implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any trait implementation or such method was already generated into the superclass or is a method from Any.
 */
public fun findTraitImplementation(descriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    if (descriptor.getKind().isReal()) return null
    if (isOrOverridesSynthesized(descriptor)) return null

    val implementation = findImplementationFromInterface(descriptor) ?: return null
    val immediateConcreteSuper = firstSuperMethodFromKotlin(descriptor, implementation) ?: return null

    if (!DescriptorUtils.isInterface(immediateConcreteSuper.getContainingDeclaration())) {
        // If this implementation is already generated into the superclass, we need not generate it again, it'll be inherited
        return null
    }

    return immediateConcreteSuper
}

/**
 * Given a fake override, returns an overridden non-abstract function from an interface which is the actual implementation of this function
 * that should be called when the given fake override is called.
 */
public fun findImplementationFromInterface(descriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    val overridden = OverrideResolver.getOverriddenDeclarations(descriptor)
    val filtered = OverrideResolver.filterOutOverridden(overridden)

    val result = filtered.firstOrNull { it.modality != Modality.ABSTRACT } ?: return null

    if (DescriptorUtils.isClassOrEnumClass(result.containingDeclaration)) return null

    return result
}

/**
 * Given a fake override and its implementation (non-abstract declaration) somewhere in supertypes,
 * returns the first immediate super function of the given fake override which overrides that implementation.
 * The returned function should be called from TImpl-bridges generated for the given fake override.
 */
public fun firstSuperMethodFromKotlin(
        descriptor: CallableMemberDescriptor,
        implementation: CallableMemberDescriptor
): CallableMemberDescriptor? {
    return descriptor.getOverriddenDescriptors().firstOrNull { overridden ->
        overridden.getModality() != Modality.ABSTRACT &&
        (overridden == implementation || OverrideResolver.overrides(overridden, implementation))
    }
}
