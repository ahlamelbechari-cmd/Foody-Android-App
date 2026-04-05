package com.imadev.foody.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.database.IgnoreExtraProperties

enum class PaymentMethod {
    CASH, CARD
}

enum class OrderStatus {
    PENDING, ACCEPTED, PREPARING, IN_DELIVERY, DELIVERED, CANCELLED
}

@IgnoreExtraProperties
data class Order(
    @DocumentId
    var id: String = "",
    var orderNumber: String = "",
    var date: Long = -1L,
    var meals: List<Meal> = listOf(),
    var client: Client = Client(),
    var paymentMethod: PaymentMethod = PaymentMethod.CARD,
    var status: OrderStatus = OrderStatus.PENDING,
    var to: String? = null, 
    var uid: String = "", 
    var deliveryLat: Double = 0.0,
    var deliveryLon: Double = 0.0,
    var accepted: Boolean = false,
    var totalPrice: Double = 0.0
)