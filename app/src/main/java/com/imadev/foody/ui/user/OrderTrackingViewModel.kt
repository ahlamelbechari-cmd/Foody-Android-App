package com.imadev.foody.ui.user

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.model.Order
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants.ORDERS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OrderTrackingViewModel"

@HiltViewModel
class OrderTrackingViewModel @Inject constructor() : BaseViewModel() {

    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference

    private val _currentOrder = MutableStateFlow<Order?>(null)
    val currentOrder = _currentOrder.asStateFlow()

    private val _driverLocation = MutableStateFlow<LatLng?>(null)
    val driverLocation = _driverLocation.asStateFlow()

    fun trackOrder(orderId: String) {
        realtimeDb.child(ORDERS_COLLECTION).child(orderId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Order::class.java)
                _currentOrder.value = order
                order?.to?.let { driverId ->
                    trackDriver(driverId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "trackOrder: ${error.message}")
            }
        })
    }

    private fun trackDriver(driverId: String) {
        realtimeDb.child("delivery-users").child(driverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driver = snapshot.getValue(DeliveryUser::class.java)
                if (driver != null) {
                    _driverLocation.value = LatLng(driver.latitude, driver.longitude)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "trackDriver: ${error.message}")
            }
        })
    }
}