package com.imadev.foody.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.imadev.foody.R
import com.imadev.foody.adapter.OrderHistoryAdapter
import com.imadev.foody.databinding.FragmentUserDashboardBinding
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserDashboardFragment : BaseFragment<FragmentUserDashboardBinding, UserDashboardViewModel>() {

    override val viewModel: UserDashboardViewModel by viewModels()

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserDashboardBinding = FragmentUserDashboardBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToolbarTitle(requireActivity() as HomeActivity)

        viewModel.activeOrders.collectFlow(viewLifecycleOwner) { orders ->
            // CONFIGURATION DE L'ADAPTER AVEC LES DEUX ACTIONS
            binding.orderList.adapter = OrderHistoryAdapter(
                orders = orders,
                isAdmin = false,
                onDetailsClick = { order ->
                    Log.d("NAV_USER", "Going to Details for: ${order.id}")
                    val bundle = Bundle().apply { putString("orderId", order.id) }
                    findNavController().navigate(R.id.action_userDashboardFragment_to_userOrderDetailsFragment, bundle)
                },
                onTrackClick = { order ->
                    Log.d("NAV_USER", "Going to Tracking for: ${order.id}")
                    val bundle = Bundle().apply { putString("orderId", order.id) }
                    findNavController().navigate(R.id.action_userDashboardFragment_to_orderTrackingFragment, bundle)
                }
            )
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Suivi Commandes")
    }
}
