package com.example.whatsapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.*
import com.example.whatsapp.models.User
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_people.*

private const val TAG = "PeopleFragment"
private const val NORMAL_VIEW_TYPE = 1
private const val DELETED_VIEW_TYPE = 2

class PeopleFragment : Fragment() {

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private lateinit var adapter: FirestorePagingAdapter<User, RecyclerView.ViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if(EMULATORS_ENABLED) {
            firestore.useEmulator(getString(R.string.local_host), 5500)
        }
        return inflater.inflate(R.layout.fragment_people, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        rvPeople.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PeopleFragment.adapter
        }
    }

    private fun setupAdapter() {
        Log.d(TAG, "setupAdapter: ")
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setPrefetchDistance(2)
            .build()

        val query = firestore.collection("users")
            .orderBy("name", Query.Direction.ASCENDING)
        val options = FirestorePagingOptions.Builder<User>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setQuery(query, config, User::class.java)
            .build()

        adapter = object: FirestorePagingAdapter<User, RecyclerView.ViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                when (viewType){
                    DELETED_VIEW_TYPE->{
                        EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.empty_list_item, parent, false))
                    }
                    else->{
                        UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
                    }
                }


            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, model: User) {
                when(getItemViewType(position)){
                    DELETED_VIEW_TYPE->{

                    }
                    else->{
                        (holder as UserViewHolder).bind(model){name, thumbImg, uid->
                            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                                putExtra(NAME, name)
                                putExtra(UID, uid)
                                putExtra(THUMB_IMG_URL, thumbImg)
                            })
                        }
                    }
                }
            }

            override fun getItemViewType(position: Int): Int {
                val user = getItem(position)?.toObject(User::class.java)
                user?.let {
                    return if(user.uid == auth.uid){
                        DELETED_VIEW_TYPE
                    }else{
                        NORMAL_VIEW_TYPE
                    }
                }
                return super.getItemViewType(position)
            }
        }
    }
}