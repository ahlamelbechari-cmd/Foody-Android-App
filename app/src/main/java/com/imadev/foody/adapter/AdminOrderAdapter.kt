package com.imadev.foody.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.model.Order
import java.text.SimpleDateFormat
import java.util.*

class AdminOrderAdapter(
    private var orders: List<Order>,
    private val onDetailsClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val tvOrderItems: TextView = itemView.findViewById(R.id.tvOrderItems)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val btnDetails: Button = itemView.findViewById(R.id.btnDetails)

        fun bind(order: Order) {
            tvOrderNumber.text = "Order #${order.id.takeLast(6).uppercase()}"
            tvStatusBadge.text = order.status.name

            val sdf = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault())
            tvOrderDate.text = sdf.format(Date(order.date))

            val itemsSummary = order.meals.joinToString("\n") { "• ${it.name} x${it.quantity}" }
            tvOrderItems.text = itemsSummary

            val calculatedTotal = if (order.totalPrice > 0) {
                order.totalPrice
            } else {
                order.meals.sumOf { it.price * it.quantity }
            }

            tvTotalAmount.text = "${calculatedTotal} MAD"

            btnDetails.setOnClickListener {
                Log.d("DETAIL_CLICK", "Clicked order: ${order.id}")
                onDetailsClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_admin_order,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateList(newList: List<Order>) {
        orders = newList
        notifyDataSetChanged()
    }
}
