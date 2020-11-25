package com.udumbara.udumbara

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.udumbara.udumbara.bottomNav.feed.FeedFragment
import com.udumbara.udumbara.utils.DailyQuote
import java.text.SimpleDateFormat
import java.util.*

class SignInActivity : AppCompatActivity() {
    private val RC_SIGN_IN: Int = 1103
    private val TAG = "SignInActivity"
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var  firebaseAuth: FirebaseAuth
    companion object{
        var dailyQuote: DailyQuote? = null
        val db = Firebase.firestore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            if (FirebaseAuth.getInstance().currentUser != null){
                loadQuote()
            }else{
//                setContentView(R.layout.activity_sign_in)

//                mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                    .requestIdToken(getString(R.string.default_web_client_id))
//                    .requestEmail()
//                    .requestProfile()
//                    .build()
//                mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)

                firebaseAuth = FirebaseAuth.getInstance()
                guestSignIn()

//                val btGoogle = findViewById<Button>(R.id.bt_google)
//                btGoogle.setOnClickListener{
//                    googleSignIn()
//                }
//
//                val btGuest = findViewById<Button>(R.id.bt_guest)
//                btGuest.setOnClickListener{
//                    guestSignIn()
//                }
            }
        }, 1000)
    }

    private fun guestSignIn(){
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { loadQuote() }
            .addOnCanceledListener { loadQuote() }
            .addOnFailureListener { loadQuote() }
    }

    private fun googleSignIn(){
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Sign In Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                loadQuote()
            }else{
                Toast.makeText(this, "Sign In Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadQuote(){
        val date = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val dailyQuoteDoc = db.collection("Daily Quotes").whereEqualTo("Day", date).limit(1)
        val tempPreferences = getSharedPreferences("Daily_Quote", Context.MODE_PRIVATE)
        val source = if (tempPreferences.getBoolean(date.toString(), false)) Source.CACHE else Source.SERVER
        dailyQuoteDoc.get(source).addOnSuccessListener {
            dailyQuote = if (it.documents.size == 0) null
            else it.toObjects(DailyQuote::class.java)[0]
            if (dailyQuote != null) {
                val editor = tempPreferences.edit()
                editor.clear()
                editor.putBoolean(date.toString(), true)
                editor.apply()
                editor.commit()
            }
            startActivity(Intent(this, StartActivity::class.java))
//            loadTags()
            finish()
        }.addOnFailureListener {
            dailyQuote = DailyQuote()
            dailyQuote!!.Quote = null
            startActivity(Intent(this, StartActivity::class.java))
//            loadTags()
            finish()
        }
    }

    private fun loadTags(){
        db.collection("Tags").document("Tags").get(Source.DEFAULT).addOnSuccessListener {
            if(it != null){
                if (it["Tags"] != null) {
                    FeedFragment.tagList.addAll(it["Tags"] as List<String>)
                }
            }
        }
    }
}