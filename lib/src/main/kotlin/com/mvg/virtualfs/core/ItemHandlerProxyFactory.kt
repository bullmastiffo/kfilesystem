package com.mvg.virtualfs.core

import javax.management.InvalidApplicationException

class ItemHandlerProxyFactory : ProxyFactory<ItemHandler>{
    override fun <T : ItemHandler> create(target: T, closeOverride: () -> Unit): T
    {
        return when(target){
            is FolderHandler -> DeferredCloseFolderHandlerDecorator(target, closeOverride) as T
            is FileHandler -> DeferredCloseFileHandlerDecorator(target, closeOverride) as T
            else -> throw InvalidApplicationException("Trying to create proxy for unknown ItemHandler type ${target::class}")
        }
    }
}