package com.imadev.foody.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Client(
    var id: String? = null,
    var username: String? = null,
    var address: Address? = null,
    var phone: String? = null,
    var email: String? = null,
    var token: String? = null,
    var role: String = ROLE_USER
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
        const val ROLE_DELIVERY = "delivery"
    }

    override fun toString(): String {
        return "$username $address $phone $email $token"
    }
}
