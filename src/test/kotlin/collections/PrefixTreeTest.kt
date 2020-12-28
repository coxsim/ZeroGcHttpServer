package collections

import collections.PrefixTree.Companion.evaluate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PrefixTreeTest {
    @Test fun test() {
        val prefixTree = PrefixTree<String>(mapOf(
            "/" to "root",
            "/health" to "health"
        ))
        assertEquals("root", prefixTree.evaluate("/".length, 0) { index -> "/"[index] })
        assertEquals("health", prefixTree.evaluate("/health".length, 0) { index -> "/health"[index] })
        assertNull(prefixTree.evaluate("/foo".length, 0) { index -> "/foo"[index] })
    }
}