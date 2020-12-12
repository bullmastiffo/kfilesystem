package com.mvg.virtualfs.core

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class DeferredCloseFolderHandlerDecoratorTest{

    @Test
    fun `Test close delegates call to provided action`() {
        val targetHandler = mockk<FolderHandler>()
        val action = spyk<()->Unit>()
        val sut = DeferredCloseFolderHandlerDecorator(targetHandler, action)

        sut.close()

        verify { action() }
        verify(exactly =  0) { targetHandler.close()  }
    }
}