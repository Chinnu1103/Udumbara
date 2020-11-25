package com.udumbara.udumbara.bottomNav.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.udumbara.udumbara.R
import com.udumbara.udumbara.SignInActivity.Companion.dailyQuote
import com.udumbara.udumbara.SignInActivity.Companion.db
import com.udumbara.udumbara.utils.DailyQuote
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), View.OnClickListener {
    private val TAG = "HomeFragment"
    private val todaysDate = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
        .format(Date())

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var rvHome: RecyclerView
    private lateinit var rvHomeAdapter: RecyclerView.Adapter<*>
    private lateinit var rvHomeManager: RecyclerView.LayoutManager
    private lateinit var root: View

    lateinit var cvMood: CardView
    lateinit var cvMoodShare: CardView
    lateinit var btGoToFeed: Button

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object{
        private var moodSelected: String? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        root = inflater.inflate(R.layout.fragment_home, container, false)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Home")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeFragment")
        }

        if (dailyQuote != null && dailyQuote!!.Quote != null) {
            displayQuote()
        }else{
            displayQuoteError(pb = false, error = true)
        }

        root.bt_quote_retry.setOnClickListener {
            root.pb_quote.visibility = View.VISIBLE
            displayQuoteError(pb = true, error = true)
            val date = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val dailyQuoteDoc = db.collection("Daily Quotes").whereEqualTo("Day", date).limit(1)
            val tempPreferences = (activity as AppCompatActivity)
                .getSharedPreferences("Daily_Quote", Context.MODE_PRIVATE)
            dailyQuoteDoc.get(Source.SERVER).addOnSuccessListener {
                dailyQuote = if (it.documents.size == 0) null
                else it.toObjects(DailyQuote::class.java)[0]
                pb_quote.visibility = View.GONE
                if (dailyQuote != null) {
                    val editor = tempPreferences.edit()
                    editor.clear()
                    editor.putBoolean(date.toString(), true)
                    editor.apply()
                    editor.commit()
                    displayQuoteError(pb = false, error = false)
                    displayQuote()
                }else{
                    displayQuoteError(pb = false, error = true)
                }
            }.addOnFailureListener {
                pb_quote.visibility = View.GONE
                dailyQuote = DailyQuote()
                dailyQuote!!.Quote = null
                displayQuoteError(pb = false, error = true)
            }
        }

        if (moodSelected == null){
            root.iv_peace.setOnClickListener(this)
            root.iv_sad.setOnClickListener(this)
            root.iv_anger.setOnClickListener(this)
            root.iv_motivation.setOnClickListener(this)
            root.iv_wisdom.setOnClickListener(this)
            root.iv_fear.setOnClickListener(this)
            root.iv_joy.setOnClickListener(this)
            root.iv_stress.setOnClickListener(this)
            cvMood = root.cv_mood
            cvMoodShare = root.cv_mood_share
            btGoToFeed = root.bt_go_to_feed
            cvMood.visibility = View.VISIBLE
            btGoToFeed.setOnClickListener{
                val action = HomeFragmentDirections.actionMoodSelection(moodSelected!!)
                it.findNavController().navigate(action)
                cvMoodShare.visibility = View.GONE
            }
        }

        sharedPreferences = (activity as AppCompatActivity)
            .getSharedPreferences("Daily_Streak", Context.MODE_PRIVATE)
        var currStreak = sharedPreferences.getInt("currStreak", 1)
        var bestStreak = sharedPreferences.getInt("bestStreak", 1)
        val lastLogin = sharedPreferences.getString("lastLogin", null)
        val cal = Calendar.getInstance()
        if (lastLogin != null && lastLogin != todaysDate){
            cal.add(Calendar.DATE, -1)
            val yesterday = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(cal.time)
            Log.d(TAG, "onCreateView: " + yesterday + " " + lastLogin)
            if (yesterday == lastLogin){
                currStreak += 1
                if (bestStreak < currStreak){
                    bestStreak = currStreak
                    firebaseAnalytics.logEvent("bestStreakUpdated"){
                        param("bestStreak", bestStreak.toString())
                    }
                }
            }else{
                currStreak = 1
            }
            cal.add(Calendar.DATE, 1)
        }
        if (lastLogin == null || lastLogin != todaysDate){
            val edit = sharedPreferences.edit()
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) edit.clear()
            edit.putInt("currStreak", currStreak)
            edit.putInt("bestStreak", bestStreak)
            edit.putString("lastLogin", todaysDate)
            edit.putBoolean(SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(cal.time), true)
            edit.apply()
            edit.commit()
        }

        root.tv_curr_streak.text = "$currStreak day" + if (currStreak > 1) "s" else ""
        root.tv_best_streak.text = "$bestStreak day" + if (bestStreak > 1) "s" else ""

        checkDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))

        do{
            val day = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(cal.time)
            if (sharedPreferences.getBoolean(day, false))
                checkDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
            cal.add(Calendar.DATE, -1)
        }while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)

        return root
    }

    private fun checkDayOfWeek(day: Int){
        when(day) {
            Calendar.MONDAY -> root.iv_mon.setImageResource (R.drawable.ic_circle_filled)
            Calendar.TUESDAY -> root.iv_tue.setImageResource (R.drawable.ic_circle_filled)
            Calendar.WEDNESDAY -> root.iv_wed.setImageResource (R.drawable.ic_circle_filled)
            Calendar.THURSDAY -> root.iv_thu.setImageResource (R.drawable.ic_circle_filled)
            Calendar.FRIDAY -> root.iv_fri.setImageResource (R.drawable.ic_circle_filled)
            Calendar.SATURDAY -> root.iv_sat.setImageResource (R.drawable.ic_circle_filled)
            Calendar.SUNDAY -> root.iv_sun.setImageResource (R.drawable.ic_circle_filled)
        }
    }

    private fun displayQuote(){
        val quote = "\" " + dailyQuote?.Quote + " \""
        root.tv_quote.text = quote
        root.tv_quote_reference.text = dailyQuote?.Reference
    }

    private fun displayQuoteError(pb: Boolean, error: Boolean){
        if (dailyQuote == null){
            val noQuoteText = "We are not able to display today's quote due to a " +
                    "technical error. Please try again after sometime."
            root.tv_quote_fail.text = noQuoteText
        }else{
            val noInternetText = "Please connect to the internet to view today's quote."
            root.tv_quote_fail.text = noInternetText
        }
        root.tv_quote.visibility = if (!error) View.VISIBLE else View.GONE
        root.tv_quote_fail.visibility = if (!error) View.GONE else if(pb) View.GONE else View.VISIBLE
        root.bt_quote_retry.visibility = if (!error) View.GONE else if(pb) View.GONE else View.VISIBLE
        root.ll_quote_options.visibility = if (!error) View.VISIBLE else View.GONE
    }

    override fun onClick(p0: View?) {
        cvMood.visibility = View.GONE
        cvMoodShare.visibility = View.VISIBLE
        when(p0?.id){
            R.id.iv_anger -> moodSelected =  "Anger"
            R.id.iv_peace -> moodSelected =  "Peace"
            R.id.iv_motivation -> moodSelected =  "Motivation"
            R.id.iv_wisdom -> moodSelected =  "Wisdom"
            R.id.iv_sad -> moodSelected =  "Sad"
            R.id.iv_fear -> moodSelected =  "Fear"
            R.id.iv_joy -> moodSelected =  "Joy"
            R.id.iv_stress -> moodSelected =  "Stress"
        }
    }
}