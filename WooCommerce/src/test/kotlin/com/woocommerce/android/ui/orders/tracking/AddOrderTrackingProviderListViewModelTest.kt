package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.ui.orders.OrderTestUtils.ORDER_IDENTIFIER
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class AddOrderTrackingProviderListViewModelTest : BaseUnitTest() {
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val shipmentProvidersRepository: OrderShipmentProvidersRepository = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val testShipmentProvider = listOf(
        OrderShipmentProvider(carrierName = "test aaa", carrierLink = "", country = "US"),
        OrderShipmentProvider(carrierName = "test bbb", carrierLink = "", country = "US"),
        OrderShipmentProvider(carrierName = "test ccc", carrierLink = "", country = "US")
    )

    private lateinit var viewModel: AddOrderTrackingProviderListViewModel

    private val savedState = AddOrderTrackingProviderListFragmentArgs(orderId = ORDER_IDENTIFIER).initSavedStateHandle()

    fun setupViewModel() {
        viewModel = AddOrderTrackingProviderListViewModel(
            savedState = savedState,
            orderDetailRepository = orderDetailRepository,
            shipmentProvidersRepository = shipmentProvidersRepository,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `Shows and hides the provider list skeleton correctly`() = runBlockingTest {
        doReturn(testShipmentProvider).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        setupViewModel()
        var state: ViewState? = null
        viewModel.trackingProviderListViewStateData.observeForever { _, viewState ->
            state = viewState
        }

        verify(shipmentProvidersRepository, times(1)).fetchOrderShipmentProviders(ORDER_IDENTIFIER)
        assertThat(state!!.showSkeleton).isFalse()
        assertThat(state!!.providersList).isEqualTo(testShipmentProvider)
    }

    @Test
    fun `Display error snackbar when provider list is empty`() = runBlockingTest {
        doReturn(emptyList<OrderShipmentProvider>()).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        setupViewModel()
        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertThat((event as ShowSnackbar).message)
            .isEqualTo(R.string.order_shipment_tracking_provider_list_error_empty_list)
    }

    @Test
    fun `Display error snackbar when error occurs`() = runBlockingTest {
        doReturn(null).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        setupViewModel()
        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertThat((event as ShowSnackbar).message)
            .isEqualTo(R.string.order_shipment_tracking_provider_list_error_fetch_generic)
    }

    @Test
    fun `filter results`() = runBlockingTest {
        doReturn(testShipmentProvider).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        setupViewModel()
        viewModel.onSearchQueryChanged("bbb")

        var state: ViewState? = null
        viewModel.trackingProviderListViewStateData.observeForever { _, viewState ->
            state = viewState
        }

        assertThat(state!!.providersList.size).isEqualTo(1)
        assertThat(state!!.providersList[0]).isEqualTo(testShipmentProvider[1])
    }

    @Test
    fun `handle carrier selection`() = runBlockingTest {
        doReturn(testShipmentProvider).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        setupViewModel()
        viewModel.onProviderSelected(testShipmentProvider[0])

        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        assertThat((event as ExitWithResult<Carrier>).data.name).isEqualTo(testShipmentProvider[0].carrierName)
        assertThat((event as ExitWithResult<Carrier>).data.isCustom).isFalse()
    }
}
