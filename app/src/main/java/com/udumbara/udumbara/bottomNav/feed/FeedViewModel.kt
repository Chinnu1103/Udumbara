package com.udumbara.udumbara.bottomNav.feed

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager

class FeedViewModel(private val state: SavedStateHandle) : ViewModel() {

    fun getRVSave(): Parcelable?{
        return state.get<Parcelable?>("Save")
    }

    fun setRVSave(save: Parcelable?){
        state.set("Save", save)
    }
}