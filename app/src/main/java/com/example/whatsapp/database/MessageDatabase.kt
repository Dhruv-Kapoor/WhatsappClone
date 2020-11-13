package com.example.whatsapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.whatsapp.models.Message
import java.util.*
import kotlin.collections.ArrayList

private const val DB_NAME = "mWhatsapp"

private const val MSG_COL_UID = "uid"
private const val MSG_COL_MESSAGE = "message"
private const val MSG_COL_SENDER = "sender"
private const val MSG_COL_TYPE = "type"
private const val MSG_COL_SENTAT = "sentAt"
private const val MSG_COL_STATUS = "status"

private const val CHAT_IDS_TABLE_NAME = "ChatIds"
private const val CHAT_COL_ID = "uid"

private const val TAG = "MessageDatabase"

class MessageDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    private var currentSender = ""
    private var lastMsgTime = System.currentTimeMillis()
    private val readLimit = 50

    override fun onCreate(db: SQLiteDatabase?) {
        val query = """create table $CHAT_IDS_TABLE_NAME (
               $CHAT_COL_ID TEXT PRIMARY KEY
        )""".trimMargin()
        db?.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun addMessage(message: Message, uid:String) {
        Log.d(TAG, "addMessage: ")
        val tableName = "t$uid"
        ensureTableExists(tableName)
        val values = ContentValues().apply {
            put(MSG_COL_UID, message.uid)
            put(MSG_COL_MESSAGE, message.message)
            put(MSG_COL_SENDER, message.senderUid)
            put(MSG_COL_TYPE, message.type)
            put(MSG_COL_STATUS, message.status)
            put(MSG_COL_SENTAT, message.sentAt.time)
        }
        writableDatabase.insert(tableName, null, values)
    }


    fun getNextSetOfMessages(sender: String): List<Message> {
        Log.d(TAG, "getNextSetOfMessages: ")
        if (currentSender != sender) {
            currentSender = sender
            lastMsgTime = System.currentTimeMillis()
        }
        val tableName = "t$sender"
        ensureTableExists(tableName)
        val list = ArrayList<Message>()
        val query =
            "select * from $tableName where $MSG_COL_SENTAT < $lastMsgTime order by $MSG_COL_SENTAT desc limit $readLimit;"
        val cursor = readableDatabase.rawQuery(query, null)

        while (cursor.moveToNext()) {
            list.add(
                Message(
                    cursor.getString(cursor.getColumnIndex(MSG_COL_UID)),
                    cursor.getString(cursor.getColumnIndex(MSG_COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(MSG_COL_SENDER)),
                    cursor.getString(cursor.getColumnIndex(MSG_COL_TYPE)),
                    cursor.getInt(cursor.getColumnIndex(MSG_COL_STATUS)),
                    Date(
                        cursor.getString(cursor.getColumnIndex(MSG_COL_SENTAT)).toLong()
                    )
                )
            )
        }
        if(list.isNotEmpty())
            lastMsgTime = list.last().sentAt.time
        cursor.close()
        return list
    }

//    fun getMessageCount(sender: String): Int{
//        if(currentSender == sender && currentMessageCount!=-1){
//            return currentMessageCount
//        }
//        currentSender = sender
//        ensureTableExists(sender)
//        val query = "select * from $sender"
//        val cursor = readableDatabase.rawQuery(query,null)
//        currentMessageCount = cursor.count
//        cursor.close()
//        return currentMessageCount
//    }

    private fun ensureTableExists(tableName: String) {
        Log.d(TAG, "ensureTableExists: ")
        val query = """create table if not exists $tableName (
            $MSG_COL_UID TEXT PRIMARY KEY,
            $MSG_COL_MESSAGE TEXT,
            $MSG_COL_SENDER TEXT,
            $MSG_COL_TYPE TEXT,
            $MSG_COL_STATUS INTEGER,
            $MSG_COL_SENTAT INTEGER
            )""".trimMargin()
        writableDatabase.execSQL(query)

        val query2 = "select * from $CHAT_IDS_TABLE_NAME where $CHAT_COL_ID = '$tableName'"
        val cursor = readableDatabase.rawQuery(query2, null)
        if(cursor.count == 0){
            val values = ContentValues().apply {
                put(CHAT_COL_ID, tableName)
            }
            writableDatabase.insert(CHAT_IDS_TABLE_NAME, null, values)
        }
        cursor.close()
    }

    fun deleteMessagesOf(uid: String){
        Log.d(TAG, "deleteMessagesOf: ")
        val tableName = "t$uid"
        val query = "delete from $tableName"
        writableDatabase.execSQL(query)
    }

    fun deleteMessagesFromAll(){
        Log.d(TAG, "deleteMessagesFromAll: ")
        val query = "select * from $CHAT_IDS_TABLE_NAME"
        val cursor = readableDatabase.rawQuery(query, null)
        while(cursor.moveToNext()){
            val chatId = cursor.getString(cursor.getColumnIndex(CHAT_COL_ID))
            deleteMessagesOf(chatId)
        }
        cursor.close()
    }

    fun updateMessageStatus(userUid: String, msgUid: String, newStatus:Int){
        Log.d(TAG, "updateMessageStatus: ")
        val tableName = "t$userUid"
        val query = "update '$tableName' set $MSG_COL_STATUS = $newStatus where $MSG_COL_UID = '$msgUid'"
        writableDatabase.execSQL(query)
    }

    fun getUnreadMessagesIds(userUid: String): List<String>{
        Log.d(TAG, "getUnreadMessagesIds: ")
        val tableName = "t$userUid"
        val query = "select $MSG_COL_UID from '$tableName' where $MSG_COL_STATUS = ${Message.MSG_RECEIVED_UNREAD} order by $MSG_COL_SENTAT desc"
        val cursor = readableDatabase.rawQuery(query, null)
        val list = ArrayList<String>()
        while (cursor.moveToNext()){
            list.add(cursor.getString(cursor.getColumnIndex(MSG_COL_UID)))
        }
        cursor.close()
        return list
    }
}