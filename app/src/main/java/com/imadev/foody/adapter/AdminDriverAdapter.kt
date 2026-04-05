package com.imadev.foody.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.databinding.ItemRowAdminDriverBinding
import com.imadev.foody.model.DeliveryUser

class AdminDriverAdapter(
    private var drivers: List<DeliveryUser>,
    private val onToggleStatus: (String, Boolean) -> Unit,
    private val onDetailsClick: (DeliveryUser) -> Unit,
    private val onDeleteClick: (DeliveryUser) -> Unit
) : RecyclerView.Adapter<AdminDriverAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRowAdminDriverBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRowAdminDriverBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val driver = drivers[position]
        with(holder.binding) {
            driverName.text = driver.username
            transportType.text = driver.transportType
            deliveryCount.text = "${driver.totalDeliveries} Livraisons"
            
            val color = if (driver.available) R.color.foody_green else R.color.foody_orange
            statusIndicator.backgroundTintList = root.context.getColorStateList(color)

            statusSwitch.isChecked = driver.available
            statusSwitch.setOnCheckedChangeListener { _, isChecked ->
                driver.id?.let { onToggleStatus(it, isChecked) }
            }

            btnDetails.setOnClickListener { onDetailsClick(driver) }
            btnDeleteDriver.setOnClickListener { onDeleteClick(driver) }
        }
    }

    override fun getItemCount(): Int = drivers.size

    fun updateList(newList: List<DeliveryUser>) {
        drivers = newList
        notifyDataSetChanged()
    }
}
