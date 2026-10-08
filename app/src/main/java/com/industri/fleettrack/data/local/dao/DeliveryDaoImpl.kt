package com.industri.fleettrack.data.local.dao

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeliveryDaoImpl(context: Context) : DeliveryDao {
    private val prefs = context.getSharedPreferences("fleettrack_local_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mutex = Mutex()
    private val _ordersFlow = MutableStateFlow<List<DeliveryOrderEntity>>(emptyList())

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString("delivery_orders", null)
        val initialList: List<DeliveryOrderEntity> = if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<DeliveryOrderEntity>>() {}.type
                gson.fromJson(json, type) ?: defaultOrders()
            } catch (e: Exception) {
                defaultOrders()
            }
        } else {
            defaultOrders()
        }
        _ordersFlow.value = initialList
        saveToPrefs(initialList)
    }

    private fun defaultOrders(): List<DeliveryOrderEntity> = listOf(
        DeliveryOrderEntity(
            orderId = "ORD-2026-8801",
            trackingNumber = "EXP-90021-JKT",
            recipientName = "Ibu Ratna Dewi",
            recipientPhone = "081234567890",
            destinationAddress = "Jl. Melati Raya No. 45, Kesambi, Cirebon",
            codAmount = 125000.0,
            deliveryStatus = "PENDING",
            isSynced = true
        ),
        DeliveryOrderEntity(
            orderId = "ORD-2026-8802",
            trackingNumber = "EXP-90022-BDG",
            recipientName = "Pak Bambang Sutrisno",
            recipientPhone = "081398765432",
            destinationAddress = "Jl. Kartini No. 12, Kejaksan, Cirebon",
            codAmount = 0.0,
            deliveryStatus = "IN_DELIVERY",
            isSynced = true
        ),
        DeliveryOrderEntity(
            orderId = "ORD-2026-8803",
            trackingNumber = "EXP-90023-CRB",
            recipientName = "Sdr. Dimas Prasetyo",
            recipientPhone = "085712349988",
            destinationAddress = "Perum Citra Garden Blok B-3, Kedawung",
            codAmount = 75000.0,
            deliveryStatus = "DELIVERED",
            isSynced = true
        )
    )

    private fun saveToPrefs(orders: List<DeliveryOrderEntity>) {
        val json = gson.toJson(orders)
        prefs.edit().putString("delivery_orders", json).apply()
    }

    override fun getAllDeliveryOrders(): Flow<List<DeliveryOrderEntity>> = _ordersFlow.asStateFlow()

    override fun getOrdersByStatus(status: String): Flow<List<DeliveryOrderEntity>> {
        return _ordersFlow.map { list -> list.filter { it.deliveryStatus == status } }
    }

    override suspend fun insertAll(orders: List<DeliveryOrderEntity>) {
        mutex.withLock {
            val currentMap = _ordersFlow.value.associateBy { it.orderId }.toMutableMap()
            orders.forEach { currentMap[it.orderId] = it }
            val updatedList = currentMap.values.sortedByDescending { it.updatedAt }
            _ordersFlow.value = updatedList
            saveToPrefs(updatedList)
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        isSynced: Boolean,
        timestamp: Long
    ) {
        mutex.withLock {
            val updatedList = _ordersFlow.value.map { item ->
                if (item.orderId == orderId) {
                    item.copy(deliveryStatus = status, isSynced = isSynced, updatedAt = timestamp)
                } else {
                    item
                }
            }
            _ordersFlow.value = updatedList
            saveToPrefs(updatedList)
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            _ordersFlow.value = emptyList()
            saveToPrefs(emptyList())
        }
    }
}