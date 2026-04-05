package com.imadev.foody.ui.user

import androidx.lifecycle.viewModelScope
import com.imadev.foody.model.Address
import com.imadev.foody.model.Client
import com.imadev.foody.repository.FoodyRepo
import com.imadev.foody.ui.common.BaseViewModel
import com.imadev.foody.utils.Constants
import com.imadev.foody.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    val repository: FoodyRepo
) : BaseViewModel() {

    private val _client = MutableStateFlow<Resource<Client?>>(Resource.Loading())
    val client = _client.asStateFlow()

    private val _updateStatus = MutableStateFlow<Resource<Void?>>(Resource.Success(null))
    val updateStatus = _updateStatus.asStateFlow()

    fun getClient(uid: String) = viewModelScope.launch {
        repository.getClient(uid).collectLatest {
            _client.value = it
        }
    }

    fun updateProfile(uid: String, username: String, phone: String, city: String, addressStr: String) = viewModelScope.launch {
        _updateStatus.value = Resource.Loading()
        
        // Update username
        repository.updateField(Constants.CLIENTS_COLLECTION, uid, "username", username).collectLatest {
            if (it is Resource.Success) {
                // Update phone
                repository.updateField(Constants.CLIENTS_COLLECTION, uid, "phone", phone).collectLatest { res ->
                    if (res is Resource.Success) {
                        // Get current client to get current address (latlng etc)
                        val currentClient = (_client.value as? Resource.Success)?.data
                        val newAddress = (currentClient?.address ?: Address()).copy(city = city, address = addressStr)
                        
                        repository.updateField(Constants.CLIENTS_COLLECTION, uid, "address", newAddress).collectLatest { addrRes ->
                            _updateStatus.value = addrRes
                            if (addrRes is Resource.Success) {
                                getClient(uid)
                            }
                        }
                    } else if (res is Resource.Error) {
                        _updateStatus.value = Resource.Error(res.error)
                    }
                }
            } else if (it is Resource.Error) {
                _updateStatus.value = Resource.Error(it.error)
            }
        }
    }
}