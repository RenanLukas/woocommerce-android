package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.REMOVE_CARD
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import com.woocommerce.android.cardreader.internal.payments.AdditionalInfoMapper
import com.woocommerce.android.cardreader.LogWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BluetoothReaderListenerImplTest {
    private val logWrapper: LogWrapper = mock()
    private val additionalInfoMapper: AdditionalInfoMapper = mock()
    private val updateErrorMapper: UpdateErrorMapper = mock()
    private val listener = BluetoothReaderListenerImpl(logWrapper, additionalInfoMapper, updateErrorMapper)

    @Test
    fun `when finishes installing update with error, then failed emitted`() {
        // GIVEN
        val expectedMessage = "message"
        val errorType: SoftwareUpdateStatusErrorType = SoftwareUpdateStatusErrorType.Failed
        val exception = mock<TerminalException> {
            on { message }.thenReturn(expectedMessage)
            on { errorCode }.thenReturn(TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED)
        }
        whenever(updateErrorMapper.map(TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED))
            .thenReturn(errorType)

        // WHEN
        listener.onFinishInstallingUpdate(mock(), exception)

        // THEN
        assertThat(listener.updateStatusEvents.value).isEqualTo(SoftwareUpdateStatus.Failed(errorType, expectedMessage))
    }

    @Test
    fun `when finishes installing update without error, then success emitted`() {
        // WHEN
        listener.onFinishInstallingUpdate(mock(), null)

        // THEN
        assertThat(listener.updateStatusEvents.value).isEqualTo(SoftwareUpdateStatus.Success)
    }

    @Test
    fun `when finishes installing update without error, then update not available emitted`() {
        // WHEN
        listener.onFinishInstallingUpdate(mock(), null)

        // THEN
        assertThat(listener.updateAvailabilityEvents.value).isEqualTo(SoftwareUpdateAvailability.NotAvailable)
    }

    @Test
    fun `when on report available update called, then update available emitted`() {
        // WHEN
        listener.onReportAvailableUpdate(mock())

        // THEN
        assertThat(listener.updateAvailabilityEvents.value).isEqualTo(SoftwareUpdateAvailability.Available)
    }

    @Test
    fun `when on report reader update progress called, then installing emitted`() {
        // GIVEN
        val progress = 0.3f

        // WHEN
        listener.onReportReaderSoftwareUpdateProgress(progress)

        // THEN
        assertThat(listener.updateStatusEvents.value).isEqualTo(SoftwareUpdateStatus.Installing(progress))
    }

    @Test
    fun `when on start installing update called, then installation started emitted`() {
        // WHEN
        listener.onStartInstallingUpdate(mock(), mock())

        // THEN
        assertThat(listener.updateStatusEvents.value).isEqualTo(SoftwareUpdateStatus.InstallationStarted)
    }

    @Test
    fun `when on reader display message called, then display message emitted`() {
        // WHEN
        whenever(additionalInfoMapper.map(any())).thenReturn(REMOVE_CARD)
        listener.onRequestReaderDisplayMessage(ReaderDisplayMessage.REMOVE_CARD)

        // THEN
        assertThat(listener.displayMessagesEvent.value)
            .isEqualTo(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
    }

    @Test
    fun `when on reader input message called, then input message emitted`() {
        // WHEN
        val options = ReaderInputOptions(listOf())
        listener.onRequestReaderInput(options)

        // THEN
        assertThat(listener.displayMessagesEvent.value)
            .isEqualTo(BluetoothCardReaderMessages.CardReaderInputMessage(options.toString()))
    }
}
