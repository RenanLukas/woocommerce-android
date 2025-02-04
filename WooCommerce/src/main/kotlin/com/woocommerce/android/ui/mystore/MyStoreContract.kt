package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreContract {
    interface Presenter : BasePresenter<View> {
        fun loadStats(granularity: StatsGranularity, forced: Boolean = false)
        fun getStatsCurrency(): String?
        fun getSelectedSiteName(): String?
        suspend fun loadTopPerformersStats(granularity: StatsGranularity, forced: Boolean = false)
        fun dismissJetpackBenefitsBanner()
    }

    interface View : BaseView<Presenter> {
        var isRefreshPending: Boolean

        fun refreshMyStoreStats(forced: Boolean = false)
        fun showStats(revenueStatsModel: WCRevenueStatsModel?, granularity: StatsGranularity)
        fun showStatsError(granularity: StatsGranularity)
        fun updateStatsAvailabilityError()
        fun showTopPerformers(topPerformers: List<WCTopPerformerProductModel>, granularity: StatsGranularity)
        fun showTopPerformersError(granularity: StatsGranularity)
        fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity)
        fun showVisitorStatsError(granularity: StatsGranularity)
        fun showEmptyVisitorStatsForJetpackCP()
        fun showErrorSnack()
        fun showEmptyView(show: Boolean)
        fun showJetpackBenefitsBanner(show: Boolean)

        fun showChartSkeleton(show: Boolean)
        fun showTopPerformersSkeleton(show: Boolean)
    }
}
