package com.udumbara.udumbara.utils

import com.google.firebase.Timestamp

class Video {
    var Title: String? = null
    var Description: String? = null
    var File: String? = null
    var references: ArrayList<VerseFeed> = ArrayList()
    var Source: String? = null
    var Tags: ArrayList<String> = ArrayList()
    var Time: Timestamp? = null
}