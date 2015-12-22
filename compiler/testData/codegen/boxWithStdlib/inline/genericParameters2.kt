interface MComparator<T> {
    fun compare(o1: T, o2: T): Int
}

open class CustomerService {

    fun <T> comparator() = object : MComparator<T> {
        override fun compare(o1: T, o2: T): Int {
            throw UnsupportedOperationException()
        }
    }

    inline fun <T> comparator(crossinline z: () -> Int) = object : MComparator<T> {

        override fun compare(o1: T, o2: T): Int {
            return z()
        }

    }

    fun callInline() =  comparator<String> { 1 }

}

fun box(): String {

    val comparable = CustomerService().comparator<String>()
    val method = comparable.javaClass.getMethod("compare", Any::class.java, Any::class.java)
    val genericParameterTypes = method.genericParameterTypes
    if (genericParameterTypes.size != 2) return "fail 1: ${genericParameterTypes.size}"
    if (genericParameterTypes[0].toString() != "T") return "fail 2: ${genericParameterTypes[0]}"
    if (genericParameterTypes[1].toString() != "T") return "fail 2: ${genericParameterTypes[1]}"


    val comparable2 = CustomerService().callInline()
    val method2 = comparable2.javaClass.getMethod("compare", Any::class.java, Any::class.java)
    val genericParameterTypes2 = method2.genericParameterTypes
    if (genericParameterTypes2.size != 2) return "fail 1: ${genericParameterTypes2.size}"
    if (genericParameterTypes2[0].toString() != "T") return "fail 2: ${genericParameterTypes2[0]}"
    if (genericParameterTypes2[1].toString() != "T") return "fail 2: ${genericParameterTypes2[1]}"

    return "OK"
}