package com.imadev.foody.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.imadev.foody.R
import com.imadev.foody.adapter.OrderHistoryAdapter
import com.imadev.foody.databinding.FragmentHistoryBinding
import com.imadev.foody.model.Order
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.checkout.CheckoutViewModel
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding, HistoryViewModel>() {

    override val viewModel: HistoryViewModel by viewModels()
    private val checkoutViewModel: CheckoutViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.historyOrders.collectFlow(viewLifecycleOwner) { orders ->
            if (orders.isNotEmpty()) {
                binding.noHistoryContainer.hide()
                binding.list.show()
                
                binding.list.adapter = OrderHistoryAdapter(
                    orders = orders,
                    isAdmin = false,
                    onDetailsClick = { order ->
                        // Dans l'historique (Uniquement DELIVERED), on fait un reorder
                        reorder(order)
                    },
                    onTrackClick = null // Pas de suivi dans l'historique car que du DELIVERED
                )
            } else {
                binding.noHistoryContainer.show()
                binding.list.hide()
            }
        }
    }

    private fun reorder(order: Order) {
        checkoutViewModel.resetList()
        order.meals.forEach { meal ->
            checkoutViewModel.addToCart(meal)
        }
        
        Toast.makeText(requireContext(), "Items added to cart", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.cartFragment)
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHistoryBinding = FragmentHistoryBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        setToolbarTitle(requireActivity() as HomeActivity)
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitle(R.string.history)
    }
}
