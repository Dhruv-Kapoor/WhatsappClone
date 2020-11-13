package com.example.whatsapp

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.models.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.list_item.view.*

class UserViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    fun bind(user: User, onClick:(name: String, thumbUrl:String, uid:String)->Unit){
        with(view){
            tvCount.isVisible = false
            tvTime.isVisible = false

            tvTitle.text = user.name
            tvSubtitle.text = user.status

            if(user.thumbImg.isNotEmpty()) {
                Picasso.get().load(user.thumbImg)
                    .placeholder(R.drawable.defaultavatar)
                    .error(R.drawable.defaultavatar)
                    .into(ivUserImg)
            }

            setOnClickListener {
                onClick.invoke(user.name, user.thumbImg, user.uid)
            }
        }
    }
}