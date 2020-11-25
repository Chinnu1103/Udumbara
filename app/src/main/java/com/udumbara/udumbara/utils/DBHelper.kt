package com.udumbara.udumbara.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.Reference

const val DB_NAME = "udumbara.sqlite"

class DBHelper(val context: Context): SQLiteOpenHelper(context, DB_NAME, null, 1) {

    private var db: SQLiteDatabase? = null

    private val bgMap: HashMap<Int, Int> = hashMapOf(
        1 to 46, 2 to 72, 3 to 43, 4 to 42, 5 to 29, 6 to 47, 7 to 30,
        8 to 28, 9 to 34, 10 to 42, 11 to 55, 12 to 20, 13 to 29
    )

    private fun initDB(){
        if (!File(context.getDatabasePath(DB_NAME).path).exists()) copyDatabase()
        db = SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    public fun getVerse(reference: String?): Verse{
        initDB()
        val cursor = db?.rawQuery(
            "SELECT Verse, Meaning " +
                "FROM udumbara " +
                "WHERE Reference = \"$reference\"" +
                "LIMIT 1", null)
        cursor?.moveToFirst()
        val verseString = cursor?.getString(0) ?: "No Verse Available"
        val meaningString = cursor?.getString(1) ?: "No Explanation Available"
        cursor?.close()
        close()
        return Verse(reference, verseString, meaningString)
    }

    public fun getVerseCount(book: String, chapter: Int): Int{
        if (book == "BG"){
            return bgMap[chapter]?:0
        }
        return 0
    }

    private fun copyDatabase(){
        val inputStream = context.assets.open("databases/$DB_NAME")
        val opFile = File(context.getDatabasePath(DB_NAME).path)
        val outputStream = FileOutputStream(opFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()
    }

    override fun close() {
        db?.close()
        super.close()
    }

    override fun onCreate(p0: SQLiteDatabase?) {

    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }
}