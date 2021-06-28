package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.StoreOwnerDetails
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class EditShippingLabelPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        loadPaymentMethods()
    }

    private fun loadPaymentMethods() {
        launch {
            viewState = viewState.copy(isLoading = true)
            val accountSettings = shippingLabelRepository.getAccountSettings().let {
                if (it.isError) {
                    triggerEvent(ShowSnackbar(0))
                    triggerEvent(Exit)
                    return@launch
                }
                it.model!!
            }
            viewState = ViewState(
                isLoading = false,
                currentAccountSettings = accountSettings,
                paymentMethods = accountSettings.paymentMethods.map {
                    PaymentMethodUiModel(paymentMethod = it, isSelected = it.id == accountSettings.selectedPaymentId)
                },
                canManagePayments = accountSettings.canManagePayments,
                // Allow editing the email receipts option if the user has either the permission to change settings
                // or changing payment options
                canEditSettings = accountSettings.canEditSettings || accountSettings.canManagePayments,
                emailReceipts = accountSettings.isEmailReceiptEnabled,
                storeOwnerDetails = accountSettings.storeOwnerDetails
            )
        }
    }

    fun onEmailReceiptsCheckboxChanged(isChecked: Boolean) {
        viewState = viewState.copy(emailReceipts = isChecked)
    }

    fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
        val paymentMethodsModels = viewState.paymentMethods.map {
            it.copy(isSelected = it.paymentMethod == paymentMethod)
        }
        viewState = viewState.copy(paymentMethods = paymentMethodsModels)
    }

    fun onDoneButtonClicked() {
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_PAYMENT_METHOD_DONE_BUTTON_TAPPED)

        launch {
            viewState = viewState.copy(showSavingProgressDialog = true)
            val selectedPaymentMethod = viewState.paymentMethods.find { it.isSelected }!!.paymentMethod
            val result = shippingLabelRepository.updatePaymentSettings(
                selectedPaymentMethodId = selectedPaymentMethod.id,
                emailReceipts = viewState.emailReceipts
            )
            viewState = viewState.copy(showSavingProgressDialog = false)

            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_payments_saving_error))
            } else {
                triggerEvent(ExitWithResult(selectedPaymentMethod))
            }
        }
    }

    fun onBackButtonClicked() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        private val currentAccountSettings: ShippingAccountSettings? = null,
        val canManagePayments: Boolean = false,
        val canEditSettings: Boolean = false,
        val paymentMethods: List<PaymentMethodUiModel> = emptyList(),
        val emailReceipts: Boolean = false,
        val storeOwnerDetails: StoreOwnerDetails? = null,
        val showSavingProgressDialog: Boolean = false
    ) : Parcelable {
        val hasChanges: Boolean
            get() {
                return currentAccountSettings?.let {
                    val selectedPaymentMethod = paymentMethods.find { it.isSelected }
                    (selectedPaymentMethod != null &&
                        selectedPaymentMethod.paymentMethod.id != currentAccountSettings.selectedPaymentId) ||
                        emailReceipts != currentAccountSettings.isEmailReceiptEnabled
                } ?: false
            }
    }

    @Parcelize
    data class PaymentMethodUiModel(
        val paymentMethod: PaymentMethod,
        val isSelected: Boolean
    ) : Parcelable
}
