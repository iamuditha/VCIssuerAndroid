package com.example.vcissuerandroid

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.Drive
import com.google.gson.Gson
import crypto.did.DID
import crypto.did.DIDDocument
import crypto.did.DIDDocumentGenerator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.nio.file.FileSystem
import com.google.api.client.json.gson.GsonFactory
import java.io.File


class MainActivity : BaseActivity() {

    private val REQUEST_IMAGE_CAPTURE: Int = 100
    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    private val  publicKey:String = ""
    private lateinit var pubKeyET:TextView
    private lateinit var nextBtn:Button
    private lateinit var uploadButton: Button

    @RequiresApi(Build.VERSION_CODES.Q)

    var gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nextBtn = findViewById(R.id.next)
        pubKeyET = findViewById(R.id.pKeyET)
        uploadButton = findViewById(R.id.uploadBtn)
        onNextButtonClicked(nextBtn)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            onUploadButtonClicked(uploadButton)
        }

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

        var prefs: SharedPreferences = getSharedPreferences("PROFILE_DATA", MODE_PRIVATE)
        var name: String? = prefs.getString("name", "No name defined")
        var email: String? = prefs.getString("email", "no email")
        var url: String? = prefs.getString("url", "no url")

        val navigationView: NavigationView = findViewById<View>(R.id.nav_view) as NavigationView
        val headerView: View = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById(R.id.doctorName) as TextView
        val navUseremail = headerView.findViewById(R.id.doctorEmail) as TextView
        val navUserImage = headerView.findViewById(R.id.doctorImage) as ImageView


        navUsername.text = name
        navUseremail.text = email
        Glide.with(this).load(url).apply(RequestOptions.circleCropTransform()).into(navUserImage)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RESULT_FIRST_USER && resultCode == RESULT_OK && null != data) {
            var selectedData: Uri? =data.data
//            var projection = (MediaStore.Da)
        }
    }
    private fun generateDidDoc(publicKey:String ): String {
        val did:String = DID.getInstance().generateDID()
        val didDoc:DIDDocument = DIDDocumentGenerator.getInstance().generateDIDDocument(did, publicKey);
        val gson = Gson()
        val didDocString:String = gson.toJson(didDoc)
        return didDocString

    }
    private fun onNextButtonClicked(nextBtn:Button) {
        nextBtn.setOnClickListener(View.OnClickListener {
//            val didString:String = generateDidDoc(this.pubKeyET.text.toString() )
//            val intent: Intent = Intent(this, NextActivity::class.java)
//            Log.i("tag", pubKeyET.text.toString())
//            //upload to google drive and get link
//            intent.putExtra("didDocString", didString)
//            intent.putExtra("issuerDid","issuerDid")
//            Log.i("tag", didString)
//            startActivity(intent)

            getPermission()
            checkForGooglePermissions()
            uploadFileToDrive()

        })
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun onUploadButtonClicked(uploadButton:Button) {
        uploadButton.setOnClickListener(
            View.OnClickListener {
                val file:Intent =   Intent(Intent.ACTION_OPEN_DOCUMENT,MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                startActivityForResult(file, RESULT_FIRST_USER)

            }
        )
    }

    //get permission for the device to access the files
    private fun getPermission() {
        //if the system is marshmallow or above get the run time permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
        ) {
            //permission was not enabled
            val permission = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            //show popup to request permission
            requestPermissions(permission, REQUEST_IMAGE_CAPTURE)

        }
    }

    //check if the device has google permission to access the google services
    private fun checkForGooglePermissions() {
        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(applicationContext),
                ACCESS_DRIVE_SCOPE,
                SCOPE_EMAIL,
                SCOPE_APP_DATA
            )
        ) {
            Log.i("drive", "oooooooooooooooooooooooooooooooooooops")
            GoogleSignIn.requestPermissions(
                this@MainActivity,
                RC_AUTHORIZE_DRIVE,
                GoogleSignIn.getLastSignedInAccount(applicationContext),
                ACCESS_DRIVE_SCOPE,
                SCOPE_EMAIL,
                SCOPE_APP_DATA
            )
        } else {
            driveSetUp()
        }
    }

    //request the google drive permission for the device
    private fun driveSetUp() {
        val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, setOf(Scopes.DRIVE_APPFOLDER)
        )
        credential.selectedAccount = mAccount!!.account
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("GoogleDriveIntegration 3")
            .build()
        val mGoogleDriveService = googleDriveService
        mDriveServiceHelper = mGoogleDriveService?.let { DriveServiceHelper(it) }
    }

    private fun uploadFileToDrive() {
        val file = File(Environment.getExternalStorageDirectory(), "Test.jpg")
        mDriveServiceHelper!!.uploadFile(file, "image/jpeg", null)
            ?.addOnSuccessListener { googleDriveFileHolder ->
                val gson = Gson()
                Log.i(
                    "drive uploading",
                    "on success File upload" + gson.toJson(googleDriveFileHolder)
                )
            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "drive uploading",
                    "on failure of file upload" + e.message
                )
            }
    }
}