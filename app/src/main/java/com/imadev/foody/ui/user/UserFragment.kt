package com.imadev.foody.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentUserBinding
import com.imadev.foody.model.Client
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.auth.LoginActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class UserFragment : BaseFragment<FragmentUserBinding, UserViewModel>() {

    override val viewModel: UserViewModel by viewModels()
    private val mapsViewModel: com.imadev.foody.ui.map.MapsViewModel by activityViewModels()

    private var userRole: String = Client.ROLE_USER
    private var isInitialDataLoaded = false
    
    private val databaseUrl = "https://foody-app-a1b12-default-rtdb.firebaseio.com/"
    private val dbRef = FirebaseDatabase.getInstance(databaseUrl).reference

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserBinding = FragmentUserBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().uid ?: return
        viewModel.getClient(uid)

        setupVehicleDropdown()
        observeUser(uid)
        observeMaps()

        binding.changeAddress.setOnClickListener {
            viewModel.navigate(R.id.mapsFragment)
        }

        binding.viewOrdersBtn.setOnClickListener {
            viewModel.navigate(R.id.action_userFragment_to_userDashboardFragment)
        }

        binding.saveBtn.setOnClickListener {
            saveAllInformation(uid)
        }

        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            requireContext().getSharedPreferences(Constants.FCM_TOKEN_PREF, android.content.Context.MODE_PRIVATE).edit().clear().apply()
            moveTo(LoginActivity::class.java)
        }

        binding.driverStatusSwitch.setOnClickListener {
            val isChecked = binding.driverStatusSwitch.isChecked
            updateDriverStatusInDb(uid, isChecked)
        }
    }

    private fun setupVehicleDropdown() {
        val types = arrayOf("Moto", "Voiture")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.vehicleTypeInput.setAdapter(adapter)
    }

    private fun saveAllInformation(uid: String) {
        val name = if (userRole == Client.ROLE_ADMIN) binding.adminNameInput.text.toString().trim() else binding.userNameInput.text.toString().trim()
        val phone = if (userRole == Client.ROLE_ADMIN) binding.adminPhoneInput.text.toString().trim() else binding.userPhoneInput.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill name and phone", Toast.LENGTH_SHORT).show()
            return
        }

        when (userRole) {
            Client.ROLE_USER -> {
                val city = binding.userCityInput.text.toString()
                val address = binding.userAddressInput.text.toString()
                viewModel.updateProfile(uid, name, phone, city, address)
            }
            Client.ROLE_DELIVERY -> {
                val vehicle = binding.vehicleTypeInput.text.toString()
                val registration = binding.registrationInput.text.toString().trim()
                val ageStr = binding.ageInput.text.toString().trim()
                val age = if (ageStr.isEmpty()) 0 else ageStr.toInt()

                val updates = HashMap<String, Any>()
                updates["clients/$uid/username"] = name
                updates["clients/$uid/phone"] = phone
                updates["delivery-users/$uid/username"] = name
                updates["delivery-users/$uid/phone"] = phone
                updates["delivery-users/$uid/transportType"] = vehicle
                updates["delivery-users/$uid/vehicleRegistration"] = registration
                updates["delivery-users/$uid/age"] = age

                dbRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profil enregistré avec succès", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Erreur: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            Client.ROLE_ADMIN -> {
                val updates = HashMap<String, Any>()
                updates["clients/$uid/username"] = name
                updates["clients/$uid/phone"] = phone

                dbRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Admin info saved", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDriverStatusInDb(uid: String, available: Boolean) {
        dbRef.child("delivery-users").child(uid).child("available").setValue(available)
    }

    private fun observeUser(uid: String) {
        lifecycleScope.launchWhenStarted {
            viewModel.client.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val client = resource.data
                    client?.let {
                        userRole = it.role
                        binding.userEmail.text = it.email
                        updateUIForRole(it.role)
                        setToolbarTitle(requireActivity() as HomeActivity)
                        
                        if (!isInitialDataLoaded) {
                            if (it.role == Client.ROLE_ADMIN) {
                                binding.adminNameInput.setText(it.username)
                                binding.adminPhoneInput.setText(it.phone)
                                binding.adminIdText.text = "Admin ID: $uid"
                                isInitialDataLoaded = true
                            } else {
                                binding.userNameInput.setText(it.username)
                                binding.userPhoneInput.setText(it.phone)
                                
                                if (it.role == Client.ROLE_USER) {
                                    binding.userCityInput.setText(it.address?.city)
                                    binding.userAddressInput.setText(it.address?.address)
                                    isInitialDataLoaded = true
                                } else if (it.role == Client.ROLE_DELIVERY) {
                                    fetchExtraDriverInfo(uid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchExtraDriverInfo(uid: String) {
        dbRef.child("delivery-users").child(uid).get().addOnSuccessListener { snapshot ->
            val driver = snapshot.getValue(DeliveryUser::class.java)
            if (driver != null) {
                binding.vehicleTypeInput.setText(driver.transportType, false)
                binding.registrationInput.setText(driver.vehicleRegistration)
                binding.ageInput.setText(driver.age.toString())
                binding.driverEarningsText.text = "Total Earnings: ${String.format("%.2f", driver.earnings)} MAD"
                binding.driverStatusSwitch.isChecked = driver.available
            }
            isInitialDataLoaded = true
        }.addOnFailureListener {
            isInitialDataLoaded = true
        }
    }

    private fun updateUIForRole(role: String) {
        binding.userRoleLabel.text = "ROLE: ${role.uppercase()}"
        
        when (role) {
            Client.ROLE_ADMIN -> {
                binding.editCard.hide()
                binding.viewOrdersBtn.hide()
                binding.driverSpecificLayout.hide()
                binding.userSpecificLayout.hide()
                
                binding.adminCard.show()
                binding.saveBtn.show()
                binding.userRoleLabel.backgroundTintList = requireContext().getColorStateList(R.color.black)
            }
            Client.ROLE_DELIVERY -> {
                binding.adminCard.hide()
                binding.viewOrdersBtn.hide()
                binding.userSpecificLayout.hide()
                
                binding.editCard.show()
                binding.saveBtn.show()
                binding.driverSpecificLayout.show()
                binding.userRoleLabel.backgroundTintList = requireContext().getColorStateList(R.color.foody_green)
            }
            else -> {
                binding.adminCard.hide()
                binding.driverSpecificLayout.hide()
                
                binding.editCard.show()
                binding.viewOrdersBtn.show()
                binding.saveBtn.show()
                binding.userSpecificLayout.show()
                binding.userRoleLabel.backgroundTintList = requireContext().getColorStateList(R.color.foody_orange)
            }
        }
    }

    private fun observeMaps() {
        mapsViewModel.address.observe(viewLifecycleOwner) { newAddress ->
            if (newAddress != null && userRole == Client.ROLE_USER) {
                binding.userAddressInput.setText(newAddress.address)
                binding.userCityInput.setText(newAddress.city)
            }
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        val title = when(userRole) {
            Client.ROLE_ADMIN -> "Profil Admin"
            Client.ROLE_DELIVERY -> "Profil Livreur"
            else -> "Mon Profil"
        }
        activity.setToolbarTitleString(title)
    }
}
