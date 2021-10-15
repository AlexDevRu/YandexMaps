package com.example.yandexmaps.ui.fragments.adapters.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.yandexmaps.ui.fragments.place.PlaceInfoFragment
import com.example.yandexmaps.ui.fragments.place.PlacePhotosFragment

class PlaceTabsAdapter(fragmentManager: FragmentManager, lifeCycle: Lifecycle):
    FragmentStateAdapter(fragmentManager, lifeCycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> PlaceInfoFragment()
            else -> PlacePhotosFragment()
        }
    }
}