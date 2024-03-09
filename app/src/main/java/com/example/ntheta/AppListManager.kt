package com.example.ntheta

import android.content.Context
import kotlinx.coroutines.flow.Flow

class DataStoreHiddenAppMapManager (private val context: Context){
    fun getHiddenList(): Flow<HiddenAppMap> {
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

    suspend fun setHiddenList(hiddenMap: Map<String, AppInfo>) {
        context.dataStore.updateData {
            it.toBuilder().clearHiddenMap().putAllHiddenMap(hiddenMap).build()
        }
    }
}