package com.imadev.foody.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.imadev.foody.R
import com.imadev.foody.adapter.AdminDriverAdapter
import com.imadev.foody.databinding.DialogAddDriverBinding
import com.imadev.foody.databinding.FragmentAdminDriversBinding
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDriversFragment : BaseFragment<FragmentAdminDriversBinding, AdminViewModel>() {

    override val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminDriverAdapter

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminDriversBinding = FragmentAdminDriversBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeDrivers()
        
        binding.btnAddDriver.setOnClickListener {
            showAddDriverDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminDriverAdapter(
            emptyList(),
            onToggleStatus = { driverId, isActive ->
                viewModel.toggleDriverStatus(driverId, isActive)
            },
            onDetailsClick = { driver ->
                showDriverDetails(driver)
            },
            onDeleteClick = { driver ->
                showDeleteConfirmation(driver)
            }
        )
        binding.driversRecyclerView.adapter = adapter
    }

    private fun observeDrivers() {
        viewModel.allDrivers.collectFlow(viewLifecycleOwner) { drivers ->
            adapter.updateList(drivers)
        }
    }

    private fun showAddDriverDialog() {
        val dialogBinding = DialogAddDriverBinding.inflate(layoutInflater)
        
        val vehicleTypes = arrayOf("Moto", "Velo", "Voiture")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        dialogBinding.vehicleTypeInput.setAdapter(adapter)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Driver")
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = dialogBinding.nameInput.text.toString().trim()
                val email = dialogBinding.emailInput.text.toString().trim()
                val phone = dialogBinding.phoneInput.text.toString().trim()
                val vehicle = dialogBinding.vehicleTypeInput.text.toString()
                val registration = dialogBinding.registrationInput.text.toString().trim()
                val ageStr = dialogBinding.ageInput.text.toString().trim()

                if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || registration.isEmpty() || ageStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val driver = DeliveryUser(
                    username = name,
                    email = email,
                    phone = phone,
                    transportType = vehicle,
                    vehicleRegistration = registration,
                    age = ageStr.toInt(),
                    available = false
                )

                viewModel.createDriver(driver)
                Toast.makeText(requireContext(), "Driver added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDriverDetails(driver: DeliveryUser) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Driver Details: ${driver.username}")
            .setMessage("""
                Email: ${driver.email}
                Phone: ${driver.phone}
                Vehicle: ${driver.transportType}
                Plate: ${driver.vehicleRegistration}
                Age: ${driver.age}
                Total Deliveries: ${driver.totalDeliveries}
                Total Earnings: ${String.format("%.2f MAD", driver.earnings)}
                Status: ${if (driver.available) "On Duty" else "Off Duty"}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteConfirmation(driver: DeliveryUser) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer le livreur")
            .setMessage("Voulez-vous vraiment supprimer ${driver.username} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                driver.id?.let { viewModel.deleteDriver(it) }
                Toast.makeText(requireContext(), "Livreur supprimé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Manage Drivers")
    }
}
