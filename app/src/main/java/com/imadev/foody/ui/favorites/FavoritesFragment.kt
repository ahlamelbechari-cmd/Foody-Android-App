package com.imadev.foody.ui.favorites

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.imadev.foody.R
import com.imadev.foody.adapter.FavoriteMealsAdapter
import com.imadev.foody.model.Meal
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.utils.showErrorToast
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "FavoritesFragment"
@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    // Retour à l'ancienne URL
    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference

    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as HomeActivity).setToolbarTitle(R.string.favourites)

        recyclerView = view.findViewById(R.id.list)

        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        realtimeDb.child("favorites").child(uid).get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<Meal>()
            snapshot.children.forEach {
                it.getValue(Meal::class.java)?.let { meal ->
                    list.add(meal)
                }
            }
            recyclerView.adapter = FavoriteMealsAdapter(list)
            
        }.addOnFailureListener {
            Log.e(TAG, "Error loading favorites: ${it.message}")
            showErrorToast()
        }
    }
}
