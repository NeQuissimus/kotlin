fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!TYPE_MISMATCH!>mc<!>)

    mc.addAll(<!TYPE_MISMATCH!>arrayListOf<CharSequence>()<!>)
    mc.addAll(<!TYPE_MISMATCH!>arrayListOf()<!>)

    mc.addAll(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>listOf("")<!>)
    mc.addAll(<!TYPE_MISMATCH!>listOf<String>("")<!>)
    mc.addAll(<!TYPE_MISMATCH!>listOf<CharSequence>("")<!>)

    mc.addAll(<!TYPE_MISMATCH!>emptyList()<!>)
    mc.addAll(emptyList<Nothing>())
}
