package com.woocommerce.android.cardreader

import android.app.Application
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.firmware.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.firmware.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.payments.PaymentInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
interface CardReaderManager {
    val isInitialized: Boolean
    val readerStatus: StateFlow<CardReaderStatus>
    fun initialize(app: Application)
    fun discoverReaders(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents>
    suspend fun connectToReader(cardReader: CardReader): Boolean
    suspend fun disconnectReader(): Boolean

    suspend fun collectPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus>

    suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus>
    fun cancelPayment(paymentData: PaymentData)

    suspend fun softwareUpdateAvailability(): Flow<SoftwareUpdateAvailability>
    suspend fun updateSoftware(): Flow<SoftwareUpdateStatus>
    suspend fun clearCachedCredentials()
}
