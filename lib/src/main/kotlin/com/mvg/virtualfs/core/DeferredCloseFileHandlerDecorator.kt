package com.mvg.virtualfs.core

class DeferredCloseFileHandlerDecorator(
        target: FileHandler,
        private val closeAction: () -> Unit)
    : FileHandlerDecoratorBase(target) {

    override fun close() {
        closeAction()
    }
}