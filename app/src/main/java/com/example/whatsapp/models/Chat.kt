package com.example.whatsapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Chat(
    val name: String,
    val imageUrl: String,
    val thumbImg: String,
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    val phoneNumber:String,
    var lastMessage:String,
    var unreadCount:Int,
    var time: Long
){
    constructor(): this("","","","","","",0, System.currentTimeMillis())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chat

        if (name != other.name) return false
        if (imageUrl != other.imageUrl) return false
        if (thumbImg != other.thumbImg) return false
        if (uid != other.uid) return false
        if (phoneNumber != other.phoneNumber) return false
        if (lastMessage != other.lastMessage) return false
        if (unreadCount != other.unreadCount) return false

        return true
    }

    override fun toString(): String {
        return "Chat(name='$name', imageUrl='$imageUrl', thumbImg='$thumbImg', uid='$uid', phoneNumber='$phoneNumber', lastMessage='$lastMessage', unreadCount=$unreadCount, time=$time)"
    }


}