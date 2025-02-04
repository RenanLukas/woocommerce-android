package com.woocommerce.android.extensions

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.store.WCProductStore

internal const val CASH_ON_DELIVERY_PAYMENT_TYPE = "cod"
internal const val WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE = "woocommerce_payments"

val CASH_PAYMENTS = listOf(CASH_ON_DELIVERY_PAYMENT_TYPE, "bacs", "cheque")

/**
 * Returns true if all the products specified in the [WCOrderModel.LineItem] is a virtual product
 * and if product exists in the local cache.
 */
fun isVirtualProduct(
    site: SiteModel,
    lineItems: List<LineItem>,
    productStore: WCProductStore
): Boolean {
    if (lineItems.isNullOrEmpty()) {
        return false
    }

    val remoteProductIds: List<Long> = lineItems.filter { it.productId != null }.map { it.productId!! }
    if (remoteProductIds.isNullOrEmpty()) {
        return false
    }

    // verify that the LineItem product is in the local cache and
    // that the product count in the local cache matches the lineItem count.
    val productModels = productStore.getProductsByRemoteIds(site, remoteProductIds)
    if (productModels.isNullOrEmpty() || productModels.count() != remoteProductIds.count()) {
        return false
    }

    return productModels.filter { !it.virtual }.isEmpty()
}

val String.isCashPayment: Boolean
    get() = CASH_PAYMENTS.contains(this)

fun WCOrderModel.getBillingName(defaultValue: String): String {
    return if (billingFirstName.isEmpty() && billingLastName.isEmpty()) {
        defaultValue
    } else "$billingFirstName $billingLastName"
}
