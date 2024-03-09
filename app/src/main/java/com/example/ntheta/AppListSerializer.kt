package com.example.ntheta

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

object HiddenAppMapSerializer: Serializer<HiddenAppMap> {
    override val defaultValue: HiddenAppMap
        get() = HiddenAppMap.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): HiddenAppMap {
        return HiddenAppMap.parseFrom(input)
    }

    override suspend fun writeTo(t: HiddenAppMap, output: OutputStream) {
        t.writeTo(output)
    }
}