package com.imadev.foody.ui.checkout

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.imadev.foody.fcm.remote.PushNotification
import com.imadev.foody.model.*
import com.imadev.foody.repository.FoodyRepo
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.formatDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CheckoutViewModel"

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    val repository: FoodyRepo
) : BaseViewModel() {


    private val _client = MutableStateFlow<Resource<Client?>>(Resource.Loading())
    val client = _client.asStateFlow()

    private var _cartList: MutableList<Meal> = mutableListOf()
    val cartList: List<Meal> get() = _cartList

    private val _totalPrice = MutableStateFlow("0.00")
    val totalPrice = _totalPrice.asStateFlow()

    private var _availableDeliveryUsers =
        MutableSharedFlow<Resource<List<DeliveryUser?>>>()
    val availableDeliveryUsers = _availableDeliveryUsers.asSharedFlow()

    private var _notificationSent = MutableStateFlow(false)
    var notificationSent = _notificationSent.asStateFlow()


    var order = Order()
        private set

    fun setOrder(order: Order) {
        this.order = order
    }

    fun computeTotal() {
        val total = _cartList.sumOf { (it.quantity.takeIf { q -> q > 0 } ?: 1) * it.price }
        _totalPrice.value = formatDecimal(total)
    }

    fun getTotal(): String {
        computeTotal()
        return _totalPrice.value
    }

    fun addToCart(meal: Meal, position: Int) {
        _cartList.add(position, meal)
        computeTotal()
        updateCartEmptiness()
        observeQuantity()
    }

    fun addToCart(meal: Meal) {
        _cartList.add(meal)
        computeTotal()
        updateCartEmptiness()
        observeQuantity()
    }

    fun removeFromCart(meal: Meal) {
        _cartList.remove(meal)
        computeTotal()
        updateCartEmptiness()
        observeQuantity()
    }

    fun removeFromCart(position: Int) {
        _cartList.removeAt(position)
        computeTotal()
        updateCartEmptiness()
        observeQuantity()
    }

    fun resetList() {
        _cartList.clear()
        computeTotal()
        updateCartEmptiness()
        observeQuantity()
    }

    private val _cartIsEmpty = MutableLiveData<Boolean>()
    val cartIsEmpty: LiveData<Boolean> = _cartIsEmpty

    val canProceedToPayment = MutableLiveData<Boolean>()


    init {
        updateCartEmptiness()
        observeQuantity()
        computeTotal()
    }

    private fun updateCartEmptiness() {
        _cartIsEmpty.postValue(_cartList.isEmpty())
    }


    fun observeQuantity() {
        // Le bouton est activé si le panier n'est pas vide
        canProceedToPayment.postValue(_cartList.isNotEmpty())
        computeTotal() // Mise à jour du total quand la quantité change
    }


    fun getClient(uid: String) = viewModelScope.launch {
        repository.getClient(uid).collectLatest {
            _client.emit(it)
        }
    }


    fun updateAddress(uid: String, address: Address) = GlobalScope.launch {
        repository.updateField(Constants.CLIENTS_COLLECTION, uid, "address", address).collect {
            when (it) {
                is Resource.Error -> {
                    Log.d(TAG, "updateAddress: ${it.error?.message}")
                }
                is Resource.Loading -> {


                }
                is Resource.Success -> {
                    Log.d(TAG, "updateAddress: success")
                }
            }
        }
    }


    private fun sendNotification(pushNotification: PushNotification) = viewModelScope.launch {
        val response = repository.sendNotification(pushNotification)

        _notificationSent.emit(response.isSuccessful)

    }


    fun getAvailableDeliveryUsers() {
        viewModelScope.launch {
            repository.getAvailableDeliveryUsers().collect {
                _availableDeliveryUsers.emit(it)
            }
        }
    }


    fun sendOrder(order: Order) = viewModelScope.launch {
        repository.sendOrder(order).collectLatest { 
            when(it) {
                is Resource.Error ->{
                    Log.d(TAG, "sendOrder: ${it.error?.message}")
                }
                is Resource.Loading -> {

                }
                is Resource.Success -> {
                    // Commande envoyée avec succès
                }
            }
        }

    }



}
