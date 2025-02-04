package com.woocommerce.android.ui.orders.quickorder

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class QuickOrderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val wooCommerceStore: WooCommerceStore,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData

    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val decimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    var currentPrice: BigDecimal
        get() = viewState.currentPrice
        set(value) {
            viewState = viewState.copy(
                currentPrice = value,
                isDoneButtonEnabled = value > BigDecimal.ZERO
            )
        }

    fun onDoneButtonClicked() {
        createQuickOrder()
    }

    fun createQuickOrder() {
        if (!networkStatus.isConnected()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIMPLE_PAYMENTS_FLOW_FAILED)
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            return
        }

        viewState = viewState.copy(isProgressShowing = true, isDoneButtonEnabled = false)

        launch(Dispatchers.IO) {
            val result = orderStore.postQuickOrder(
                selectedSite.get(),
                viewState.currentPrice.toString()
            )

            withContext(Dispatchers.Main) {
                viewState = viewState.copy(isProgressShowing = false, isDoneButtonEnabled = true)
                if (result.isError) {
                    WooLog.e(WooLog.T.ORDERS, "${result.error.type.name}: ${result.error.message}")
                    AnalyticsTracker.track(AnalyticsTracker.Stat.SIMPLE_PAYMENTS_FLOW_FAILED)
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.quickorder_creation_error))
                } else {
                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SIMPLE_PAYMENTS_FLOW_COMPLETED,
                        mapOf(AnalyticsTracker.KEY_AMOUNT to viewState.currentPrice.toString())
                    )
                    viewState = viewState.copy(createdOrder = result.order!!.toAppModel())
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null
    ) : Parcelable

    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }
}
