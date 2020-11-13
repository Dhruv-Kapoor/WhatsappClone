package com.example.whatsapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.EmptyViewHolder
import com.example.whatsapp.R
import com.example.whatsapp.models.ChatEvent
import com.example.whatsapp.models.DateHeader
import com.example.whatsapp.models.Message
import com.example.whatsapp.utils.formatAsTime
import kotlinx.android.synthetic.main.list_item_chat_recv_message.view.*
import kotlinx.android.synthetic.main.list_item_chat_recv_message.view.tvMessage
import kotlinx.android.synthetic.main.list_item_chat_recv_message.view.tvTime
import kotlinx.android.synthetic.main.list_item_chat_sent_message.view.*
import kotlinx.android.synthetic.main.list_item_date_header.view.*

private const val TAG = "MessagesAdapter"

class MessagesAdapter(val list: List<ChatEvent>, val mUid: String, val onFifthMsgReachedCallback: OnFifthMsgReachedCallback?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = {layout:Int ->
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
        }
        return when(viewType){
            TEXT_MESSAGE_RECEIVED->MessageViewHolder(inflate(R.layout.list_item_chat_recv_message))
            TEXT_MESSAGE_SENT->MessageViewHolder(inflate(R.layout.list_item_chat_sent_message))
            DATE_HEADER->DateViewHolder(inflate(R.layout.list_item_date_header))
            else->EmptyViewHolder(inflate(R.layout.empty_list_item))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: $position")
        if(position == 4 && onFifthMsgReachedCallback!=null){
            onFifthMsgReachedCallback.onFifthMsgReached()
        }
        when(holder){
            is MessageViewHolder ->{
                holder.bind(list[position] as Message)
            }
            is DateViewHolder -> {
                holder.bind(list[position] as DateHeader)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        return when (val event = list[position]) {
            is Message -> {
                if(event.senderUid == mUid){
                    TEXT_MESSAGE_SENT
                }else{
                    TEXT_MESSAGE_RECEIVED
                }
            }
            is DateHeader -> {
                DATE_HEADER
            }
            else->{
                UNSUPPORTED
            }
        }
    }



    companion object {
        private const val UNSUPPORTED = -1
        private const val TEXT_MESSAGE_RECEIVED = 0
        private const val TEXT_MESSAGE_SENT = 1
        private const val DATE_HEADER = 2
    }
}

class MessageViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
    fun bind(message: Message){
        with(itemView){
            tvMessage.text = message.message
            tvTime.text = message.sentAt.formatAsTime()
            when(message.status){
                  Message.MSG_PENDING-> tvStatus.text = "P"
                  Message.MSG_SENT-> tvStatus.text = "S"
                  Message.MSG_DELIVERED-> tvStatus.text = "D"
                  Message.MSG_READ-> tvStatus.text = "R"
            }
        }
    }
}

class DateViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
    fun bind(dateHeader: DateHeader){
        with(itemView){
            tvDate.text = dateHeader.date
        }
    }
}

interface OnFifthMsgReachedCallback{
    fun onFifthMsgReached()
}