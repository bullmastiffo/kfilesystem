package com.mvg.virtualfs.core

class DeferredCloseFolderHandlerDecorator(
        target: FolderHandler,
        private val closeAction: () -> Unit)
    : FolderHandlerDecoratorBase(target) {

    override fun close() {
        closeAction()
    }

    protected fun finalize() {
        close()
    }
}