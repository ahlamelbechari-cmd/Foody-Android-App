package com.imadev.foody.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

class OrderHistoryAdapter(
    private val orders: List<Order>,
    private val isAdmin: Boolean = false,
    private val onDetailsClick: (Order) -> Unit,
    private val onTrackClick: ((Order) -> Unit)? = null
) : RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderId: TextView = itemView.findViewById(R.id.order_id)
        val orderStatus: TextView = itemView.findViewById(R.id.order_status)
        val orderDate: TextView = itemView.findViewById(R.id.order_date)
        val orderItems: TextView = itemView.findViewById(R.id.order_items)
        val orderTotal: TextView = itemView.findViewById(R.id.order_total)
        val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        val btnDetails: Button = itemView.findViewById(R.id.btnDetails)
        val btnTrack: Button = itemView.findViewById(R.id.btnTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_row_order_history,
            parent,
            false
        )
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        val context = holder.itemView.context

        holder.orderId.text = "Order #${order.id.takeLast(6).uppercase()}"
        
        val sdf = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault())
        holder.orderDate.text = sdf.format(Date(order.date))

        // UI Status
        when(order.status) {
            OrderStatus.PENDING -> {
                holder.orderStatus.text = "En attente"
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, R.color.foody_orange))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.foody_orange))
            }
            OrderStatus.ACCEPTED, OrderStatus.PREPARING -> {
                holder.orderStatus.text = "En préparation"
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            }
            OrderStatus.IN_DELIVERY -> {
                holder.orderStatus.text = "En livraison"
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, R.color.foody_orange))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.foody_orange))
            }
            OrderStatus.DELIVERED -> {
                holder.orderStatus.text = "Livrée"
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, R.color.foody_green))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.foody_green))
            }
            else -> {
                holder.orderStatus.text = "Annulée"
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, R.color.foody_gray))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.foody_gray))
            }
        }

        // Visibility and Text Logic
        if (isAdmin) {
            holder.btnTrack.visibility = View.GONE
            holder.btnDetails.visibility = View.VISIBLE
            holder.btnDetails.text = "Détails"
        } else {
            when(order.status) {
                OrderStatus.IN_DELIVERY -> {
                    holder.btnTrack.visibility = View.VISIBLE
                    holder.btnDetails.visibility = View.VISIBLE
                    holder.btnTrack.text = "Suivre"
                    holder.btnDetails.text = "Détails"
                }
                OrderStatus.DELIVERED -> {
                    holder.btnTrack.visibility = View.GONE
                    holder.btnDetails.visibility = View.VISIBLE
                    holder.btnDetails.text = "Recommander"
                }
                else -> {
                    holder.btnTrack.visibility = View.GONE
                    holder.btnDetails.visibility = View.VISIBLE
                    holder.btnDetails.text = "Détails"
                }
            }
        }

        val itemsText = order.meals.joinToString("\n") { "• ${it.name} x${it.quantity}" }
        holder.orderItems.text = itemsText
        
        val total = if (order.totalPrice > 0) order.totalPrice else order.meals.sumOf { it.price * it.quantity }
        holder.orderTotal.text = "${total} MAD"

        // Click Listeners
        holder.btnDetails.setOnClickListener {
            Log.d("ORDER_CLICK", "Details clicked for: ${order.id}")
            onDetailsClick(order)
        }

        holder.btnTrack.setOnClickListener {
            Log.d("ORDER_CLICK", "Track clicked for: ${order.id}")
            onTrackClick?.invoke(order)
        }
    }

    override fun getItemCount(): Int = orders.size
}
