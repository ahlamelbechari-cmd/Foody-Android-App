package com.imadev.foody.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.imadev.foody.fcm.remote.NotificationService
import com.imadev.foody.fcm.remote.PushNotification
import com.imadev.foody.model.*
import com.imadev.foody.utils.Constants.CATEGORY_COLLECTION
import com.imadev.foody.utils.Constants.CLIENTS_COLLECTION
import com.imadev.foody.utils.Constants.MEALS_COLLECTION
import com.imadev.foody.utils.Constants.ORDERS_COLLECTION
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.safeFirebaseCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

private const val TAG = "FoodyRepoImp"

class FoodyRepoImp @Inject constructor(
    private val db: FirebaseFirestore,
    private val api: NotificationService
) : FoodyRepo {

    private val realtimeDb = FirebaseDatabase.getInstance("https://foody-app-a1b12-default-rtdb.firebaseio.com/").reference

    override suspend fun getMealsByCategory(categoryId: String) = safeFirebaseCall {
        val snapshot = realtimeDb.child(MEALS_COLLECTION).get().await()
        snapshot.children.mapNotNull {
            it.getValue(Meal::class.java)?.apply { if (id == null) id = it.key }
        }.filter { it.categoryId == categoryId }
    }

    override suspend fun getCategories() = safeFirebaseCall {
        val snapshot = realtimeDb.child(CATEGORY_COLLECTION).get().await()
        snapshot.children.mapNotNull { it.getValue(Category::class.java) }
    }

    override suspend fun getClient(uid: String) = safeFirebaseCall {
        realtimeDb.child(CLIENTS_COLLECTION).child(uid).get().await().getValue(Client::class.java)
    }

    override suspend fun updateField(
        collectionName: String,
        uid: String,
        field: String,
        value: Any
    ) = safeFirebaseCall {
        realtimeDb.child(collectionName).child(uid).child(field).setValue(value).await()
    }

    override suspend fun sendNotification(pushNotification: PushNotification): Response<ResponseBody> {
        return api.postNotification(pushNotification)
    }

    override suspend fun getAvailableDeliveryUsers(): Flow<Resource<List<DeliveryUser?>>> = safeFirebaseCall {
        val snapshot = realtimeDb.child("delivery-users").get().await()
        snapshot.children.mapNotNull { it.getValue(DeliveryUser::class.java) }
            .filter { it.available == true }
    }

    override suspend fun sendOrder(order: Order): Flow<Resource<Void?>> = safeFirebaseCall {
        // Source unique pour toutes les commandes
        val key = order.date.toString()
        realtimeDb.child(ORDERS_COLLECTION).child(key).setValue(order).await()
    }

    override suspend fun getMeals(): Flow<Resource<List<Meal?>>> = safeFirebaseCall {
        val snapshot = realtimeDb.child(MEALS_COLLECTION).get().await()
        snapshot.children.mapNotNull {
            it.getValue(Meal::class.java)?.apply { if (id == null) id = it.key }
        }
    }

    override fun observeAllOrders(): Flow<Resource<List<Order>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { doc ->
                    doc.getValue(Order::class.java)?.apply { id = doc.key ?: "" }
                }
                trySend(Resource.Success(orders))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.toException()))
            }
        }
        realtimeDb.child(ORDERS_COLLECTION).addValueEventListener(listener)
        awaitClose { realtimeDb.child(ORDERS_COLLECTION).removeEventListener(listener) }
    }
}