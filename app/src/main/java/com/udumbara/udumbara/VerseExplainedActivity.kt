package com.udumbara.udumbara

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.navigation.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class VerseExplainedActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verse_explained)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.SCREEN_NAME, "VerseFeed")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "VerseExplainedActivity")
        }

        val toolbar: Toolbar = findViewById(R.id.tb_verse)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val args: VerseExplainedActivityArgs by navArgs()
        findViewById<TextView>(R.id.tv_verse_text).text = args.verseText
        findViewById<TextView>(R.id.tv_verse_reference).text = args.verseReference
        findViewById<TextView>(R.id.tv_verse_explanation).text = args.explanation.replace("_n", "\n")
        val flTags = findViewById<FlexboxLayout>(R.id.fl_tags)
        for (tag in args.tags){
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.feed_chip_view, flTags, false) as TextView
            chip.text = tag
            flTags.addView(chip)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}