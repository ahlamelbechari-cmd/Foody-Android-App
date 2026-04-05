package com.imadev.foody.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.imadev.foody.R
import com.imadev.foody.adapter.AdminUserAdapter
import com.imadev.foody.databinding.FragmentManageUsersBinding
import com.imadev.foody.model.Client
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminUsersFragment : BaseFragment<FragmentManageUsersBinding, AdminViewModel>() {

    override val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminUserAdapter

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentManageUsersBinding = FragmentManageUsersBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUsers()
    }

    private fun setupRecyclerView() {
        adapter = AdminUserAdapter(
            emptyList(),
            onDetailsClick = { user ->
                showUserDetails(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmation(user)
            }
        )
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminUsersFragment.adapter
        }
    }

    private fun observeUsers() {
        viewModel.allUsers.collectFlow(viewLifecycleOwner) { users ->
            // Filtrage strict : Uniquement les utilisateurs simples
            val onlyUsers = users.filter { it.role == Client.ROLE_USER }
            adapter.updateList(onlyUsers)
        }
    }

    private fun showUserDetails(user: Client) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profil Utilisateur")
            .setMessage("""
                Nom : ${user.username}
                Email : ${user.email}
                Téléphone : ${user.phone ?: "Non renseigné"}
                Rôle : ${user.role.uppercase()}
            """.trimIndent())
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun showDeleteConfirmation(user: Client) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer l'utilisateur")
            .setMessage("Voulez-vous vraiment supprimer définitivement ${user.username} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                user.id?.let { viewModel.deleteUser(it) }
                Toast.makeText(requireContext(), "Utilisateur supprimé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Gestion Utilisateurs")
    }
}
