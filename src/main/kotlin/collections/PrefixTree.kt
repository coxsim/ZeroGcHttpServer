package collections

class PrefixTree<T>(val value: T?, val next: MutableMap<Char, PrefixTree<T>> = mutableMapOf()) {
    companion object {
        operator fun <T> invoke(prefixes: Map<String, T>): PrefixTree<T> {
            return PrefixTree<T>(null).also { root ->
                for ((prefix, value) in prefixes) {
                    var cur = root
                    prefix.forEachIndexed { index, char ->
                        cur = cur.next.compute(char) { _, existing ->
                            if (existing == null) PrefixTree(if (index + 1 == prefix.length) value else null)
                            else {
                                if (index + 1 == prefix.length) PrefixTree(value, existing.next)
                                else existing
                            }
                        }!!
                    }
                }
            }
        }

        tailrec fun <T> PrefixTree<T>.evaluate(length: Int, index: Int, func: (Int) -> Char): T? {
            if (index == length) return value
            val char = func(index)
            val nextTree = this.next[char] ?: return null
            return nextTree.evaluate(length, index + 1, func)
        }
    }
}