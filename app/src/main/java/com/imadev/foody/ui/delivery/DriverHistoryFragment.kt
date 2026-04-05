package com.imadev.foody.ui.delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.imadev.foody.R
import com.imadev.foody.adapter.DeliveryOrderAdapter
import com.imadev.foody.databinding.FragmentDriverHistoryBinding
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.collectFlow
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DriverHistoryFragment : BaseFragment<FragmentDriverHistoryBinding, DeliveryViewModel>() {

    override val viewModel: DeliveryViewModel by viewModels()
    
    private lateinit var historyAdapter: DeliveryOrderAdapter
    private var currentDriver: DeliveryUser? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDriverHistoryBinding = FragmentDriverHistoryBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // Dans l'historique, pas d'actions de clic nécessaires sur le bouton
        historyAdapter = DeliveryOrderAdapter(emptyList()) { _ -> }
        binding.historyList.adapter = historyAdapter
    }

    private fun observeViewModel() {
        // Observer le profil pour les calculs de distance si besoin
        viewModel.currentDriver.collectFlow(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                currentDriver = resource.data
            }
        }

        // Observer l'historique (commandes livrées par ce livreur)
        viewModel.historyOrders.collectFlow(viewLifecycleOwner) { orders ->
            historyAdapter.updateList(orders, currentDriver)
            
            if (orders.isEmpty()) {
                binding.historyList.hide()
                binding.emptyHistoryLayout.show()
            } else {
                binding.historyList.show()
                binding.emptyHistoryLayout.hide()
            }
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Archive Livraisons")
    }
}
