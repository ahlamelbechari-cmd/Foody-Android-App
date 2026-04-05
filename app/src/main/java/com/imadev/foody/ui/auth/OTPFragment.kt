package com.imadev.foody.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.imadev.foody.databinding.FragmentOTPBinding
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OTPFragment : BaseFragment<FragmentOTPBinding, AuthViewModel>() {
    override val viewModel: AuthViewModel by viewModels()


    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOTPBinding = FragmentOTPBinding.inflate(layoutInflater, container, false)

    override fun setToolbarTitle(activity: HomeActivity) {
        // Implémentation si nécessaire
    }
}
