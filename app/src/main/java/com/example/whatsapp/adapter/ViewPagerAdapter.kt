package com.example.whatsapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.whatsapp.fragments.ChatsFragment
import com.example.whatsapp.fragments.PeopleFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when(position){
        0->ChatsFragment()
        else->PeopleFragment()
    }

}