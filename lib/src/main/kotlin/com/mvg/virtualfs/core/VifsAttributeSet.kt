package com.mvg.virtualfs.core

import java.util.*

class ViFsAttributeSet(private val _created: Date, _lastModified: Date): AttributeSet {

    var lastModifiedDate = _lastModified

    override val created: Date
        get() = _created
    override val lastModified: Date
        get() = lastModifiedDate
}