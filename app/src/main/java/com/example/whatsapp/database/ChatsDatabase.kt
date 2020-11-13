package com.example.whatsapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.whatsapp.models.Chat

const val CHATS_DB_NAME = "ChatsDatabase"

@Database(entities = [Chat::class], version = 1)
abstract class ChatsDatabase : RoomDatabase() {
    abstract fun chatsDao(): ChatsDao

    companion object{
        private var INSTANCE: ChatsDatabase? = null

        fun getDatabase(context: Context): ChatsDatabase{
            val tempInstance = INSTANCE

            if(tempInstance!=null){
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(context.applicationContext, ChatsDatabase::class.java, CHATS_DB_NAME).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}