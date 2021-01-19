package com.example.vcissuerandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.vcissuerandroid.drive.DriveFileList
import com.example.vcissuerandroid.drive.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.toolbar.*

class SettingsActivity : BaseActivity() ,NavigationView.OnNavigationItemSelectedListener{

    private val REQUEST_ACCESS_STORAGE: Int = 105
    var mDriveServiceHelper: DriveServiceHelper? = null
    lateinit var googleDriveService: Drive

    private var RC_AUTHORIZE_DRIVE = 101
    private val REQUEST_IMAGE_CAPTURE: Int = 100
    private val REQUEST_IMAGE_SELECT: Int = 104

    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        checkForGooglePermissions()
        //toolbar and drawer setup
        (R.id.toolbar_main)
        setSupportActionBar(toolbar_main)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar_main,
            R.string.open,
            R.string.close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setHomeButtonEnabled(true)

        val prefs: SharedPreferences = getSharedPreferences("PROFILE_DATA", MODE_PRIVATE)
        val name: String? = prefs.getString("name", "No name defined")
        val email: String? = prefs.getString("email", "no email")
        val url: String? = prefs.getString("url", "no url")

        val navigationView: NavigationView = findViewById<View>(R.id.nav_view) as NavigationView
        val headerView: View = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById(R.id.doctorName) as TextView
        val navUserEmail = headerView.findViewById(R.id.doctorEmail) as TextView
        val navUserImage = headerView.findViewById(R.id.doctorImage) as ImageView


        navUsername.text = name
        navUserEmail.text = email
        Glide.with(this).load(url).apply(RequestOptions.circleCropTransform()).into(navUserImage)

        getPermission()

        getStarted.setOnClickListener{
            buttonEffect(getStarted)
//            getStarted.setBackgroundColor(getColor(R.color.yellow))
//            getStarted.setBackgroundColor(getColor(R.color.orange))
            val intent = Intent(this,DidGenerateActivity::class.java)
            startActivity(intent)
        }

//        getFolderID()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun buttonEffect(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter( getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    v.background.clearColorFilter()
                    v.invalidate()
                }
            }
            false
        }
    }

    //get permission for the device to access the files
    private fun getPermission() {
        //if the system is marshmallow or above get the run time permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
        ) {
            //permission was not enabled
            val permission = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            //show popup to request permission
            requestPermissions(permission, REQUEST_ACCESS_STORAGE)

        }
    }


    private fun getFolderID(){
        val thread1 = Thread{
            listFilesInDrive()
        }
        thread1.start()
        thread1.join()

        val thread2 = Thread{
            val check = DriveFileList.isFileAvailable("vcanddiddocs")
            Log.i("getFolderId", "check v : $check")

            if (check){
                Log.i("getFolderId", "hello world how ar you")
//                val folderId = DriveFileList.getFolderId("vcanddiddocs")
//                val ids = getSharedPreferences("ID_DATA", MODE_PRIVATE).edit()
//                ids.putString("folderId", folderId)
//                ids.apply()
//                val prefs: SharedPreferences = getSharedPreferences("ID_DATA", MODE_PRIVATE)
//
//                Log.i("getFolderId", "value of folder id is : ${prefs.getString("folderId","null")}")

            }else{
                createFolderInDrive("vcanddiddocs")
                Log.i("getFolderId", "folder created")

            }
        }
        thread2.start()
//        thread2.join()
//        getFolderID()
    }

    //list the files in the drive
    private fun listFilesInDrive() {
        val thread = Thread(Runnable {
            try {
                if (mDriveServiceHelper == null) {
                    checkForGooglePermissions()
                }
                val thread1 = Thread {
                    val fileList123456 = mDriveServiceHelper?.listDriveImageFiles()
                    if (fileList123456 != null) {
                        for (i in fileList123456) {
                            DriveFileList.addFile(i)
                        }
                    }
                }
                thread1.start()
                thread1.join()
                for (i in DriveFileList.driveFileList()) {
                    Log.i("myFileList", i.toString())
                }
                val check = DriveFileList.isFileAvailable("vcanddiddocs")
                Log.i("myFileList", check.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        thread.start()
    }

    //check for google permissions
    private fun checkForGooglePermissions() {
        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(applicationContext),
                ACCESS_DRIVE_SCOPE,
                SCOPE_EMAIL,
                SCOPE_APP_DATA
            )
        ) {
            GoogleSignIn.requestPermissions(
                this,
                RC_AUTHORIZE_DRIVE,
                GoogleSignIn.getLastSignedInAccount(applicationContext),
                ACCESS_DRIVE_SCOPE,
                SCOPE_EMAIL,
                SCOPE_APP_DATA
            )
        } else {
            Toast.makeText(
                this,
                "Permission to access Drive and Email has been granted",
                Toast.LENGTH_SHORT
            ).show()
            driveSetUp()
        }
    }

    //setting up the drive
    private fun driveSetUp() {
        val mAccount =
            GoogleSignIn.getLastSignedInAccount(this)
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, setOf(Scopes.DRIVE_FILE)
        )
        credential.selectedAccount = mAccount!!.account
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("GoogleDriveIntegration 3")
            .build()
        mDriveServiceHelper = DriveServiceHelper(googleDriveService)
    }

    //create folder in the drive
    private fun createFolderInDrive(folderName: String){
        mDriveServiceHelper?.createFolder(folderName, null)
            ?.addOnSuccessListener(OnSuccessListener<Any> { googleDriveFileHolder ->
                Log.i(
                    "creatingFolder",
                    "Successfully Uploaded. File Id :$googleDriveFileHolder"
                )
            })
            ?.addOnFailureListener { e ->
                Log.i(
                    "creatingFolder",
                    "Failed to Upload. File Id :" + e.message
                )
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        Log.i("logout","id.toString()")

        when (id) {
            (R.id.logout) -> {
                Log.i("logout","logout pressed")
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(this, gso).signOut()
                    .addOnCompleteListener(this, OnCompleteListener<Void?> {
                        val intent = Intent(applicationContext, SignInActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show()
                    }).addOnFailureListener {
                        Toast.makeText(this, "Issue with Logout", Toast.LENGTH_SHORT).show()
                    }
                Toast.makeText(this, "I am clicked", Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            else -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }

    }

}