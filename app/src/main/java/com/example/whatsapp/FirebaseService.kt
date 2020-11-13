package com.example.whatsapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.example.whatsapp.broadcastReceivers.MsgDbChangedReceiver
import com.example.whatsapp.database.ChatsDatabase
import com.example.whatsapp.database.MessageDatabase
import com.example.whatsapp.models.Chat
import com.example.whatsapp.models.Message
import com.example.whatsapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

private const val TAG = "FirebaseService"
private const val NOTIFICATION_CHANNEL_ID = "1234"
private const val NOTIFICATION_CHANNEL_NAME = "Messages"

class FirebaseService : FirebaseMessagingService() {

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private var NOTIFICATION_ID = 1
    private val chatsDB by lazy {
        ChatsDatabase.getDatabase(applicationContext).chatsDao()
    }
    private val mUid by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
    }
    private val realtimeDbRef by lazy {
        FirebaseDatabase.getInstance().reference.child("messages").child(mUid!!)
    }
    private val messageDatabase by lazy {
        MessageDatabase(applicationContext)
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val deliveryDbRef by lazy {
        FirebaseDatabase.getInstance().reference.child("delivery")
    }
    private val sharedPreferences by lazy{
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: ")
        super.onMessageReceived(remoteMessage)

        if(!sharedPreferences.getBoolean(IS_APP_ACTIVE, false)){
            val notification =
                NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
                    setContentTitle(remoteMessage.data["title"])
                    setContentText(remoteMessage.data["body"])
                    if (remoteMessage.data["icon"].isNullOrEmpty()) {
                        setSmallIcon(R.drawable.defaultavatar)
                    } else {
                        val url = URL(remoteMessage.data["icon"])
                        val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                        setLargeIcon(bitmap)
                    }
                }.build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        GlobalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "onMessageReceived: global scope")
            remoteMessage.data["senderUid"]?.let {
                Log.d(TAG, "onMessageReceived: sender: $it")
                fetchMessages(it)
            }
        }


    }

    private suspend fun fetchMessages(senderUid: String) {
        Log.d(TAG, "fetchMessages: ")
        val url = URL("https://whatsapp-c-lone.firebaseio.com/messages/$mUid/$senderUid.json")
        Log.d(TAG, "fetchMessages: URL: ${url.toString()}")
        try {
            val inputStream = url.openStream()
            val br = BufferedReader(InputStreamReader(inputStream))
            val jsonObject = JSONObject(br.readText())
            Log.d(TAG, "fetchMessages: json: $jsonObject")
            val messageKeys = jsonObject.keys()
            for (key in messageKeys) {
                val messageJson = jsonObject.getJSONObject(key)
                val message = Message(
                    messageJson.getString("uid"),
                    messageJson.getString("message"),
                    messageJson.getString("senderUid"),
                    messageJson.getString("type"),
                    Message.MSG_RECEIVED_UNREAD,
                    Date(
                        messageJson.getJSONObject("sentAt").getLong("time")
                    )
                )

                Log.d(TAG, "fetchMessages: message: $message")

                //Update last message and unread count in chat database
                updateChat(senderUid, message)

                //Add message in messages database
                messageDatabase.addMessage(message, senderUid)

                //Notify message database changed
                applicationContext.sendBroadcast(
                    Intent(MsgDbChangedReceiver.ACTION_DATABASE_CHANGED).apply {
                        putExtra("sender", senderUid)
                        putExtra("message", message)
                    }
                )

                //Remove message from firebase database
                realtimeDbRef.child(senderUid).child(key).removeValue()

                //Send Delivery Report
                deliveryDbRef.child(senderUid).child(mUid!!).push().setValue(message.uid)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateChat(senderUid: String, message: Message) {
        val chatsList = chatsDB.getChat(senderUid)
        if (chatsList.isNullOrEmpty()) {
            firestore.collection("users").document(senderUid).get().addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                val chat = Chat(user.name, user.imageUrl, user.thumbImg, user.uid, user.phoneNumber, "", 0,0L)
                GlobalScope.launch(Dispatchers.IO) {
                    updateChatValues(chat, message)
                }
            }
        } else {
            updateChatValues(chatsList[0], message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: ")
    }

    private suspend fun updateChatValues(chat: Chat, message: Message) {
        chat.lastMessage = message.message
        chat.unreadCount = chat.unreadCount + 1
        chat.time = message.sentAt.time
        Log.d(TAG, "fetchMessages: chat: $chat")
        chatsDB.addChat(chat)
    }

}