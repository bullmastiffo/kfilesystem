package com.mvg.virtualfs

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions

suspend fun massiveRun(times: Int, action: suspend (Int) -> Unit) {
    coroutineScope {
        repeat(times) {
            launch {
                action(it)
            }
        }
    }
}

/**
 * Helper method to test decorators, it checks that passed testCall invocation will be proxied to target
 * instance of type M
 * @param expected T expected method invocation result
 * @param sutFactory Function1<M, M> Factory method to create tested decorator (system under test)
 * @param testCall Function1<[@kotlin.ParameterName] M, T> call invocation to test
 */
inline fun <T, reified M : Any> testDecoratorCallProxiesToTarget(
        expected: T,
        sutFactory:(M) -> M,
        crossinline testCall: (sut: M) -> T){
    val targetHandler = mockk<M>()
    val sut = sutFactory(targetHandler)
    every { testCall(targetHandler) }.returns(expected)

    val result = testCall(sut)

    Assertions.assertEquals(expected, result)
    verify { testCall(targetHandler) }
}