enum class MyEnum {
    A, B
}

fun foo(x: MyEnum?): Int {
    val y: Int
    // See KT-6046: y is always initialized
    when (x) {
        MyEnum.A -> y = 1
        MyEnum.B -> y = 2
        null -> y = 0
    }
    return <!UNINITIALIZED_VARIABLE!>y<!>
}