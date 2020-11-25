package com.udumbara.udumbara.utils

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class DailyQuote() : Parcelable {
    var Id: Int? = null
    var Quote: String? = null
    var Reference: String? = null
    var Source: String? = null

    constructor(parcel: Parcel) : this() {
        Id = parcel.readInt()
        Quote = parcel.readString()
        Reference = parcel.readString()
        Source = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(Id?:Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        parcel.writeString(Quote)
        parcel.writeString(Reference)
        parcel.writeString(Source)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DailyQuote> {
        override fun createFromParcel(parcel: Parcel): DailyQuote {
            return DailyQuote(parcel)
        }

        override fun newArray(size: Int): Array<DailyQuote?> {
            return arrayOfNulls(size)
        }
    }
}