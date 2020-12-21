import java.util.*

class ObjectPool<T>(capacity: Int, private val factory: () -> T) {
    inner class PooledObject(val obj: T) {
        fun release() { this@ObjectPool.release(this@PooledObject) }
    }
    private val pool: Queue<PooledObject> = ArrayDeque(capacity)
    init {
        for (i in 0..capacity) {
            pool.add(PooledObject(factory()))
        }
    }

    fun acquire(): PooledObject = pool.poll() ?: PooledObject(factory())

    private fun release(item: PooledObject) = pool.add(item)
}