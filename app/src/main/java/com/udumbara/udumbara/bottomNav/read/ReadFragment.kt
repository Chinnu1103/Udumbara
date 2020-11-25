package com.udumbara.udumbara.bottomNav.read

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.navigation.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.udumbara.udumbara.R
import com.udumbara.udumbara.StoryActivityArgs
import com.udumbara.udumbara.bottomNav.ViewPagerAdapter
import com.udumbara.udumbara.utils.DBHelper
import com.udumbara.udumbara.utils.Verse
import kotlinx.android.synthetic.main.fragment_read.view.*

class ReadFragment : Fragment() {

    lateinit var readViewModel: ReadViewModel
    private lateinit var btChapter: Button
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var tvRead: TextView
    private lateinit var btgRead: MaterialButtonToggleGroup
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object{
        var book: String = "BG"
        var chapter: Int = 1
        var verse: Int = 1
        var currentVerse: Verse? = null
        lateinit var vpReadFilter: ViewPager2
    }

    override fun onStop() {
        super.onStop()
        sharedPrefs = (activity as AppCompatActivity)
            .getSharedPreferences("CurrentVerse", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("Book", book)
        editor.putInt("Chapter", chapter)
        editor.putInt("Verse", verse)
        editor.apply()
        editor.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedPrefs = (activity as AppCompatActivity)
            .getSharedPreferences("CurrentVerse", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("Book", book)
        editor.putInt("Chapter", chapter)
        editor.putInt("Verse", verse)
        editor.apply()
        editor.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        readViewModel = ViewModelProvider(this).get(ReadViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_read, container, false)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Read")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ReadFragment")
        }

        btChapter = root.bt_chapter
        tvRead = root.tv_read
        btgRead = root.btg_read

        val args: ReadFragmentArgs by navArgs()
        sharedPrefs = (activity as AppCompatActivity)
            .getSharedPreferences("CurrentVerse", Context.MODE_PRIVATE)
        if(args.chapter != -1){
            book = args.book
            chapter = args.chapter
            verse = args.verse
            currentVerse = DBHelper(context as Context)
                .getVerse("$book $chapter:$verse")
            adjustSelection(true)
        }else if (currentVerse == null){
            book = sharedPrefs.getString("Book", "BG")!!
            chapter = sharedPrefs.getInt("Chapter", 1)
            verse = sharedPrefs.getInt("Verse", 1)
            currentVerse = DBHelper(context as Context)
                .getVerse("$book $chapter:$verse")
            adjustSelection()
        }else adjustSelection()

        btgRead.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked){
                when(checkedId){
                    R.id.bt_script_toggle -> {
                        tvRead.text = currentVerse?.text
                        tvRead.gravity = Gravity.CENTER_HORIZONTAL
                    }
                    else -> {
                        tvRead.text = currentVerse?.meaning
                        tvRead.gravity = Gravity.START
                    }
                }
            }
        }

        btChapter = root.bt_chapter
        btChapter.setOnClickListener {
            val view = LayoutInflater.from(context).inflate(R.layout.filter_view, null)

            vpReadFilter = view.findViewById<ViewPager2>(R.id.vp_filters)
            vpReadFilter.offscreenPageLimit = 1
            val vpAdapter = ViewPagerAdapter(1, context as Context)
            vpReadFilter.adapter = vpAdapter
            val tlReadFilter = view.findViewById<TabLayout>(R.id.tl_feed_filters)

            TabLayoutMediator(tlReadFilter, vpReadFilter) { tab, position ->
                tab.text = if (position == 0) "Chapter" else "Verse"
            }.attach()

            val dialog = MaterialAlertDialogBuilder(context as Context)
                .setView(view)
                .create()

            val btOk = view.findViewById<Button>(R.id.bt_ok)
            btOk.setOnClickListener {
                chapter = vpAdapter.tempChapter
                verse = vpAdapter.tempVerse
                currentVerse = DBHelper(context as Context)
                    .getVerse("$book $chapter:$verse")
                adjustSelection()
                dialog.cancel()
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT){
                    param(FirebaseAnalytics.Param.ITEM_ID, "$book, $chapter:$verse")
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Read Filter Selected")
                }
            }

            val btCancel = view.findViewById<Button>(R.id.bt_cancel)
            btCancel.setOnClickListener {
                dialog.cancel()
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val params = WindowManager.LayoutParams();
            params.copyFrom(dialog.window?.attributes)
            params.width = (displayMetrics.widthPixels * 0.9f).toInt()
            params.height = (displayMetrics.heightPixels * 0.9f).toInt()
            dialog.window?.attributes = params
        }

        root.iv_prev.setOnClickListener {
            if (verse == 1){
                chapter -= 1
                if (chapter == 0) chapter = 13
                verse = DBHelper(context as Context).getVerseCount(book, chapter)
            }else verse -= 1
            currentVerse = DBHelper(context as Context)
                .getVerse("$book $chapter:$verse")
            adjustSelection()
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
                param(FirebaseAnalytics.Param.ITEM_ID, "$book, $chapter:$verse")
                param(FirebaseAnalytics.Param.ITEM_NAME, "Left")
            }
        }

        root.iv_next.setOnClickListener {
            val maxVerse = DBHelper(context as Context).getVerseCount(book, chapter)
            if (verse == maxVerse){
                verse = 1
                chapter += 1
                if (chapter == 14) chapter = 1
            }else verse += 1
            currentVerse = DBHelper(context as Context)
                .getVerse("$book $chapter:$verse")
            adjustSelection()
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
                param(FirebaseAnalytics.Param.ITEM_ID, "$book, $chapter:$verse")
                param(FirebaseAnalytics.Param.ITEM_NAME, "Right")
            }
        }

        root.bt_share.setOnClickListener {
            val text = "${currentVerse?.getFormattedReference()}\n\n" +
                    "Text:\n" +
                    "${currentVerse?.text}\n\n" +
                    "Meaning:\n" +
                    "${currentVerse?.meaning}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, null))
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
                param(FirebaseAnalytics.Param.ITEM_ID, "$book, $chapter:$verse")
                param(FirebaseAnalytics.Param.ITEM_NAME, "Share")
            }
        }

        return root
    }

    private fun adjustSelection(tags: Boolean = false){
        val text = "Chapter $chapter ; Verse $verse"
        btChapter.text = text
        when(btgRead.checkedButtonId){
            R.id.bt_script_toggle -> {
                tvRead.text = currentVerse?.text
                tvRead.gravity = Gravity.CENTER_HORIZONTAL
            }
            else -> {
                tvRead.text = currentVerse?.meaning
                tvRead.gravity = Gravity.START
            }
        }
    }


}