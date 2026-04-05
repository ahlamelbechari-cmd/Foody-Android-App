package com.imadev.foody.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import com.imadev.foody.utils.formatDecimal
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Meal(
    var id: String? = null,
    var name: String = "",
    var price: Double = 0.0,
    var ingredient: List<String> = listOf(),
    var image: String? = null,
    var favorite: Boolean = false,
    var categoryId: String? = null,
    var quantity: Int = 0,
    var uid: String = "",
    var date: Long = -1L
) : Parcelable {

    @IgnoredOnParcel
    private var formattedPrice: String = formatDecimal(price)
}
