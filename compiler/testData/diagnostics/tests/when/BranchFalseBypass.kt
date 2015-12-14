enum class My { A, B }

fun test(a: My): String {
    val q: String?

    when (a) {
        My.A -> q = "1"
        My.B -> q = "2"
    }
    // When is exhaustive
    return <!UNINITIALIZED_VARIABLE, DEBUG_INFO_SMARTCAST!>q<!>
}
