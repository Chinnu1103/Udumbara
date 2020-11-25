package com.udumbara.udumbara.bottomNav

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.udumbara.udumbara.R
import com.udumbara.udumbara.bottomNav.feed.FeedFragment
import com.udumbara.udumbara.bottomNav.read.ReadFragment
import com.udumbara.udumbara.bottomNav.read.ReadFragment.Companion.chapter
import com.udumbara.udumbara.bottomNav.read.ReadFragment.Companion.verse
import com.udumbara.udumbara.utils.DBHelper
import kotlinx.android.synthetic.main.read_options.view.*

class ViewPagerAdapter constructor(page: Int, val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TAG = "ViewPagerAdapter"
    private val mPage = page
    private var mTag: String? = null
    lateinit var btgOne: MaterialButtonToggleGroup
    lateinit var btgTwo: MaterialButtonToggleGroup

    var tempChapter: Int = chapter
    var tempVerse: Int = verse

    var flChapter: FlexboxLayout? = null
    var flVerse: FlexboxLayout? = null

    class FeedFilterViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tgFilter: MaterialButtonToggleGroup = view.findViewById(R.id.btg_filter)
    }

    class ReadFilterViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val flRead: FlexboxLayout = view.fl_read
    }

    public fun getSelectedIdOne(): Int {
        return btgOne.checkedButtonId
    }

    public fun getSelectedIdTwo(): Int {
        return btgTwo.checkedButtonId
    }

    public fun getSelectedTag(): String {
        return mTag?:"All"
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else if(position == 1) 1 else 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (mPage == 0){
            val layout = when (viewType) {
                0 -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.feed_type_options, parent, false)
                else -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.feed_mood_options, parent, false)
//                else -> LayoutInflater.from(parent.context)
//                    .inflate(R.layout.feed_tag_options, parent, false)
            }
            FeedFilterViewHolder(layout)
        }else{
            ReadFilterViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.read_options, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mPage == 0){
            val feedHolder = holder as FeedFilterViewHolder
            when (position) {
                0 -> {
                    btgOne = feedHolder.tgFilter
                    feedHolder.tgFilter.check(when(FeedFragment.typeFilter){
                        "Story" -> R.id.bt_story_type
                        "Verse Explained" -> R.id.bt_verse_explained_type
                        "Verse" -> R.id.bt_verse_type
                        else -> View.NO_ID
                    })
                }
                else -> {
                    btgTwo = feedHolder.tgFilter
                    feedHolder.tgFilter.check(when(FeedFragment.moodFilter){
                        "Peace" -> R.id.bt_peace_mood
                        "Sad" -> R.id.bt_sad_mood
                        "Motivation" -> R.id.bt_motivation_mood
                        "Fear" -> R.id.bt_fear_mood
                        "Stress" -> R.id.bt_stress_mood
                        "Joy" -> R.id.bt_joy_mood
                        "Anger" -> R.id.bt_anger_mood
                        "Wisdom" -> R.id.bt_wisdom_mood
                        else -> View.NO_ID
                    })
                }
//                else -> {
//                    for (tag in tagList) {
//                        val bt:MaterialButton = LayoutInflater.from(feedHolder.tgFilter.context)
//                            .inflate(R.layout.toggle_button_view, feedHolder.tgFilter, false) as MaterialButton
//                        bt.id = ViewCompat.generateViewId()
//                        bt.text = tag
//                        bt.addOnCheckedChangeListener { button, isChecked ->
//                            if (isChecked) mTag = button.text.toString()
//                        }
//                        feedHolder.tgFilter.addView(bt)
//                        if (tag == tagFilter) feedHolder.tgFilter.check(bt.id)
//                    }
//                }
            }
        }else{
            val readHolder = holder as ReadFilterViewHolder
            if (position == 0){
                flChapter = readHolder.flRead
                readHolder.flRead.removeAllViews()
                for (i in 1..13){
                    val view = LayoutInflater.from(context)
                        .inflate(R.layout.read_tv_options, flChapter, false) as TextView
                    view.text = (i).toString()
                    if (i == tempChapter) view.isSelected = true
                    readHolder.flRead.addView(view)
                    view.setOnClickListener {
                        ReadFragment.vpReadFilter.setCurrentItem(1, true)
                        flChapter?.getChildAt(tempChapter-1)?.isSelected = false
                        it.isSelected = true
                        tempChapter = (it as TextView).text.toString().toInt()
                        flVerse?.removeAllViews()
                        tempVerse = 1
                        populateVerses()
                    }
                }
            }
            else {
                flVerse = readHolder.flRead
                readHolder.flRead.removeAllViews()
                populateVerses()
            }
        }
    }

    private fun populateVerses(){
        for (i in 1..DBHelper(context).getVerseCount("BG", tempChapter)){
            val view = LayoutInflater.from(context)
                .inflate(R.layout.read_tv_options, flVerse, false) as TextView
            view.text = (i).toString()
            if (i == tempVerse) view.isSelected = true
            flVerse?.addView(view)
            view.setOnClickListener {
                flVerse?.getChildAt(tempVerse-1)?.isSelected = false
                tempVerse = (it as TextView).text.toString().toInt()
                it.isSelected = true
            }
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}