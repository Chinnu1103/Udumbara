package com.udumbara.udumbara.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

class VerseFeed() : Parcelable {
    var Text: String? = null
    var Reference: String? = null
    var Tags = mutableListOf<String>()
    var Timestamp: Timestamp? = null
    var Source: String? = null
    var Mood: String? = null

    constructor(parcel: Parcel) : this() {
        Text = parcel.readString()?.replace("_n", "\n")
        Reference = parcel.readString()
        parcel.readStringList(Tags)
        Timestamp = parcel.readParcelable(com.google.firebase.Timestamp::class.java.classLoader)
        Source = parcel.readString()
        Mood = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Text)
        parcel.writeString(Reference)
        parcel.readStringList(Tags)
        parcel.writeParcelable(Timestamp, flags)
        parcel.writeString(Source)
        parcel.writeString(Mood)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VerseFeed> {
        override fun createFromParcel(parcel: Parcel): VerseFeed {
            return VerseFeed(parcel)
        }

        override fun newArray(size: Int): Array<VerseFeed?> {
            return arrayOfNulls(size)
        }
    }
}