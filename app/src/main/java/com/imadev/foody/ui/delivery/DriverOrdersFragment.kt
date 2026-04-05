package com.imadev.foody.ui.delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.imadev.foody.R
import com.imadev.foody.adapter.DeliveryOrderAdapter
import com.imadev.foody.databinding.FragmentDriverOrdersBinding
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.collectFlow
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DriverOrdersFragment : BaseFragment<FragmentDriverOrdersBinding, DeliveryViewModel>() {

    override val viewModel: DeliveryViewModel by viewModels()
    
    private lateinit var myOrdersAdapter: DeliveryOrderAdapter
    
    private var currentDriver: DeliveryUser? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDriverOrdersBinding = FragmentDriverOrdersBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        myOrdersAdapter = DeliveryOrderAdapter(emptyList()) { order ->
            when (order.status) {
                OrderStatus.ACCEPTED, OrderStatus.PREPARING -> {
                    viewModel.startDelivery(order)
                    // REDIRECTION AUTOMATIQUE VERS TRACKING
                    findNavController().navigate(R.id.mapsFragment)
                }
                OrderStatus.IN_DELIVERY -> viewModel.completeDelivery(order)
                else -> {}
            }
        }
        binding.myOrdersList.adapter = myOrdersAdapter
    }

    private fun observeViewModel() {
        // Observe driver profile
        viewModel.currentDriver.collectFlow(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val driver = resource.data
                currentDriver = driver
                
                if (driver?.available == false) {
                    binding.ordersScroll.hide()
                    binding.offDutyMessage.show()
                } else {
                    binding.ordersScroll.show()
                    binding.offDutyMessage.hide()
                }
            }
        }

        // Observe My Orders
        viewModel.myOrders.collectFlow(viewLifecycleOwner) { orders ->
            myOrdersAdapter.updateList(orders, currentDriver)
            if (orders.isEmpty()) {
                binding.myOrdersTitle.hide()
                binding.myOrdersList.hide()
                binding.emptyMyOrdersLayout.show()
            } else {
                binding.myOrdersTitle.show()
                binding.myOrdersList.show()
                binding.emptyMyOrdersLayout.hide()
            }
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Mes Commandes")
    }
}
