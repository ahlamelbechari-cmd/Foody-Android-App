package com.imadev.foody.ui.history

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

private const val TAG = "HistoryViewModel"

@HiltViewModel
class HistoryViewModel @Inject constructor() : BaseViewModel() {

    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference
    private val auth = FirebaseAuth.getInstance()

    private val _historyOrders = MutableStateFlow<List<Order>>(emptyList())
    val historyOrders = _historyOrders.asStateFlow()

    init {
        observeHistoryOrders()
    }

    private fun observeHistoryOrders() {
        val uid = auth.uid ?: return
        realtimeDb.child(ORDERS_COLLECTION).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                snapshot.children.forEach { doc ->
                    val order = doc.getValue(Order::class.java)?.apply { id = doc.key ?: "" }
                    // STRICTEMENT : Uniquement les commandes LIVRÉES
                    if (order?.uid == uid && order.status == OrderStatus.DELIVERED) {
                        orders.add(order)
                    }
                }
                _historyOrders.value = orders.sortedByDescending { it.date }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeHistoryOrders: ${error.message}")
            }
        })
    }
}