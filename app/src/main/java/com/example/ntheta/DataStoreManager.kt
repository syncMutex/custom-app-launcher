package com.example.ntheta

import android.content.Context
import kotlinx.coroutines.flow.Flow

class DataStoreManager (private val context: Context){
    fun getHiddenList(): Flow<Store> {
        return context.dataStore.data
    }

    suspend fun remove(key: String) {
        context.dataStore.updateData {
            it.toBuilder().removeHiddenMap(key).build()
        }
    }

    suspend fun add(key: String, value: AppInfo) {
        context.dataStore.updateData {
            it.toBuilder().putHiddenMap(key, value).build()
        }
    }

    suspend fun setSensitivity(sensitivity: Float) {
        context.dataStore.updateData {
            it.toBuilder().setScrollSensitivity(sensitivity).build()
        }
    }
}