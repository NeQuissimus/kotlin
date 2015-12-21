fun <E> foo(x: Any, y: Any) : Any {
    class C
    if(x is <!CANNOT_CHECK_FOR_ERASED!>C<!>) {
        return x
    }
    <!UNCHECKED_CAST!>x as C<!>

    class Outer<F> {
        inner class Inner
    }

    // bare type
    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer.Inner<!>) {
        return y
    }

    <!UNCHECKED_CAST!>y as Outer<*>.Inner<!>

    return C()
}
