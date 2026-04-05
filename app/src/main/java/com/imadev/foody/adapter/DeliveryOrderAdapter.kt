package com.imadev.foody.adapter

import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.databinding.ItemRowDeliveryOrderBinding
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.model.DeliveryUser
import java.util.*

class DeliveryOrderAdapter(
    private var orders: List<Order>,
    private var currentDriver: DeliveryUser? = null,
    private val onActionClick: (Order) -> Unit
) : RecyclerView.Adapter<DeliveryOrderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRowDeliveryOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRowDeliveryOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        with(holder.binding) {
            orderNumber.text = "Order #${order.id.takeLast(6).uppercase()}"
            clientName.text = order.client.username
            address.text = order.client.address?.address ?: "No address"
            
            val total = order.meals.sumOf { it.price * it.quantity }
            amount.text = String.format("%.2f MAD", total)

            val details = order.meals.joinToString { "${it.quantity}x ${it.name}" }
            orderDetails.text = details

            orderStatusBadge.text = order.status.name
            val badgeColor = when(order.status) {
                OrderStatus.PENDING -> R.color.foody_orange
                OrderStatus.ACCEPTED -> R.color.foody_green
                OrderStatus.PREPARING -> R.color.foody_green
                OrderStatus.IN_DELIVERY -> R.color.black
                else -> R.color.foody_gray
            }
            orderStatusBadge.backgroundTintList = root.context.getColorStateList(badgeColor)

            // CORRECTION CALCUL DISTANCE ET GAINS
            if (currentDriver != null && order.client.address?.latLng != null) {
                val results = FloatArray(1)
                
                val driverLat = currentDriver!!.latitude
                val driverLng = currentDriver!!.longitude
                val clientLat = order.client.address!!.latLng.latitude
                val clientLng = order.client.address!!.latLng.longitude

                if (driverLat != 0.0 && clientLat != 0.0) {
                    Location.distanceBetween(
                        driverLat, driverLng,
                        clientLat, clientLng,
                        results
                    )
                    
                    val distanceInKm = (results[0] / 1000.0) // Conversion explicite en Double
                    
                    // Limiter la distance pour éviter les bugs GPS (ex: 3000km si GPS pas prêt)
                    val finalDistance = if (distanceInKm > 100.0) 0.0 else distanceInKm
                    
                    distanceText.text = String.format("%.1f KM", finalDistance)
                    
                    // Gain = 2 MAD per KM (min 10 MAD)
                    val earning = (finalDistance * 2.0).coerceAtLeast(10.0)
                    earningText.text = String.format("%.2f MAD", earning)
                } else {
                    distanceText.text = "0.0 KM"
                    earningText.text = "10.00 MAD"
                }
            } else {
                distanceText.text = "0.0 KM"
                earningText.text = "10.00 MAD"
            }

            actionButton.visibility = View.VISIBLE
            when (order.status) {
                OrderStatus.PENDING -> actionButton.text = "Accepter la commande"
                OrderStatus.ACCEPTED, OrderStatus.PREPARING -> actionButton.text = "Commencer la livraison"
                OrderStatus.IN_DELIVERY -> actionButton.text = "Marquer comme livré"
                else -> actionButton.visibility = View.GONE
            }

            actionButton.setOnClickListener {
                onActionClick(order)
            }
        }
    }

    override fun getItemCount(): Int = orders.size

    fun updateList(newOrders: List<Order>, driver: DeliveryUser? = null) {
        orders = newOrders
        currentDriver = driver
        notifyDataSetChanged()
    }
}