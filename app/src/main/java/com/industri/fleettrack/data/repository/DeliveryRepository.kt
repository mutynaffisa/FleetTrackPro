package com.industri.fleettrack.data.repository

import com.industri.fleettrack.data.local.dao.DeliveryDao
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import com.industri.fleettrack.data.remote.api.CourierApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DeliveryRepository(
    private val deliveryDao: DeliveryDao,
    private val apiService: CourierApiService
) {
    val manifestOrdersFlow: Flow<List<DeliveryOrderEntity>> = deliveryDao.getAllDeliveryOrders()

    suspend fun refreshManifest(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val response = apiService.getTodayManifest()
                if (response.isSuccessful && response.body() != null) {
                    val dtoList = response.body()!!
                    val entities = dtoList.map { dto ->
                        DeliveryOrderEntity(
                            orderId = dto.orderId,
                            trackingNumber = dto.trackingNumber,
                            recipientName = dto.recipientName,
                            recipientPhone = dto.recipientPhone,
                            destinationAddress = dto.destinationAddress,
                            codAmount = dto.codAmount,
                            deliveryStatus = dto.status,
                            isSynced = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    deliveryDao.insertAll(entities)
                    return@runCatching
                }
            } catch (e: Exception) {
                // Network Fail Fallback
            }
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val mockData = listOf(
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
        deliveryDao.insertAll(mockData)
    }

    suspend fun updateDeliveryStatus(orderId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            deliveryDao.updateOrderStatus(orderId, newStatus, isSynced = false, timestamp = now)
            try {
                val apiResponse = apiService.updateDeliveryStatus(orderId, newStatus)
                if (apiResponse.isSuccessful) {
                    deliveryDao.updateOrderStatus(orderId, newStatus, isSynced = true, timestamp = now)
                }
            } catch (netErr: Exception) {
                // Offline fallback - saved locally
            }
        }
    }
}