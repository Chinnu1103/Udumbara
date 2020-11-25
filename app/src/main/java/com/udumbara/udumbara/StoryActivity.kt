package com.udumbara.udumbara

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
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
import com.google.firebase.storage.ktx.storage
import com.udumbara.udumbara.bottomNav.feed.FeedFragment
import kotlinx.android.synthetic.main.activity_story.view.*

private const val TAG = "StoryActivity"

class StoryActivity : AppCompatActivity() {
    private val storage = Firebase.storage
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Story")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "StoryActivity")
        }

        val toolbar: Toolbar = findViewById(R.id.tb_story)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val args: StoryActivityArgs by navArgs()
        findViewById<TextView>(R.id.tv_story_title).text = args.story.Title
        findViewById<TextView>(R.id.tv_story_content).text = args.story.Description?.replace("_n", "\n")
        val ivImage = findViewById<ImageView>(R.id.iv_story_image)
        if (args.story.Picture != null){
            Glide.with(this)
                .load(storage.getReferenceFromUrl(args.story.Picture!!))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .fitCenter()
                .placeholder(R.drawable.ic_photo_default)
                .into(ivImage)
        }else{
            ivImage.setImageDrawable(null)
        }
        val flTags = findViewById<FlexboxLayout>(R.id.fl_tags)
        for (tag in args.story.Tags){
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