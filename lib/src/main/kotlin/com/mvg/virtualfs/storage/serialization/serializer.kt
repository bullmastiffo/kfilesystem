package com.mvg.virtualfs.storage.serialization

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.*
import kotlin.reflect.full.*

private val map: ConcurrentHashMap<KClass<*>, OutputChannelSerializer<*>> = ConcurrentHashMap<KClass<*>, OutputChannelSerializer<*>>()
fun <T> serializeToChannel(channel: OutputChannel, value: T){
    if (value == null)
    {
        throw IllegalArgumentException("value must not be null")
    }

    val typeClass = value!!::class
    val serializer = map.getOrPut(typeClass, {  initialize<T>(typeClass) }) as OutputChannelSerializer<T>
    serializer.serialize(channel, value)
}

private fun <T> initialize(typeClass: KClass<*>) : OutputChannelSerializer<T>
{
    val serializerAttribute = typeClass.annotations.find { it is OutputChannelSerializable } as? OutputChannelSerializable
            ?: throw IllegalArgumentException("${typeClass.simpleName} must have OutputChannelSerializable attribute")

    var instance = serializerAttribute.with.objectInstance as? OutputChannelSerializer<T>
    if (instance == null) {
        instance = serializerAttribute.with.createInstance() as OutputChannelSerializer<T>
    }

    return instance
}