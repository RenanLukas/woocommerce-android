package com.woocommerce.android.ui.prefs.cardreader.update

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogCardReaderUpdateBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderUpdateDialogFragment : DialogFragment(R.layout.dialog_card_reader_update) {
    val viewModel: CardReaderUpdateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        with(requireDialog()) {
            this.setCanceledOnTouchOutside(false)
            this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                viewModel.onBackPressed()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogCardReaderUpdateBinding.bind(view)

        initObservers(binding)
    }

    private fun initObservers(binding: DialogCardReaderUpdateBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is ExitWithResult<*> -> navigateBackWithResult(
                        KEY_READER_UPDATE_RESULT,
                        event.data as UpdateResult
                    )
                    else -> event.isHandled = false
                }
            }
        )

        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                with(binding) {
                    UiHelpers.setTextOrHide(updateReaderTitleTv, state.title)
                    UiHelpers.setTextOrHide(updateReaderDescriptionTv, state.description)
                    UiHelpers.setTextOrHide(updateReaderProgressWarningTv, state.cancelWarning)
                    UiHelpers.setTextOrHide(updateReaderProgressDescriptionTv, state.progressText)
                    UiHelpers.updateVisibility(updateReaderProgressGroup, state.progress != null)
                    UiHelpers.setTextOrHide(updateReaderPrimaryActionBtn, state.primaryButton?.text)
                    updateReaderPrimaryActionBtn.setOnClickListener { state.primaryButton?.onActionClicked?.invoke() }
                    UiHelpers.setTextOrHide(updateReaderSecondaryActionBtn, state.secondaryButton?.text)
                    updateReaderSecondaryActionBtn.setOnClickListener {
                        state.secondaryButton?.onActionClicked?.invoke()
                    }
                    state.progress?.let {
                        updateReaderPb.progress = it
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    companion object {
        const val KEY_READER_UPDATE_RESULT = "key_reader_update_result"
    }
}
