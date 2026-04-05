package com.imadev.foody.ui.delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.imadev.foody.R
import com.imadev.foody.adapter.DeliveryOrderAdapter
import com.imadev.foody.databinding.FragmentDeliveryDashboardBinding
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.collectFlow
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeliveryDashboardFragment : BaseFragment<FragmentDeliveryDashboardBinding, DeliveryViewModel>() {

    override val viewModel: DeliveryViewModel by viewModels()
    
    private lateinit var availableAdapter: DeliveryOrderAdapter
    
    private var currentDriver: DeliveryUser? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDeliveryDashboardBinding = FragmentDeliveryDashboardBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()

        binding.statusSwitch.setOnClickListener {
            viewModel.toggleStatus()
        }
    }

    private fun setupRecyclerViews() {
        availableAdapter = DeliveryOrderAdapter(emptyList()) { order ->
            viewModel.acceptOrder(order)
        }
        binding.availableOrdersList.adapter = availableAdapter
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.collectFlow(viewLifecycleOwner) { isLoading ->
            if (isLoading) (activity as? HomeActivity)?.showProgressBar()
            else (activity as? HomeActivity)?.hideProgressBar()
        }

        // Observe driver profile
        viewModel.currentDriver.collectFlow(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> (activity as? HomeActivity)?.showProgressBar()
                is Resource.Error -> {
                    (activity as? HomeActivity)?.hideProgressBar()
                    Toast.makeText(context, "Error: ${resource.error?.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    (activity as? HomeActivity)?.hideProgressBar()
                    val driver = resource.data
                    currentDriver = driver
                    updateUI(driver)
                }
            }
        }

        // Dashboard : UNIQUEMENT les commandes disponibles
        viewModel.availableOrders.collectFlow(viewLifecycleOwner) { orders ->
            availableAdapter.updateList(orders, currentDriver)
            if (orders.isEmpty()) {
                binding.availableTitle.hide()
                binding.availableOrdersList.hide()
            } else if (currentDriver?.available == true) {
                binding.availableTitle.show()
                binding.availableOrdersList.show()
            }
        }
    }

    private fun updateUI(driver: DeliveryUser?) {
        binding.driverName.text = driver?.username ?: "Livreur"
        binding.statusSwitch.isChecked = driver?.available ?: false
        binding.earnings.text = String.format("%.2f MAD", driver?.earnings ?: 0.0)
        binding.deliveries.text = (driver?.totalDeliveries ?: 0).toString()

        if (driver?.available == false) {
            binding.contentLayout.hide()
            binding.offDutyMessage.show()
        } else {
            binding.contentLayout.show()
            binding.offDutyMessage.hide()
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitle(R.string.app_name)
    }
}
