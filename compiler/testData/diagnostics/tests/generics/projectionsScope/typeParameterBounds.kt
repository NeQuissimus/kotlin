// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Out<out X>
class In<in Y>
class Inv<Z>

class A<T> {
    fun <E : Out<T>> foo1(x: E) = 1
    fun <F : Inv<T>> foo2(x: F) = 1
    fun <G : In<T>>  foo3(x: G) = 1
}

fun foo2(a: A<out CharSequence>, b: A<in CharSequence>) {
    a.foo1(<!TYPE_MISMATCH!>Out()<!>)
    a.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(Out<CharSequence>())
    a.foo1<<!UPPER_BOUND_VIOLATED!>Out<CharSequence><!>>(Out())
    a.foo1(Out<Nothing>())

    a.foo2(<!TYPE_MISMATCH!>Inv()<!>)
    a.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>(Inv<CharSequence>())
    a.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(Inv())

    // TODO: type inference could be smarter here
    a.foo3(<!TYPE_MISMATCH!>In()<!>)
    a.foo3(In<CharSequence>())
    a.foo3<In<CharSequence>>(In())

    // TODO: type inference could be smarter here
    b.foo1(<!TYPE_MISMATCH!>Out()<!>)
    b.foo1(Out<CharSequence>())
    b.foo1<Out<CharSequence>>(Out())

    b.foo2(<!TYPE_MISMATCH!>Inv()<!>)
    b.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>(Inv<CharSequence>())
    b.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(Inv())

    b.foo3(<!TYPE_MISMATCH!>In()<!>)
    b.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo3<!>(In<CharSequence>())
    b.foo3<<!UPPER_BOUND_VIOLATED!>In<CharSequence><!>>(In())
    b.foo3(In<Any?>())
}
