package com.woocommerce.android.ui.orders.details.editing

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderEditingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderEditingRepository: OrderEditingRepository
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<OrderDetailFragmentArgs>()
    private val order: Order

    private val orderIdentifier: String
        get() = navArgs.orderId

    val customerOrderNote: String
        get() = order.customerNote

    init {
        order = orderEditingRepository.getOrder(orderIdentifier)
    }

    fun updateCustomerOrderNote(updatedCustomerOrderNote: String) {
        launch(dispatchers.io) {
            orderEditingRepository.updateCustomerOrderNote(order, updatedCustomerOrderNote)
        }
    }
}
