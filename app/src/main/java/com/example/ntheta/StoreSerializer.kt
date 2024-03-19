package com.example.ntheta

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

object StoreSerializer: Serializer<Store> {
    override val defaultValue: Store
        get() = Store.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Store {
        return Store.parseFrom(input)
    }

    override suspend fun writeTo(t: Store, output: OutputStream) {
        t.writeTo(output)
    }
}