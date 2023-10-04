package com.saurabhsandav.core.fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResult(

    @SerialName("name")
    var name: String,

    @SerialName("image")
    var image: String,

    @SerialName("display_name")
    var displayName: String,

    @SerialName("email_id")
    var emailId: String,

    @SerialName("PAN")
    var pan: String,

    @SerialName("fy_id")
    var fyId: String,

    @SerialName("pin_change_date")
    var pinChangeDate: String,

    @SerialName("mobile_number")
    var mobileNumber: String,

    @SerialName("totp")
    var totp: Boolean,

    @SerialName("pwd_change_date")
    var pwdChangeDate: String,

    @SerialName("pwd_to_expire")
    var pwdToExpire: Int,
)
