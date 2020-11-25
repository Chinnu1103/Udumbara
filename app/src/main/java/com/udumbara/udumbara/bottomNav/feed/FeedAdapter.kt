package com.udumbara.udumbara.bottomNav.feed

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.udumbara.udumbara.R
import com.udumbara.udumbara.bottomNav.feed.FeedFragment.Companion.feedList
import com.udumbara.udumbara.bottomNav.feed.FeedFragment.Companion.moodFilter
import com.udumbara.udumbara.bottomNav.feed.FeedFragment.Companion.typeFilter
import com.udumbara.udumbara.utils.*
import kotlinx.android.synthetic.main.rv_feed_pravachan_view.view.*
import kotlinx.android.synthetic.main.rv_feed_story_view.view.*
import kotlinx.android.synthetic.main.rv_feed_verse_view.view.*
import kotlinx.android.synthetic.main.rv_feed_video_view.view.*
import kotlinx.android.synthetic.main.rv_feed_video_view.view.tv_description
import kotlinx.android.synthetic.main.rv_feed_video_view.view.tv_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FeedAdapter(private val context: FeedFragment): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val storage = Firebase.storage
    private val TAG = "FeedAdapter"
    private var mProgressBar: ProgressBar? = null

    class LoadingViewHolder(progressBar: ProgressBar): RecyclerView.ViewHolder(progressBar){
        val pb = progressBar
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val ivTemplate: ImageView = view.iv_template
        val tvTitle: TextView = view.tv_title
        val tvDescription: TextView = view.tv_description
    }

    class PravachanViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvTitle: TextView = view.tv_pravachan_title
        val tvDescription: TextView = view.tv_pravachan_description
    }

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvTitle: TextView = view.tv_story_title
        val ivImage: ImageView = view.iv_story_image
        val tvContent: TextView = view.tv_story_content
        val cvMain: CardView = view.cv_story_feed
        val flTags: FlexboxLayout = view.fl_story_tags
    }

    class VerseViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvVerse: TextView = view.tv_verse
        val tvReference: TextView = view.tv_reference
        val tvExplanation: TextView = view.tv_explanation
        val flTags: FlexboxLayout = view.fl_verse_tags
        val cvMain: CardView = view.cv_verse_feed
        val tvVerseType: TextView = view.tv_verse_type
    }

    public fun updateLoading(isLoading: Boolean){
        if(isLoading) mProgressBar?.visibility = View.VISIBLE
        else mProgressBar?.visibility = View.GONE
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == feedList.size -> 3
            feedList[position] is Story -> 0
            feedList[position] is VerseFeed &&
                    (feedList[position] as VerseFeed).Text == null -> 1
            feedList[position] is VerseFeed &&
                    (feedList[position] as VerseFeed).Text != null-> 2
            else -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.rv_feed_story_view, parent, false)
                StoryViewHolder(view)
            }
            1,2 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.rv_feed_verse_view, parent, false)
                VerseViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.loading_view, parent, false) as ProgressBar
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            0 -> {
                val storyHolder = holder as StoryViewHolder
                val story = feedList[position] as Story
                storyHolder.tvTitle.text = story.Title
                if (story.Description != null){
                    if(story.Description!!.length > 500){
                        storyHolder.tvContent.text = story.Description!!.substring(0, 500)
                            .replace("_n", "\n")
                    }else{
                        storyHolder.tvContent.text = story.Description!!
                            .replace("_n", "\n")
                    }
                }
                storyHolder.flTags.removeAllViews()
                for (tag in story.Tags){
                    val chip = LayoutInflater.from(context.context as Context)
                        .inflate(R.layout.feed_chip_view, storyHolder.flTags, false) as TextView
                    chip.text = tag
//                    chip.setOnClickListener {
//                        context.setSelection(typeFilter, moodFilter, tag)
//                    }
                    storyHolder.flTags.addView(chip)
                }
                if (story.Picture != null){
                    Glide.with(context.context as Context)
                        .load(storage.getReferenceFromUrl(story.Picture!!))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.ic_photo_default)
                        .into(storyHolder.ivImage)
                }else{
                    storyHolder.ivImage.setImageDrawable(null)
                }
                storyHolder.cvMain.setOnClickListener {
                    context.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
                        param(FirebaseAnalytics.Param.ITEM_NAME, story.Title.toString())
                        param(FirebaseAnalytics.Param.ITEM_VARIANT, "Story")
                    }
                    val action = FeedFragmentDirections.actionDisplayStory(feedList[position] as Story)
                    it.findNavController().navigate(action)
                }
            }
            1, 2 -> {
                val verseHolder = holder as VerseViewHolder
                val verseFeed = feedList[position] as VerseFeed
                val verse = DBHelper(context.context as Context).getVerse(verseFeed.Reference)
                verseHolder.tvVerseType.text =
                    if (verseFeed.Text == null) "Verse" else "Verse Explained"
                verseHolder.tvReference.text = verse.getFormattedReference()
                verseHolder.tvVerse.text = verse.text
                verseHolder.tvExplanation.text = if (verseFeed.Text != null){
                    if (verseFeed.Text!!.length > 500){
                        verseFeed.Text!!.substring(0, 500)
                            .replace("_n", "\n")
                    }else{
                        verseFeed.Text!!.replace("_n", "\n")
                    }
                }else{
                     verse.meaning
                }

                verseHolder.flTags.removeAllViews()
                for (tag in verseFeed.Tags) {
                    val chip = LayoutInflater.from(context.context as Context)
                        .inflate(R.layout.feed_chip_view, verseHolder.flTags, false) as TextView
                    chip.text = tag
//                    chip.setOnClickListener {
//                        context.setSelection(typeFilter, moodFilter, tag)
//                    }
                    verseHolder.flTags.addView(chip)
                }
                verseHolder.cvMain.setOnClickListener {
                    context.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                        param(FirebaseAnalytics.Param.ITEM_NAME, verse.getFormattedReference())
                        param(
                            FirebaseAnalytics.Param.ITEM_VARIANT,
                            if (verseFeed.Text == null) "Verse" else "Verse Explained"
                        )
                    }
                    val action = if (verseFeed.Text != null) FeedFragmentDirections
                        .actionDisplayVerseExplained(
                            verseText = verse.text,
                            verseReference = verse.getFormattedReference(),
                            explanation = verseFeed.Text
                                ?: "No explanation could be obtained. Please try again",
                            tags = verseFeed.Tags.toTypedArray()
                        )
                    else FeedFragmentDirections.actionDisplayVerse(
                        book = verse.book,
                        chapter = verse.chapterNo,
                        verse = verse.verseNo
                    )
                    it.findNavController().navigate(action)
                }
            }
            else -> mProgressBar = (holder as LoadingViewHolder).pb
        }
    }

    override fun getItemCount(): Int {
        return feedList.size + 1
    }
}