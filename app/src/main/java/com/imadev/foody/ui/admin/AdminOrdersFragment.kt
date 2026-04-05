package com.imadev.foody.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.imadev.foody.R
import com.imadev.foody.adapter.AdminOrderAdapter
import com.imadev.foody.databinding.FragmentManageOrdersBinding
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminOrdersFragment : BaseFragment<FragmentManageOrdersBinding, AdminViewModel>() {

    override val viewModel: AdminViewModel by activityViewModels()
    
    private lateinit var adminOrderAdapter: AdminOrderAdapter
    private var currentFilter: OrderStatus? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentManageOrdersBinding = FragmentManageOrdersBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adminOrderAdapter = AdminOrderAdapter(emptyList()) { order ->
            val bundle = bundleOf("orderId" to order.id)
            findNavController().navigate(R.id.action_adminOrdersFragment_to_adminOrderDetailsFragment, bundle)
        }
        binding.rvOrders.adapter = adminOrderAdapter
    }

    private fun setupFilters() {
        binding.statusFilterGroup.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.chipPending -> OrderStatus.PENDING
                R.id.chipPreparing -> OrderStatus.PREPARING
                R.id.chipInDelivery -> OrderStatus.IN_DELIVERY
                R.id.chipDelivered -> OrderStatus.DELIVERED
                else -> null
            }
            // Déclencher le filtrage avec la liste actuelle
            applyFilter(viewModel.allOrders.value)
        }
    }

    private fun observeViewModel() {
        viewModel.allOrders.collectFlow(viewLifecycleOwner) { orders ->
            applyFilter(orders)
        }
    }

    private fun applyFilter(orders: List<Order>) {
        val filteredList = if (currentFilter == null) {
            orders
        } else {
            orders.filter { it.status == currentFilter }
        }
        
        adminOrderAdapter.updateList(filteredList)
        
        // S'assurer que la liste remonte en haut lors d'un changement de filtre
        if (filteredList.isNotEmpty()) {
            binding.rvOrders.scrollToPosition(0)
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Manage Orders")
    }
}
