package com.woocommerce.android.cardreader.internal.payments

import java.math.BigDecimal
import java.math.RoundingMode

private const val USD_CURRENCY = "usd"
internal const val USD_TO_CENTS_DECIMAL_PLACES = 2

internal class PaymentUtils {
    // TODO cardreader Add support for other currencies
    fun convertBigDecimalInDollarsToLongInCents(amount: BigDecimal): Long {
        return amount
            // round to USD_TO_CENTS_DECIMAL_PLACES decimal places
            .setScale(USD_TO_CENTS_DECIMAL_PLACES, RoundingMode.HALF_UP)
            // convert dollars to cents
            .movePointRight(USD_TO_CENTS_DECIMAL_PLACES)
            .longValueExact()
    }

    // TODO Add Support for other currencies
    fun isSupportedCurrency(currency: String): Boolean = currency.equals(USD_CURRENCY, ignoreCase = true)
}
