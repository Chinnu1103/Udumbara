package com.udumbara.udumbara.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlin.collections.ArrayList

class Story(): Parcelable{
    var Title: String? = null
    var Description: String? = null
    var Picture: String? = null
    var References = mutableListOf<String?>()
    var Source: String? = null
    var Tags = mutableListOf<String>()
    var Timestamp: Timestamp? = null
    var Mood: String? = null

    constructor(parcel: Parcel) : this() {
        Title = parcel.readString()
        Description = parcel.readString()
        Picture = parcel.readString()
        parcel.readStringList(References)
        Source = parcel.readString()
        parcel.readStringList(Tags)
        Timestamp = parcel.readParcelable(com.google.firebase.Timestamp::class.java.classLoader)
        Mood = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(Title)
        p0?.writeString(Description)
        p0?.writeString(Picture)
        p0?.writeStringList(References)
        p0?.writeString(Source)
        p0?.writeStringList(Tags)
        p0?.writeParcelable(Timestamp, p1)
        p0?.writeString(Mood)
    }

    companion object CREATOR : Parcelable.Creator<Story> {
        override fun createFromParcel(parcel: Parcel): Story {
            return Story(parcel)
        }

        override fun newArray(size: Int): Array<Story?> {
            return arrayOfNulls(size)
        }
    }
}