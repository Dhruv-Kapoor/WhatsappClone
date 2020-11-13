package com.example.whatsapp.models

import android.content.Context
import android.os.Parcelable
import com.example.whatsapp.utils.formatAsHeader
import kotlinx.android.parcel.Parcelize
import java.util.*

interface ChatEvent {
    val sentAt: Date
}

@Parcelize
data class Message(
    val uid: String,
    val message: String,
    val senderUid: String,
    val type: String = "TEXT",
    var status:Int,
    override val sentAt: Date = Date()
) : ChatEvent, Parcelable{
    constructor(): this("","","","TEXT", 0,Date(0L))

    override fun toString(): String {
        return "Message(uid='$uid', message='$message', senderUid='$senderUid', type='$type', sentAt=$sentAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (uid != other.uid) return false

        return true
    }

    companion object{
        const val MSG_READ = 3
        const val MSG_DELIVERED = 2
        const val MSG_SENT = 1
        const val MSG_PENDING = 0
        const val MSG_RECEIVED_UNREAD = -1
        const val MSG_RECEIVED_READ = -2
    }


}

data class DateHeader(override val sentAt: Date, val context: Context) : ChatEvent {
    val date: String = sentAt.formatAsHeader(context)
}