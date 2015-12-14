// See KT-5113
enum class E {
    A,
    B
}

class Outer(e: E) {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val prop: Int<!>
    init {
        when(e ) {
            // When is exhaustive, property is always initialized
            E.A -> prop = 1
            E.B -> prop = 2
        }
    }
}