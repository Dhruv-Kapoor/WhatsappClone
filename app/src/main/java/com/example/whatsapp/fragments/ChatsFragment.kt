package com.example.whatsapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.adapter.ChatsAdapter
import com.example.whatsapp.database.ChatsDatabase
import com.example.whatsapp.models.Chat
import kotlinx.android.synthetic.main.fragment_chats.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "ChatsFragment"

class ChatsFragment : Fragment() {

    private val adapter by lazy {
        ChatsAdapter(requireContext())
    }
    private val chatsDB by lazy {
        ChatsDatabase.getDatabase(requireContext()).chatsDao()
    }
    private lateinit var chatsLiveData: LiveData<PagedList<Chat>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatsFragment.adapter
        }

        chatsLiveData = chatsDB.getAllChats().toLiveData(
            20,
            boundaryCallback = object : PagedList.BoundaryCallback<Chat>() {
                override fun onItemAtEndLoaded(itemAtEnd: Chat) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    Log.d(TAG, "onItemAtEndLoaded: ")
                }
            })

        chatsLiveData.observe(this, Observer {
            adapter.submitList(it)
        })


        //TEMP
//        GlobalScope.launch(Dispatchers.IO) {
//            delay(5000)
//            chatsDB.addChat(Chat("name", "", "", "cewcec", "", "lastmessage", 5, System.currentTimeMillis()))
//            delay(1000)
//            chatsDB.addChat(Chat("name", "", "", "ewfw", "", "lastmessage", 5, System.currentTimeMillis()))
//            delay(1000)
//            chatsDB.addChat(Chat("name", "", "", "berbs", "", "lastmessage", 5,System.currentTimeMillis()))
//            delay(1000)
//            chatsDB.addChat(Chat("name", "", "", "berwvb", "", "lastmessage", 5,System.currentTimeMillis()))
//            delay(1000)
//            chatsDB.addChat(Chat("name", "", "", "asca", "", "lastmessage", 5, System.currentTimeMillis()))
//            delay(1000)
//
//            for(i in 1..10){
//                delay(1000)
//                chatsDB.updateUnreadCount("cewcec", i)
//            }


    }
}