package com.udumbara.udumbara.bottomNav.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView

class HomeViewModel : ViewModel() {

    var rvFeed = MutableLiveData<RecyclerView>()
    fun getRvFeed(): LiveData<RecyclerView> {
        return rvFeed
    }
    fun loadRvFeed(rv: RecyclerView) {
        rvFeed.value = rv
    }
}