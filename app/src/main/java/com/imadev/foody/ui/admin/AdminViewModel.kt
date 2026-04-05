package com.imadev.foody.ui.admin

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.imadev.foody.model.*
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants.CLIENTS_COLLECTION
import com.imadev.foody.utils.Constants.ORDERS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "AdminViewModel"

@HiltViewModel
class AdminViewModel @Inject constructor() : BaseViewModel() {

    private val databaseUrl = "https://foody-app-a1b12-default-rtdb.firebaseio.com/"
    private val realtimeDb = FirebaseDatabase.getInstance(databaseUrl).reference

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders = _allOrders.asStateFlow()

    private val _allUsers = MutableStateFlow<List<Client>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _allDrivers = MutableStateFlow<List<DeliveryUser>>(emptyList())
    val allDrivers = _allDrivers.asStateFlow()

    init {
        observeOrders()
        observeUsers()
        observeDrivers()
    }

    private fun observeOrders() {
        realtimeDb.child(ORDERS_COLLECTION).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                snapshot.children.forEach { doc ->
                    try {
                        doc.getValue(Order::class.java)?.let { order ->
                            order.id = doc.key ?: ""
                            orders.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order ${doc.key}: ${e.message}")
                    }
                }
                _allOrders.value = orders.sortedByDescending { it.date }
            }
            override fun onCancelled(error: DatabaseError) { Log.e(TAG, error.message) }
        })
    }

    private fun observeUsers() {
        realtimeDb.child(CLIENTS_COLLECTION).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<Client>()
                snapshot.children.forEach { child ->
                    try {
                        child.getValue(Client::class.java)?.let { client ->
                            client.id = child.key
                            users.add(client)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user ${child.key}: ${e.message}")
                    }
                }
                _allUsers.value = users
            }
            override fun onCancelled(error: DatabaseError) { Log.e(TAG, error.message) }
        })
    }

    private fun observeDrivers() {
        realtimeDb.child("delivery-users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drivers = mutableListOf<DeliveryUser>()
                snapshot.children.forEach { doc ->
                    try {
                        doc.getValue(DeliveryUser::class.java)?.let { driver ->
                            driver.id = doc.key ?: ""
                            drivers.add(driver)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing driver ${doc.key}: ${e.message}")
                    }
                }
                _allDrivers.value = drivers
            }
            override fun onCancelled(error: DatabaseError) { Log.e(TAG, error.message) }
        })
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) = viewModelScope.launch {
        realtimeDb.child(ORDERS_COLLECTION).child(orderId).child("status").setValue(status).await()
    }

    fun deleteOrder(orderId: String) = viewModelScope.launch {
        realtimeDb.child(ORDERS_COLLECTION).child(orderId).removeValue().await()
    }

    fun deleteUser(userId: String) = viewModelScope.launch {
        realtimeDb.child(CLIENTS_COLLECTION).child(userId).removeValue().await()
    }

    fun deleteDriver(driverId: String) = viewModelScope.launch {
        val updates = hashMapOf<String, Any?>()
        updates["$CLIENTS_COLLECTION/$driverId"] = null
        updates["delivery-users/$driverId"] = null
        realtimeDb.updateChildren(updates).await()
    }

    fun toggleDriverStatus(driverId: String, isActive: Boolean) = viewModelScope.launch {
        realtimeDb.child("delivery-users").child(driverId).child("available").setValue(isActive).await()
    }

    fun createDriver(driver: DeliveryUser) = viewModelScope.launch {
        val driverId = realtimeDb.child("delivery-users").push().key ?: return@launch
        
        val client = Client(
            id = driverId,
            username = driver.username,
            email = driver.email,
            phone = driver.phone,
            role = Client.ROLE_DELIVERY
        )

        val updates = hashMapOf<String, Any>()
        updates["$CLIENTS_COLLECTION/$driverId"] = client
        updates["delivery-users/$driverId"] = driver.apply { id = driverId }

        realtimeDb.updateChildren(updates).await()
    }
}
