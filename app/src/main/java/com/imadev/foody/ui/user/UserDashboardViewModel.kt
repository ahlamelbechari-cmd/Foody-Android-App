package com.imadev.foody.ui.user

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants.ORDERS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val TAG = "UserDashboardViewModel"

@HiltViewModel
class UserDashboardViewModel @Inject constructor() : BaseViewModel() {

    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference
    private val auth = FirebaseAuth.getInstance()

    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val activeOrders = _activeOrders.asStateFlow()

    init {
        observeActiveOrders()
    }

    private fun observeActiveOrders() {
        val uid = auth.uid ?: return
        realtimeDb.child(ORDERS_COLLECTION).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                snapshot.children.forEach { doc ->
                    val order = doc.getValue(Order::class.java)?.apply { id = doc.key ?: "" }
                    if (order?.uid == uid) {
                        // STRICTEMENT : En cours uniquement
                        if (order.status == OrderStatus.PENDING ||
                            order.status == OrderStatus.PREPARING ||
                            order.status == OrderStatus.IN_DELIVERY) {
                            orders.add(order)
                        }
                    }
                }
                _activeOrders.value = orders.sortedByDescending { it.date }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeActiveOrders: ${error.message}")
            }
        })
    }
}