package com.imadev.foody.repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.imadev.foody.model.Category
import com.imadev.foody.model.DeliveryUser
import com.imadev.foody.model.Meal
import com.imadev.foody.utils.Constants.CATEGORY_COLLECTION
import com.imadev.foody.utils.Constants.MEALS_COLLECTION
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "GenerateFoodViewModel"

class GenerateFoodViewModel : ViewModel() {

    private val db = Firebase.database("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference

    fun generateTestData() = viewModelScope.launch {
        try {
            db.child(CATEGORY_COLLECTION).removeValue().await()
            db.child(MEALS_COLLECTION).removeValue().await()
            db.child("delivery-users").removeValue().await()

            val deliveryUsers = listOf(
                DeliveryUser(id = "livreur_1", username = "Ahmed Delivery", available = true, token = "mock_token")
            )
            deliveryUsers.forEach { db.child("delivery-users").child(it.id!!).setValue(it) }

            val categories = mapOf(
                "Foods" to listOf(
                    Meal(
                        name = "Veggie tomato mix",
                        price = 35.0,
                        ingredient = listOf("Fresh Tomatoes", "Green Peas", "Olive Oil"),
                        image = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    ),
                    Meal(
                        name = "Spicy chicken mix",
                        price = 45.0,
                        ingredient = listOf("Grilled Chicken", "Hot Pepper Sauce"),
                        image = "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    )
                ),
                "Drinks" to listOf(
                    Meal(
                        name = "Coca Cola",
                        price = 10.0,
                        ingredient = listOf("33cl Can"),
                        image = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    ),
                    Meal(
                        name = "Orange Juice",
                        price = 15.0,
                        ingredient = listOf("100% Natural"),
                        image = "https://images.unsplash.com/photo-1613478223719-2ab802602423?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    )
                ),
                "Snacks" to listOf(
                    Meal(
                        name = "Potato Chips",
                        price = 12.0,
                        ingredient = listOf("Salted", "Crispy"),
                        image = "https://images.unsplash.com/photo-1566478431375-7043306503f1?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    ),
                    Meal(
                        name = "Chocolate Cookies",
                        price = 20.0,
                        ingredient = listOf("Dark Chocolate", "Handmade"),
                        image = "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    ),
                    Meal(
                        name = "Salted Popcorn",
                        price = 15.0,
                        ingredient = listOf("Freshly popped"),
                        image = "https://images.unsplash.com/photo-1578849278619-e73505e9610f?q=80&w=500&auto=format&fit=crop",
                        quantity = 1
                    )
                )
            )

            categories.forEach { (catName, meals) ->
                val catId = db.child(CATEGORY_COLLECTION).push().key ?: return@forEach
                db.child(CATEGORY_COLLECTION).child(catId).setValue(Category(id = catId, name = catName)).await()

                meals.forEach { meal ->
                    val mId = db.child(MEALS_COLLECTION).push().key ?: return@forEach
                    meal.id = mId
                    meal.categoryId = catId
                    db.child(MEALS_COLLECTION).child(mId).setValue(meal)
                }
            }
            Log.d(TAG, "Data regenerated with all Snacks and images!")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }
}