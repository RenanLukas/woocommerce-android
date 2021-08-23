package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.nhaarman.mockitokotlin2.*
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderOnboardingCheckerTest : BaseUnitTest() {
    private lateinit var checker: CardReaderOnboardingChecker

    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wcPayStore: WCPayStore = mock()
    private val networkStatus: NetworkStatus = mock()

    private val site = SiteModel()

    @Before
    fun setUp() = testBlocking {
        checker = CardReaderOnboardingChecker(
            selectedSite,
            wooStore,
            wcPayStore,
            coroutinesTestRule.testDispatchers,
            networkStatus
        )
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(wcPayStore.loadAccount(site)).thenReturn(buildPaymentAccountResult())
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo())
    }

    @Test
    fun `when not connected to network, then NO_CONNECTION returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(false)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when connected to network, then NO_CONNECTION not returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when store country not supported, then COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.CountryNotSupported::class.java)
    }

    @Test
    fun `when store country supported, then COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.CountryNotSupported::class.java)
    }

    @Test
    fun `given country code in lower case, when store country supported, then COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("us")

            val result = checker.getOnboardingState()

            assertThat(result).isNotInstanceOf(CardReaderOnboardingState.CountryNotSupported::class.java)
        }

    @Test
    fun `when country code is not found, then COUNTRY_NOT_SUPPORTED returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.CountryNotSupported::class.java)
        }

    @Test
    fun `given country not supported, then stripe account loading does not even start`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

            checker.getOnboardingState()

            verify(wcPayStore, never()).loadAccount(anyOrNull())
        }

    @Test
    fun `when woocommerce payments plugin not installed, then WCPAY_NOT_INSTALLED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotInstalled)
        }

    @Test
    fun `when woocommerce payments plugin outdated, then WCPAY_UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(version = "2.4.0"))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayUnsupportedVersion)
        }

    @Test
    fun `when woocommerce payments plugin not active, then WCPAY_NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotActivated)
        }

    @Test
    fun `when stripe account not connected, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpaySetupNotCompleted)
        }

    @Test
    fun `when stripe account under review, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountUnderReview)
        }

    @Test
    fun `when stripe account pending requirements, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account restricted soon, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED_SOON,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account has overdue requirements, then STRIPE_ACCOUNT_OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
        }

    @Test
    fun `when stripe account has both pending and overdue requirements, then OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
        }

    @Test
    fun `when stripe account marked as fraud, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_FRAUD,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account listed, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_LISTED,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account violates terms of service, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_TERMS_OF_SERVICE,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when stripe account rejected for other reasons, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_OTHER,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPayAccountStatusEnum = WCPaymentAccountResult.WCPayAccountStatusEnum.COMPLETE,
        hasPendingRequirements: Boolean = false,
        hadOverdueRequirements: Boolean = false
    ) = WooResult(
        WCPaymentAccountResult(
            status,
            hasPendingRequirements = hasPendingRequirements,
            hasOverdueRequirements = hadOverdueRequirements,
            currentDeadline = null,
            statementDescriptor = "",
            storeCurrencies = WCPaymentAccountResult.WCPayAccountStatusEnum.StoreCurrencies("", listOf()),
            country = "US",
            isCardPresentEligible = true,
            isLive = false
        )
    )

    private fun buildWCPayPluginInfo(
        isActive: Boolean = true,
        version: String = SUPPORTED_WCPAY_VERSION
    ) = WCPluginSqlUtils.WCPluginModel(1, 1, isActive, "", "", version)
}
