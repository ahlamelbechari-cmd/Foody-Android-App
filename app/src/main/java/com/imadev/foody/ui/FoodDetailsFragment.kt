package com.imadev.foody.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentFoodDetailsBinding
import com.imadev.foody.model.Meal
import com.imadev.foody.ui.checkout.CheckoutViewModel
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.ui.home.HomeViewModel
import com.imadev.foody.utils.Constants.MEAL_ARG
import com.imadev.foody.utils.loadFromUrl
import com.imadev.foody.utils.setIcon
import com.imadev.foody.utils.showErrorToast
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "FoodDetailsFragment"
@AndroidEntryPoint
class FoodDetailsFragment : BaseFragment<FragmentFoodDetailsBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by viewModels()
    private val cartViewModel: CheckoutViewModel by activityViewModels()

    private var mSelected = false
    private var mMealID: String = ""
    private var meal: Meal? = null

    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            meal = bundle.getParcelable(MEAL_ARG)
            mMealID = meal?.id.toString()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFoodDetailsBinding = FragmentFoodDetailsBinding.inflate(inflater, container, false)

    @SuppressLint("NewApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            meal?.let {
                foodImg.loadFromUrl(requireContext(), it.image)
                foodTitle.text = it.name
                price.text = requireContext().getString(R.string.price, it.price.toString())
                description.text = it.ingredient.joinToString(separator = "\n")
            }
        }

        binding.addToCartButton.setOnClickListener {
            if (cartViewModel.cartList.contains(meal)) {
                Snackbar.make(binding.root, "Item already exists in cart", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Snackbar.make(binding.root, "Item added to cart", Snackbar.LENGTH_SHORT).setIcon(
                getDrawable(requireContext(), R.drawable.ic_success_24)!!,
                ContextCompat.getColor(requireContext(), R.color.foody_green)
            ).show()
            meal?.let { it.quantity = 1; cartViewModel.addToCart(it) }
        }

        (requireActivity() as HomeActivity).getFavoriteToolbarIcon().setOnClickListener {
            mSelected = !mSelected
            val drawable = if (mSelected) {
                addToFavorites()
                R.drawable.ic_heart_selected
            } else {
                removeFromFavorites()
                R.drawable.ic_heart
            }
            (activity as HomeActivity).getFavoriteToolbarIcon().setImageResource(drawable)
        }
        
        checkIfFavorite()
    }

    private fun checkIfFavorite() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        realtimeDb.child("favorites").child(uid).child(mMealID).get().addOnSuccessListener {
            mSelected = it.exists()
            val drawable = if (mSelected) R.drawable.ic_heart_selected else R.drawable.ic_heart
            (activity as HomeActivity).getFavoriteToolbarIcon().setImageResource(drawable)
        }
    }

    private fun removeFromFavorites() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        realtimeDb.child("favorites").child(uid).child(mMealID).removeValue().addOnFailureListener {
            showErrorToast()
        }
    }

    private fun addToFavorites() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        meal?.let { m ->
            m.favorite = true
            m.uid = uid
            realtimeDb.child("favorites").child(uid).child(mMealID).setValue(m).addOnFailureListener {
                showErrorToast()
            }
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitle(R.string.food_details)
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(requireActivity() as HomeActivity)
    }
}