package com.mvg.virtualfs.storage.serialization

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class OutputChannelSerializable(val with: KClass<out OutputChannelSerializer<*>> ){
}