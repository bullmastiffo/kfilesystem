package com.mvg.virtualfs.storage

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.*

object DateAsLongSerializer : KSerializer<Date?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date?) = encoder.encodeLong(value?.time ?: 0L)
    override fun deserialize(decoder: Decoder): Date? {
        var l = decoder.decodeLong()
        return if (l == 0L) {
            null
        } else {
            Date(l)
        }
    }
}