package com.example.whatsapp

import android.app.NotificationManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.example.whatsapp.adapter.ViewPagerAdapter
import com.example.whatsapp.database.ChatsDatabase
import com.example.whatsapp.database.MessageDatabase
import com.example.whatsapp.models.Message
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
const val IS_APP_ACTIVE = "isAppActive"

class MainActivity : AppCompatActivity() {

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private val chatsDB by lazy {
        ChatsDatabase.getDatabase(this).chatsDao()
    }
    private val messagesDB by lazy {
        MessageDatabase(this)
    }
    private val realtimeDbRef by lazy {
        FirebaseDatabase.getInstance().reference
    }
    private val deliveryDbRef by lazy {
        realtimeDbRef.child("delivery").child(auth.uid!!)
    }
    private val readReceiptsDbRef by lazy {
        realtimeDbRef.child("readReceipts").child(auth.uid!!)
    }
    private val sharedPreferencesEditor by lazy{
        PreferenceManager.getDefaultSharedPreferences(this).edit()
    }
    private val notificationManager by lazy{
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private var deliveryReportListener: ChildEventListener? = null
    private var readReceiptListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager, TabLayoutMediator.TabConfigurationStrategy{ tab, position ->
            when(position){
                0-> tab.text = "Chats"
                1-> tab.text = "People"
            }
        }).attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuSearch->{

            }
            R.id.menuSettings->{
                GlobalScope.launch(Dispatchers.IO) {
                    chatsDB.deleteAllChats()
                    messagesDB.deleteMessagesFromAll()
                }
            }
            R.id.menuSignOut->{
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                ))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        sharedPreferencesEditor.putBoolean(IS_APP_ACTIVE, false).apply()

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

    override fun onResume() {
        super.onResume()

        sharedPreferencesEditor.putBoolean(IS_APP_ACTIVE, true).apply()
        
        deliveryReportListener = deliveryDbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildAdded: $snapshot")
                handleMessageStatus(snapshot, Message.MSG_DELIVERED)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: $snapshot")
                handleMessageStatus(snapshot, Message.MSG_DELIVERED)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
        
        readReceiptListener = readReceiptsDbRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleMessageStatus(snapshot, Message.MSG_READ)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleMessageStatus(snapshot, Message.MSG_READ)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        notificationManager.cancelAll()

        Log.d(TAG, "onResume: ")
    }

    private fun handleMessageStatus(snapshot: DataSnapshot, msgStatus: Int) {
        GlobalScope.launch(Dispatchers.IO){
            val friendUid = snapshot.key
            if(friendUid.isNullOrEmpty()){
                return@launch
            }
            for (ds in snapshot.children) {
                val key = ds.key
                val msgUid = ds.getValue(String::class.java)

                if(msgUid.isNullOrEmpty() || key.isNullOrEmpty() ){
                    continue
                }

                //Update Message Database //Remove From Firebase
                if(msgStatus == Message.MSG_DELIVERED) {
                    messagesDB.updateMessageStatus(friendUid, msgUid, Message.MSG_DELIVERED)
                    deliveryDbRef.child(friendUid).child(key).removeValue()
                }else{
                    messagesDB.updateMessageStatus(friendUid, msgUid, Message.MSG_READ)
                    readReceiptsDbRef.child(friendUid).child(key).removeValue()
                }
            }
        }

    }
}