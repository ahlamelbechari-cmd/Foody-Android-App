package com.imadev.foody.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeliveryUser(
    @DocumentId var id: String? = "",
    val username: String? = "",
    val phone: String? = "",
    val email: String? = "",
    val profilePic: String? = null,
    val transportType: String? = "Moto", // Moto, Velo, Voiture
    val vehicleRegistration: String? = "", // Matricule
    val age: Int = 0,
    val token: String? = "",
    val available: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val earnings: Double = 0.0,
    val totalDeliveries: Int = 0
)