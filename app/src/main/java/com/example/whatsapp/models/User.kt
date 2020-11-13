package com.example.whatsapp.models

import com.google.firebase.firestore.FieldValue

data class User(
    val name: String,
    val imageUrl: String,
    val thumbImg: String,
    val uid: String,
    val deviceToken: String,
    val phoneNumber:String,
    val status: String,
    val onlineStatus: Boolean
) {
    constructor() : this("", "", "", "", "","", "", false)
    constructor(name: String, imageUrl: String, thumbImg: String, uid: String, deviceToken: String, phoneNumber: String, status: String) : this(
        name,
        imageUrl,
        thumbImg,
        uid,
        deviceToken,
        phoneNumber,
        status,
        false
    )

}