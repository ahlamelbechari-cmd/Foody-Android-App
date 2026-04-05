package com.imadev.foody.ui.delivery

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.imadev.foody.model.*
import com.imadev.foody.repository.FoodyRepo
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants.ORDERS_COLLECTION
import com.imadev.foody.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "DeliveryViewModel"

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val repository: FoodyRepo
) : BaseViewModel() {

    private val databaseUrl = "https://foody-app-a1b12-default-rtdb.firebaseio.com/"
    private val realtimeDb = FirebaseDatabase.getInstance(databaseUrl).reference
    private val auth = FirebaseAuth.getInstance()

    private val _currentDriver = MutableStateFlow<Resource<DeliveryUser?>>(Resource.Loading())
    val currentDriver = _currentDriver.asStateFlow()

    private val _availableOrders = MutableStateFlow<List<Order>>(emptyList())
    val availableOrders = _availableOrders.asStateFlow()

    private val _myOrders = MutableStateFlow<List<Order>>(emptyList())
    val myOrders = _myOrders.asStateFlow()

    private val _historyOrders = MutableStateFlow<List<Order>>(emptyList())
    val historyOrders = _historyOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val fullOrdersList = mutableListOf<Order>()

    init {
        viewModelScope.launch {
            while (auth.uid == null) delay(500)
            val uid = auth.uid!!
            loadDriverProfile(uid)
            observeAllOrders(uid)
        }
    }

    private fun loadDriverProfile(uid: String) {
        realtimeDb.child("delivery-users").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driver = snapshot.getValue(DeliveryUser::class.java)
                _currentDriver.value = Resource.Success(driver)
            }
            override fun onCancelled(error: DatabaseError) {
                _currentDriver.value = Resource.Error(error.toException())
            }
        })
    }

    private fun observeAllOrders(uid: String) {
        _isLoading.value = true
        realtimeDb.child(ORDERS_COLLECTION).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ordersList = mutableListOf<Order>()
                for (orderSnap in snapshot.children) {
                    try {
                        val order = orderSnap.getValue(Order::class.java)
                        order?.let { 
                            it.id = orderSnap.key ?: ""
                            ordersList.add(it) 
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order: ${e.message}")
                    }
                }
                fullOrdersList.clear()
                fullOrdersList.addAll(ordersList)
                _isLoading.value = false
                filterOrders(uid)
            }
            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        })
    }

    private fun filterOrders(currentDriverId: String) {
        val available = fullOrdersList.filter { order ->
            order.status == OrderStatus.PENDING && (order.to == null || order.to == "")
        }.sortedByDescending { it.date }

        val mine = fullOrdersList.filter { order ->
            order.to == currentDriverId && 
            order.status != OrderStatus.DELIVERED && 
            order.status != OrderStatus.CANCELLED
        }.sortedByDescending { it.date }

        val history = fullOrdersList.filter { order ->
            order.to == currentDriverId && 
            (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED)
        }.sortedByDescending { it.date }

        _availableOrders.value = available
        _myOrders.value = mine
        _historyOrders.value = history
    }

    fun toggleStatus() = viewModelScope.launch {
        val uid = auth.uid ?: return@launch
        val currentStatus = (currentDriver.value as? Resource.Success)?.data?.available ?: false
        realtimeDb.child("delivery-users").child(uid).child("available").setValue(!currentStatus).await()
    }

    fun acceptOrder(order: Order) = viewModelScope.launch {
        val uid = auth.uid ?: return@launch
        val updates = mutableMapOf<String, Any>()
        updates["status"] = OrderStatus.ACCEPTED
        updates["to"] = uid 
        realtimeDb.child(ORDERS_COLLECTION).child(order.id).updateChildren(updates).await()
    }

    fun updateLocation(lat: Double, lon: Double) = viewModelScope.launch {
        val uid = auth.uid ?: return@launch
        val updates = mapOf("latitude" to lat, "longitude" to lon)
        realtimeDb.child("delivery-users").child(uid).updateChildren(updates)
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) = viewModelScope.launch {
        realtimeDb.child(ORDERS_COLLECTION).child(orderId).child("status").setValue(status).await()
    }

    fun startDelivery(order: Order) = viewModelScope.launch {
        updateOrderStatus(order.id, OrderStatus.IN_DELIVERY)
    }

    fun completeDelivery(order: Order) = viewModelScope.launch {
        updateOrderStatus(order.id, OrderStatus.DELIVERED)
    }
}