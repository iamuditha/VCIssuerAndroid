package com.example.vcissuerandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vcissuerandroid.utils.KeyHolder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_sign_in.*


class SignInActivity : AppCompatActivity() {

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private val RC_SIGN_IN = 100;
    private var RC_AUTHORIZE_DRIVE = 101


    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Configure sign-in to request the user's ID, email address, and basic profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sign_in_button.setOnClickListener { v ->
            when (v.id) {
                R.id.sign_in_button -> signIn()
            }
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let { getProfileData(it) }
        if (account != null) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this,"Can not go back at this stage",Toast.LENGTH_SHORT).show()
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let { getProfileData(it) }
            if (account != null) {
                getProfileData(account)
            }
            // Signed in successfully, show authenticated UI.
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } catch (e: ApiException) {
            Log.w("SignedIn", "signInResult:failed code=" + e.statusCode)
            //updateUI(null)
        }
    }

    private fun getProfileData(account: GoogleSignInAccount) {
        sharedPreference(account)
    }

    private fun sharedPreference(account: GoogleSignInAccount){
        val editor = getSharedPreferences("PROFILE_DATA", MODE_PRIVATE).edit()
        editor.putString("email", account.email)
        editor.putString("name", account.displayName)
        editor.putString("url", account.photoUrl.toString())
        editor.apply()
        KeyHolder.isRegistered = false
    }

//    private fun checkForGooglePermissions() {
//        if (!GoogleSignIn.hasPermissions(
//                GoogleSignIn.getLastSignedInAccount(applicationContext),
//                ACCESS_DRIVE_SCOPE,
//                SCOPE_EMAIL,
//                SCOPE_APP_DATA
//            )
//        ) {
//            GoogleSignIn.requestPermissions(
//                this,
//                RC_AUTHORIZE_DRIVE,
//                GoogleSignIn.getLastSignedInAccount(applicationContext),
//                ACCESS_DRIVE_SCOPE,
//                SCOPE_EMAIL,
//                SCOPE_APP_DATA
//            )
//        } else {
//            Toast.makeText(
//                this,
//                "Permission to access Drive and Email has been granted",
//                Toast.LENGTH_SHORT
//            ).show()
//            driveSetUp()
//        }
//    }



}
