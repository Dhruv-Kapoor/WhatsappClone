package com.example.whatsapp.database

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.whatsapp.models.Chat

@Dao
interface ChatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChat(chat: Chat)

    @Query("select * from Chat")
    fun getAllChats(): DataSource.Factory<Int, Chat>

    @Query("update Chat set unreadCount = :unreadCount where uid == :uid")
    fun updateUnreadCount(uid: String, unreadCount: Int)

    @Query("update Chat set lastMessage = :newMessage where uid == :uid")
    suspend fun updateLastMessage(uid: String, newMessage: String)

    @Query("update Chat set time = :time where uid == :uid")
    suspend fun updateTime(uid: String, time: Long)

    @Query("delete from Chat")
    suspend fun deleteAllChats()

    @Query("select * from Chat where uid == :uid")
    fun getChat(uid: String): List<Chat>

}