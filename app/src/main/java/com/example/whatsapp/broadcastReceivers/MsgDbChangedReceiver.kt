package com.example.whatsapp.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "MsgDbChangedReceiver"
open class MsgDbChangedReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: ")
    }
    companion object{
        const val ACTION_DATABASE_CHANGED = "com.example.whatsapp.DATABASE_CHANGED"
        const val DATABASE_RECEIVER_PERMISSION = "veryStrongPasswordThisIs"
    }
}