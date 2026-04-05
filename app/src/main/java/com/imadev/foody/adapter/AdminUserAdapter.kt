package com.imadev.foody.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.databinding.ItemUserAdminBinding
import com.imadev.foody.model.Client

class AdminUserAdapter(
    private var users: List<Client>,
    private val onDetailsClick: (Client) -> Unit,
    private val onDeleteClick: (Client) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemUserAdminBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: Client) {
            with(binding) {
                tvName.text = user.username ?: "Utilisateur"
                tvEmail.text = user.email ?: "Pas d'email"
                tvRoleBadge.text = user.role.uppercase()
                
                btnDetails.setOnClickListener { onDetailsClick(user) }
                btnDelete.setOnClickListener { onDeleteClick(user) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserAdminBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<Client>) {
        users = newList
        notifyDataSetChanged()
    }
}
