package com.udumbara.udumbara.utils

import java.lang.ref.Reference

class Verse(val reference: String?, verse: String, mng: String) {
    var book = reference?.split(" ")!![0]
    var chapterNo = reference?.split(" ")!![1].split(":")[0].toInt()
    var verseNo = reference?.split(" ")!![1].split(":")[1].toInt()
    var text = verse
    var meaning = mng

    private fun getBook(book: String?): String {
        return when(book){
            "BG" -> "Bhagavad Gita"
            else -> ""
        }
    }

    public fun getFormattedReference(): String{
        return "${getBook(book)} $chapterNo:$verseNo"
    }
}