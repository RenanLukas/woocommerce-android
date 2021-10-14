package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackBenefitsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JetpackBenefitsDialog : DialogFragment(R.layout.dialog_jetpack_benefits) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogJetpackBenefitsBinding.bind(view)
    }
}
