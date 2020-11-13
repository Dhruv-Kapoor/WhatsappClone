package com.example.whatsapp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.adapter.MessagesAdapter
import com.example.whatsapp.adapter.OnFifthMsgReachedCallback
import com.example.whatsapp.broadcastReceivers.MsgDbChangedReceiver
import com.example.whatsapp.database.ChatsDatabase
import com.example.whatsapp.database.MessageDatabase
import com.example.whatsapp.models.*
import com.example.whatsapp.utils.isSameDayAs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

const val NAME = "name"
const val THUMB_IMG_URL = "thumbImgUrl"
const val UID = "uid"

private const val TAG = "ChatActivity"

class ChatActivity : AppCompatActivity(),OnFifthMsgReachedCallback {

    private val friendName by lazy {
        intent.getStringExtra(NAME)
    }
    private val friendImg by lazy {
        intent.getStringExtra(THUMB_IMG_URL)
    }
    private val friendUid by lazy {
        intent.getStringExtra(UID)
    }
    private val mUid by lazy {
        FirebaseAuth.getInstance().uid
    }
    private val realtimeDb by lazy {
        FirebaseDatabase.getInstance()
    }
    private val realtimeMsgDbRef by lazy {
        mUid?.let {
            realtimeDb.reference.child("messages").child(friendUid).child(it)
        }
    }
    private val realtimeDbRef by lazy {
        realtimeDb.reference
    }
    private val chatsDB by lazy {
        ChatsDatabase.getDatabase(this).chatsDao()
    }
    private val messageList = LinkedList<ChatEvent>()
    private val adapter by lazy {
        MessagesAdapter(messageList, mUid!!, this)
    }
    private val messageDatabase by lazy {
        MessageDatabase(this)
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val deliveryDbRef by lazy {
        realtimeDbRef.child("delivery").child(mUid!!).child(friendUid)
    }
    private val readReceiptsDbRef by lazy {
        realtimeDbRef.child("readReceipts").child(mUid!!).child(friendUid)
    }
    private val sendReadReceiptsRef by lazy {
        realtimeDbRef.child("readReceipts").child(friendUid).child(mUid!!)
    }
    private val sharedPreferencesEditor by lazy{
        PreferenceManager.getDefaultSharedPreferences(this).edit()
    }

    private var deliveryReportListener: ChildEventListener? = null
    private var readReceiptListener: ChildEventListener? = null
    private var DATA_REFERESH_NEEDED = false
    private val msgDbChangedReceiver by lazy {
        object : MsgDbChangedReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "onReceive: ")
                super.onReceive(context, intent)
                val sender = intent?.getStringExtra("sender")
                if (sender == friendUid) {
                    val message = intent?.getParcelableExtra<Message>("message")!!
                    addMessageToList(message)
                    Log.d(TAG, "onReceive: message: $message")
                    clearUnreadMessages()
                    sendReadReceiptsRef.push().setValue(message.uid)
                }
            }
        }
    }

    private fun addMessageToList(message: Message) {
        val lastEvent = messageList.lastOrNull()
        if (lastEvent == null || !lastEvent.sentAt.isSameDayAs(message.sentAt)) {
            messageList.addLast(DateHeader(message.sentAt, this))
        }
        messageList.addLast(message)
        adapter.notifyItemInserted(messageList.size)
        rvMessages.scrollToPosition(messageList.size-1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmojiManager.install(GoogleEmojiProvider())
        setContentView(R.layout.activity_chat)

        Log.d(TAG, "onCreate: ")

        //Setting Friends name and image
        tvName.text = friendName
        if (!friendImg.isNullOrEmpty()) {
            Picasso.get().load(friendImg).placeholder(R.drawable.defaultavatar)
                .error(R.drawable.defaultavatar).into(ivUser)
        }

        if (EMULATORS_ENABLED) {
            realtimeDb.useEmulator(getString(R.string.local_host), 6500)
            realtimeDb.setPersistenceEnabled(false)
        }
        rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = this@ChatActivity.adapter
        }

        clearUnreadMessages()

        val list = messageDatabase.getNextSetOfMessages(friendUid)
        for (i in list.size - 1 downTo 0) {
            addMessageToList(list[i])
        }

        btnSend.setOnClickListener {
            Log.d(TAG, "onCreate: onclick")
            val msg = etMsg.text.toString()
            etMsg.setText("")
            val key = realtimeMsgDbRef?.push()?.key
            val message = Message(key!!, msg, mUid!!, "TEXT", Message.MSG_PENDING, Date())
            addMessageToList(message)
            messageDatabase.addMessage(message, friendUid!!)
            realtimeMsgDbRef?.child(key)?.setValue(message)?.addOnSuccessListener {
                message.status = Message.MSG_SENT
                adapter.notifyItemChanged(messageList.indexOf(message))
                GlobalScope.launch {
                    messageDatabase.updateMessageStatus(friendUid, key, Message.MSG_SENT)
                }
            }
            GlobalScope.launch(Dispatchers.IO) {
                updateChat(friendUid, message)
            }
        }

        sendReadReceipts()

        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(DATA_REFERESH_NEEDED && newState == RecyclerView.SCROLL_STATE_IDLE){
                    adapter.notifyDataSetChanged()
                    DATA_REFERESH_NEEDED = false
                }
            }
        })
    }

    private fun sendReadReceipts() {
        Log.d(TAG, "sendReadReceipts: ")
        GlobalScope.launch(Dispatchers.IO) {
            val list = messageDatabase.getUnreadMessagesIds(friendUid)
            for(id in list){
                sendReadReceiptsRef.push().setValue(id)
                messageDatabase.updateMessageStatus(friendUid, id, Message.MSG_RECEIVED_READ)
            }
        }
    }

    private suspend fun updateChat(friendUid: String, message: Message) {
        Log.d(TAG, "updateChat: ")
        val chatsList = chatsDB.getChat(friendUid)
        if (chatsList.isNullOrEmpty()) {
            firestore.collection("users").document(friendUid).get().addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                val chat = Chat(
                    user.name,
                    user.imageUrl,
                    user.thumbImg,
                    user.uid,
                    user.phoneNumber,
                    "",
                    0,
                    0L
                )
                GlobalScope.launch(Dispatchers.IO) {
                    updateChatValues(chat, message)
                }
            }
        } else {
            updateChatValues(chatsList[0], message)
        }
    }

    private suspend fun updateChatValues(chat: Chat, message: Message) {
        chat.lastMessage = message.message
        chat.unreadCount = 0
        chat.time = message.sentAt.time
        Log.d(TAG, "fetchMessages: chat: $chat")
        chatsDB.addChat(chat)
    }

    private fun clearUnreadMessages() {
        Log.d(TAG, "clearUnreadMessages: ")
        GlobalScope.launch(Dispatchers.IO) {
            chatsDB.updateUnreadCount(friendUid, 0)
        }
    }

    private fun addMessageToListWithoutNotifying(message: Message) {
        val firstEvent = messageList.first
        if(firstEvent is DateHeader){
            if(!firstEvent.sentAt.isSameDayAs(message.sentAt)){
                messageList.addFirst(DateHeader(message.sentAt, this))
            }
            messageList.add(1, message)
        }

    }


    override fun onResume() {
        super.onResume()
        registerReceiver(
            msgDbChangedReceiver,
            IntentFilter(MsgDbChangedReceiver.ACTION_DATABASE_CHANGED)
        )

        sharedPreferencesEditor.putBoolean(IS_APP_ACTIVE, true).apply()

        //Delivery Report Listener
        deliveryReportListener = deliveryDbRef
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val msgUid = snapshot.getValue(String::class.java)
                    val key = snapshot.key
                    if (!msgUid.isNullOrEmpty()) {
                        GlobalScope.launch(Dispatchers.IO) {
                            for (i in messageList.size - 1 downTo 0) {
                                val event = messageList[i]
                                if (event is Message && event.uid == msgUid) {
                                    event.status = Message.MSG_DELIVERED
                                    runOnUiThread {
                                        adapter.notifyItemChanged(i)
                                    }
                                    break
                                }
                            }
                            messageDatabase.updateMessageStatus(
                                friendUid,
                                msgUid,
                                Message.MSG_DELIVERED
                            )
                        }
                    }
                    key?.let { deliveryDbRef.child(key).removeValue() }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        //Read Receipts Listener
        readReceiptListener = readReceiptsDbRef
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d(TAG, "onChildAdded: readreceipts")
                    val key = snapshot.key
                    val msgUid = snapshot.getValue(String::class.java)
                    if (!msgUid.isNullOrEmpty()) {
                        GlobalScope.launch(Dispatchers.IO) {
                            for (i in messageList.size - 1 downTo 0) {
                                val event = messageList[i]
                                if (event is Message && event.uid == msgUid) {
                                    event.status = Message.MSG_READ
                                    runOnUiThread {
                                        adapter.notifyItemChanged(i)
                                    }
                                }
                            }
                            messageDatabase.updateMessageStatus(friendUid, msgUid, Message.MSG_READ)
                        }
                    }
                    key?.let { readReceiptsDbRef.child(it).removeValue() }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        Log.d(TAG, "onResume: ")
    }

    override fun onPause() {
        super.onPause()
        sharedPreferencesEditor.putBoolean(IS_APP_ACTIVE, true).apply()
        unregisterReceiver(msgDbChangedReceiver)
        deliveryReportListener?.let { deliveryDbRef.removeEventListener(it) }
        readReceiptListener?.let { readReceiptsDbRef.removeEventListener(it)}
        deliveryReportListener = null
        readReceiptListener = null
        Log.d(TAG, "onPause: ")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun onFifthMsgReached() {
        val list = messageDatabase.getNextSetOfMessages(friendUid)
        list.forEach {
            addMessageToListWithoutNotifying(it)
        }
        Log.d(TAG, "onFifthMsgReached: ${list.size}")
        if(list.isNotEmpty())
            DATA_REFERESH_NEEDED = true
    }
}
