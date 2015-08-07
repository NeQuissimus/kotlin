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

package kotlin.reflect.jvm

import java.lang.reflect.Field
import kotlin.reflect.*

@deprecated("Use kotlinProperty instead.", ReplaceWith("kotlinProperty"))
public val Field.kotlin: KProperty<*>?
    get() = kotlinProperty


@deprecated("Use memberProperties instead.", ReplaceWith("memberProperties"))
public val <T : Any> KClass<T>.properties: Collection<KProperty1<T, *>>
    get() = memberProperties

@deprecated("Use extensionProperties instead.", ReplaceWith("extensionProperties"))
public val <T : Any> KClass<T>.extensionProperties: Collection<KProperty2<T, *, *>>
    get() = memberExtensionProperties

@deprecated("Use declaredMemberProperties instead.", ReplaceWith("declaredMemberProperties"))
public val <T : Any> KClass<T>.declaredProperties: Collection<KProperty1<T, *>>
    get() = declaredMemberProperties

@deprecated("Use declaredMemberExtensionProperties instead.", ReplaceWith("declaredMemberExtensionProperties"))
public val <T : Any> KClass<T>.declaredExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = declaredMemberExtensionProperties


@deprecated("Use isAccessible instead.", ReplaceWith("isAccessible"))
public var KProperty<*>.accessible: Boolean
    get() = isAccessible
    set(value) { isAccessible = value }


@deprecated("Use .java instead.", ReplaceWith("java"))
public val <T : Any> KClass<T>.__java: Class<T>
    @jvmName("getJava")
    get() = this.java

@deprecated("Use .kotlin instead.", ReplaceWith("kotlin"))
public val <T : Any> Class<T>.__kotlin: KClass<T>
    @jvmName("getKotlin")
    get() = this.kotlin