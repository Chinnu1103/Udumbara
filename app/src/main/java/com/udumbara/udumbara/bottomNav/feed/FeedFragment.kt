package com.udumbara.udumbara.bottomNav.feed

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.udumbara.udumbara.R
import com.udumbara.udumbara.bottomNav.ViewPagerAdapter
import com.udumbara.udumbara.utils.Story
import com.udumbara.udumbara.utils.VerseFeed
import kotlinx.android.synthetic.main.fragment_feed.view.*

class FeedFragment : Fragment(){
    private val TAG = "FeedFragment"

    private lateinit var viewModel: FeedViewModel
    private lateinit var rvFeed: RecyclerView
    private var rvFeedAdapter: RecyclerView.Adapter<*>? = null
    private var rvFeedManager: LinearLayoutManager? = null
    private lateinit var btFilter: Button
    private lateinit var cpType: Chip
    private lateinit var cpMood: Chip
    private lateinit var cpTag: Chip
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var hsvFilter: HorizontalScrollView

    private val db = Firebase.firestore
    var dialog: AlertDialog? = null

    lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object{
        var typeFilter: String = "All"
        var moodFilter: String = "All"
        var tagFilter: String = "All"
        var lastFeedItem: Timestamp? = null
        var contentAvailable: Boolean = true
        var feedList = mutableListOf<Any>()
        var tagList = mutableListOf<String>()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val editor = (activity as AppCompatActivity)
            .getSharedPreferences("Scroll_Pos", Context.MODE_PRIVATE).edit()
        editor.putInt("scroll", rvFeedManager?.findFirstVisibleItemPosition()?:0)
        editor.apply()
        editor.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_feed, container, false)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Feed")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "FeedFragment")
        }
        rvFeedManager = LinearLayoutManager(context)
        rvFeedAdapter = FeedAdapter(this)
        rvFeed = root.rv_feed
        rvFeed.setItemViewCacheSize(10)

        hsvFilter = root.hsv_filters
        cpMood = root.chip_mood
        cpTag = root.chip_tag
        cpType = root.chip_type
        refreshLayout = root.srl_feed
        btFilter = root.bt_feed_filters

        val args: FeedFragmentArgs by navArgs()
        if(args.mood != "None") {
            setSelection(typeFilter, args.mood, tagFilter)
        }else{
            rvFeed.apply {
                layoutManager = rvFeedManager
                adapter = rvFeedAdapter
            }
            adjustFilterView()
            setSelection(typeFilter, moodFilter, tagFilter)
            val scrollPos = (activity as AppCompatActivity)
                .getSharedPreferences("Scroll_Pos", Context.MODE_PRIVATE)
                .getInt("scroll", 0)
            rvFeedManager?.scrollToPosition(scrollPos)
        }

        refreshLayout.setOnRefreshListener {
            fetchFeedItems(true)
        }

        rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!contentAvailable || feedList.size < 5) return
                val pos = rvFeedManager!!.findLastVisibleItemPosition()
                if (feedList.size - pos <= 1) {
                    (rvFeedAdapter as FeedAdapter).updateLoading(true)
                    fetchFeedItems(false)
                    contentAvailable = false
                    firebaseAnalytics.logEvent("PageScrollState"){
                        param("Page", (feedList.size%5 + 1).toString())
                        param("Filters", "Type: $typeFilter, Mood: $moodFilter, Tag: $tagFilter")
                    }
                }
            }
        })

        cpMood.setOnCloseIconClickListener {
            it.visibility = View.GONE
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
                param(FirebaseAnalytics.Param.CONTENT, moodFilter)
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Mood Filter Removed")
            }
            setSelection(typeFilter, "All", tagFilter)
        }

        cpTag.setOnCloseIconClickListener {
            it.visibility = View.GONE
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
                param(FirebaseAnalytics.Param.CONTENT, tagFilter)
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Tag Filter Removed")
            }
            setSelection(typeFilter, moodFilter, "All")
        }

        cpType.setOnCloseIconClickListener {
            it.visibility = View.GONE
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
                param(FirebaseAnalytics.Param.CONTENT, typeFilter)
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Type Filter Removed")
            }
            setSelection("All", moodFilter, tagFilter)
        }

        btFilter.setOnClickListener {
            val view = LayoutInflater.from(context).inflate(R.layout.filter_view, null)
            val vpFeedFilter = view.findViewById<ViewPager2>(R.id.vp_filters)
            vpFeedFilter.offscreenPageLimit = 3
            vpFeedFilter.adapter = ViewPagerAdapter(0, context as Context)
            val tlFeedFilter = view.findViewById<TabLayout>(R.id.tl_feed_filters)
            TabLayoutMediator(tlFeedFilter, vpFeedFilter) {tab, position ->
                tab.text = if (position == 0) "Type" else "Emotion"
            }.attach()
            dialog = MaterialAlertDialogBuilder(context as Context)
                .setView(view)
                .create()
            val btOk = view.findViewById<Button>(R.id.bt_ok)
            btOk.setOnClickListener {
                val type = when((vpFeedFilter.adapter as ViewPagerAdapter).getSelectedIdOne()){
                    R.id.bt_story_type -> "Story"
                    R.id.bt_verse_explained_type -> "Verse Explained"
                    R.id.bt_verse_type -> "Verse"
                    else -> "All"
                }
                val mood = when((vpFeedFilter.adapter as ViewPagerAdapter).getSelectedIdTwo()){
                    R.id.bt_peace_mood -> "Peace"
                    R.id.bt_sad_mood -> "Sad"
                    R.id.bt_motivation_mood -> "Motivation"
                    R.id.bt_fear_mood -> "Fear"
                    R.id.bt_stress_mood -> "Stress"
                    R.id.bt_joy_mood -> "Joy"
                    R.id.bt_wisdom_mood -> "Wisdom"
                    R.id.bt_anger_mood -> "Anger"
                    else -> "All"
                }
//                val tag = (vpFeedFilter.adapter as ViewPagerAdapter).getSelectedTag()
                val tag = "All"
//                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
//                    param(FirebaseAnalytics.Param.ITEM_ID, "Type: $typeFilter, Mood: $moodFilter, Tag: $tagFilter")
//                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Feed Filter Selected")
//                }
                setSelection(type, mood, tag)
                dialog!!.cancel()
            }
            val btCancel = view.findViewById<Button>(R.id.bt_cancel)
            btCancel.setOnClickListener {
                dialog!!.cancel()
            }
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog?.show()
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val params = WindowManager.LayoutParams();
            params.copyFrom(dialog?.window?.attributes)
            params.width = (displayMetrics.widthPixels * 0.9f).toInt()
            params.height = (displayMetrics.heightPixels * 0.9f).toInt()
            dialog?.window?.attributes = params
        }

        return root
    }

    private fun adjustFilterView() {
        if (typeFilter == "All") cpType.visibility = View.GONE
        else{
            cpType.text = typeFilter
            cpType.visibility = View.VISIBLE
        }
        if (moodFilter == "All") cpMood.visibility = View.GONE
        else{
            cpMood.text = moodFilter
            cpMood.visibility = View.VISIBLE
        }
        if (tagFilter == "All") cpTag.visibility = View.GONE
        else{
            cpTag.text = tagFilter
            cpTag.visibility = View.VISIBLE
        }
        val params = btFilter.layoutParams
        if (cpMood.visibility == View.GONE && cpTag.visibility == View.GONE && cpType.visibility == View.GONE){
            hsvFilter.visibility = View.GONE
            btFilter.text = "Choose Filter"
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        }else{
            hsvFilter.visibility = View.VISIBLE
            btFilter.text = "Edit"
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        btFilter.layoutParams = params
    }

    private fun fetchFeedItems(refresh: Boolean){
        if (refresh){
            contentAvailable = true
            refreshLayout.isRefreshing = true
            lastFeedItem = null
            feedList.clear()
        }
        val feed = db.collection("Feed")
        var query = feed.orderBy("Timestamp", Query.Direction.DESCENDING)
        if (typeFilter != "All"){
            query = query.whereEqualTo("Type", typeFilter)
        }
        if (moodFilter != "All"){
            query = query.whereEqualTo("Mood", moodFilter)
        }
        if (tagFilter != "All"){
            query = query.whereArrayContains("Tags", tagFilter)
        }
        if (lastFeedItem != null && !refresh){
            query = query.startAfter(lastFeedItem)
        }
        query.limit(5).get(Source.DEFAULT).addOnCompleteListener {
            if (it.isSuccessful){
                contentAvailable = it.result!!.size() >= 5
                if (!it.result!!.isEmpty){
                    for (doc in it.result!!.documents){
                        if (doc == null) continue
                        if (doc["Type"] == "Story") feedList.add(doc.toObject(Story::class.java)!!)
                        else if (doc["Type"] == "Verse Explained") feedList.add(doc.toObject(VerseFeed::class.java)!!)
                        else if (doc["Type"] == "Verse") feedList.add(doc.toObject(VerseFeed::class.java)!!)
                        else continue
                        lastFeedItem = doc["Timestamp"] as Timestamp
                    }
                }
                if (refresh){
                    rvFeed.apply {
                        layoutManager = rvFeedManager
                        adapter = rvFeedAdapter
                    }
                }
                rvFeedAdapter?.notifyDataSetChanged()
            }else{
                Toast.makeText(context, "Failed to load new content", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "fetchFeedItems: " + it.exception)
            }
            (rvFeedAdapter as FeedAdapter).updateLoading(false)
            refreshLayout.isRefreshing = false
        }
    }

    fun setSelection(type: String, mood: String, tag: String){
        if (type == typeFilter && mood == moodFilter && tag == tagFilter && feedList.isNotEmpty()) return
        typeFilter = type
        moodFilter = mood
        tagFilter = tag
        adjustFilterView()
        fetchFeedItems(true)
    }
}