package com.woocommerce.android.ui.searchfilter

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSearchFilterBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFilterFragment : BaseFragment(R.layout.fragment_search_filter) {
    companion object {
        val TAG: String = SearchFilterFragment::class.java.simpleName
    }

    private val navArgs: SearchFilterFragmentArgs by navArgs()

    private val searchTitle: String
        get() = navArgs.title

    private val searchHint: String
        get() = navArgs.hint

    private val searchFilterItems: Array<SearchFilterItem>
        get() = navArgs.items

    private val requestKey: String
        get() = navArgs.requestKey

    private var _binding: FragmentSearchFilterBinding? = null
    private val binding: FragmentSearchFilterBinding
        get() = _binding!!

    private lateinit var searchFilterAdapter: SearchFilterAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchFilterBinding.bind(view)
        setupTitle()
        setupSearch()
        setupSearchList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showEmptyView(searchQuery: String) {
        binding.emptyView.show(
            WCEmptyView.EmptyViewType.SEARCH_RESULTS,
            searchQueryOrFilter = searchQuery
        )
    }

    private fun hideEmptyView() {
        binding.emptyView.hide()
    }

    private fun setupTitle() {
        activity?.title = searchTitle
    }

    private fun setupSearch() {
        binding.searchEditText.apply {
            hint = searchHint
            doAfterTextChanged {
                val text = it.toString()
                val searchedItems = searchFilterItems.filter { it.name.contains(other = text, ignoreCase = true) }
                searchFilterAdapter.setItems(searchedItems)
                //TODO move to VM
                if (searchedItems.isEmpty()) {
                    showEmptyView(text)
                } else {
                    hideEmptyView()
                }
            }
        }
    }

    private fun setupSearchList() {
        binding.searchItemsList.apply {
            layoutManager = LinearLayoutManager(context)
            searchFilterAdapter = SearchFilterAdapter(
                onItemSelectedListener = { selectedItem ->
                    navigateBackWithResult(requestKey, selectedItem.value)
                }
            )
            adapter = searchFilterAdapter
            searchFilterAdapter.setItems(searchFilterItems.toList())
            addItemDecoration(
                AlignedDividerDecoration(
                    ctx = context,
                    orientation = DividerItemDecoration.VERTICAL,
                    alignStartToStartOf = R.id.filterItemName
                )
            )
        }
    }
}
