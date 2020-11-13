package com.example.whatsapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.*
import com.example.whatsapp.models.Chat
import com.example.whatsapp.utils.formatAsListItem
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.list_item.view.*
import java.util.*

class ChatsAdapter(val context: Context) :
    PagedListAdapter<Chat, ChatsAdapter.ChatViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem == newItem
            }

        }
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(chat: Chat, onClick:(name: String, thumbUrl:String, uid:String)->Unit) {
            with(itemView) {
                tvTitle.text = chat.name
                tvSubtitle.text = chat.lastMessage
                tvCount.isVisible = chat.unreadCount != 0
                tvCount.text = chat.unreadCount.toString()
                tvTime.text = Date(chat.time).formatAsListItem(context)
                if (chat.thumbImg.isNotEmpty()) {
                    Picasso.get().load(chat.thumbImg).placeholder(R.drawable.defaultavatar)
                        .error(R.drawable.defaultavatar).into(ivUserImg)
                }
                itemView.setOnClickListener {
                    onClick.invoke(
                        chat.name, chat.thumbImg, chat.uid
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item, parent, false
            )
        )

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it){name, thumbImg, uid->
            context.startActivity(Intent(context, ChatActivity::class.java).apply {
                putExtra(NAME, name)
                putExtra(UID, uid)
                putExtra(THUMB_IMG_URL, thumbImg)
            })
        } }
    }
}