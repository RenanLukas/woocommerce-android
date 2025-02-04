package com.woocommerce.android.cardreader.internal.payments

import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

private const val NONE_USD_CURRENCY = "CZK"
private const val USD_CURRENCY = "USD"

@RunWith(MockitoJUnitRunner::class)
class PaymentUtilsTest {
    private val paymentUtils = PaymentUtils()

    @Test
    fun `given currency not USD, when is currency supported invoked, then false returned`() = runBlockingTest {
        val result = paymentUtils.isSupportedCurrency(NONE_USD_CURRENCY)

        assertThat(result).isFalse()
    }

    @Test
    fun `given currency USD, when is currency supported invoked, then true returned`() = runBlockingTest {
        val result = paymentUtils.isSupportedCurrency(USD_CURRENCY)

        assertThat(result).isTrue()
    }

    @Test
    fun `when amount $1, then it gets converted to 100 cents`() = runBlockingTest {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1))

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `when amount $1 005cents, then it gets rounded down to 100 cents`() = runBlockingTest {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.005))

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `when amount $1 006cents, then it gets rounded up to 101 cents`() = runBlockingTest {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.006))

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `when amount $1 99 cents, then it gets converted to 199 cents`() = runBlockingTest {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.99))

        assertThat(result).isEqualTo(199)
    }
}
