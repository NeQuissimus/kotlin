enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    when (dir) {
        Direction.NORTH -> return 1
        Direction.SOUTH -> return 2
        Direction.WEST  -> return 3
        Direction.EAST  -> return 4
    }
    // See KT-1882: no return is needed at the end
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>