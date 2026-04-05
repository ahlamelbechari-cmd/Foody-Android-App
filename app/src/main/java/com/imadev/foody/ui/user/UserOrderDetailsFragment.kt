package com.imadev.foody.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.imadev.foody.adapter.CartAdapter
import com.imadev.foody.databinding.FragmentUserOrderDetailsBinding
import com.imadev.foody.model.Order
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.admin.AdminViewModel
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class UserOrderDetailsFragment : BaseFragment<FragmentUserOrderDetailsBinding, AdminViewModel>() {

    override val viewModel: AdminViewModel by activityViewModels()
    
    private var orderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString("orderId")
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserOrderDetailsBinding =
        FragmentUserOrderDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeOrder()
    }

    private fun observeOrder() {
        viewModel.allOrders.collectFlow(viewLifecycleOwner) { orders ->
            val order = orders.find { it.id == orderId }
            order?.let { displayOrderDetails(it) }
        }
    }

    private fun displayOrderDetails(order: Order) {
        with(binding) {
            tvOrderId.text = "Order #${order.id.takeLast(6).uppercase()}"
            tvStatus.text = order.status.name
            
            // Affichage de l'adresse textuelle au lieu des coordonnées
            val addressObj = order.client.address
            if (addressObj != null && !addressObj.address.isNullOrEmpty()) {
                val fullAddress = buildString {
                    append(addressObj.address)
                    if (!addressObj.city.isNullOrEmpty()) append(", ${addressObj.city}")
                }
                tvDeliveryAddress.text = fullAddress
            } else {
                tvDeliveryAddress.text = "Lat: ${order.deliveryLat}, Lon: ${order.deliveryLon}"
            }
            
            // Logique d'affichage du livreur
            if (order.to.isNullOrEmpty()) {
                tvDriverName.text = "Livreur: Non assigné"
                tvVehicleInfo.visibility = View.GONE
                tvDriverPhone.visibility = View.GONE
            } else {
                val driver = viewModel.allDrivers.value.find { it.id == order.to }
                
                if (driver != null) {
                    tvDriverName.text = "Livreur: ${driver.username}"
                    
                    val vehicle = driver.transportType ?: "Moto"
                    val matricule = if (!driver.vehicleRegistration.isNullOrEmpty()) {
                        " - Matricule: ${driver.vehicleRegistration}"
                    } else ""
                    
                    tvVehicleInfo.text = "$vehicle$matricule"
                    tvVehicleInfo.visibility = View.VISIBLE
                    
                    if (!driver.phone.isNullOrEmpty()) {
                        tvDriverPhone.text = "Tél: ${driver.phone}"
                        tvDriverPhone.visibility = View.VISIBLE
                    } else {
                        tvDriverPhone.visibility = View.GONE
                    }
                } else {
                    tvDriverName.text = "Livreur: Assigné (${order.to})"
                    tvVehicleInfo.visibility = View.GONE
                    tvDriverPhone.visibility = View.GONE
                }
            }

            val adapter = CartAdapter(order.meals.toMutableList(), isEditable = false)
            rvOrderItems.adapter = adapter
            
            val calculatedTotal = if (order.totalPrice > 0) {
                order.totalPrice
            } else {
                order.meals.sumOf { it.price * it.quantity }
            }
            tvTotalPrice.text = "${calculatedTotal} MAD"
            
            val sdf = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault())
            tvOrderDate.text = "Commandée le ${sdf.format(Date(order.date))}"
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Détails de la commande")
    }
}
