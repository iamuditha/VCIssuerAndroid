package com.example.vcissuerandroid

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import kotlinx.android.synthetic.main.toolbar.*
import com.google.api.client.json.gson.GsonFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


class MainActivity : BaseActivity() {

    private val REQUEST_ACCESS_STORAGE: Int = 100
    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    private val publicKey: String = ""
    private lateinit var pubKeyET: TextView
    private lateinit var nextBtn: Button
    private lateinit var uploadButton: Button

    @RequiresApi(Build.VERSION_CODES.Q)

    var gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listFilesInDrive()

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

        try {
            val thread = Thread{
//                writeToFile("helloworldIamuditha","testingFile.txt",this)
                val text = readFromFile("testingFile.txt",this)
                Log.i("readtext", text)

            }
            thread.start()
            thread.join() 
        }catch (e : Exception){
            Log.i("fucked", e.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 999 && resultCode == Activity.RESULT_OK && data!= null){
//            val destination =  File(Environment.getExternalStorageDirectory().absolutePath + "/filenamehjkl.txt");
//            val newDestination = File(applicationContext.filesDir.absolutePath + "/didDocument.txt")
//
//            val `in` = contentResolver.openInputStream(data.data!!)
//            if (`in` != null) {
//                copyInputStreamToFile(`in`, newDestination)
//            }
//
//        }
    }

    private fun generateDidDoc(publicKey: String): String {
        val did: String = DID.getInstance().generateDID()
        val didDoc: DIDDocument =
            DIDDocumentGenerator.getInstance().generateDIDDocument(did, publicKey);
        val gson = Gson()
        return gson.toJson(didDoc)

    }

    private fun onNextButtonClicked(nextBtn: Button) {
        nextBtn.setOnClickListener(View.OnClickListener {
//            val didString:String = generateDidDoc(this.pubKeyET.text.toString() )
            val intent: Intent = Intent(this, NextActivity::class.java)
//            Log.i("tag", pubKeyET.text.toString())
//            //upload to google drive and get link
//            intent.putExtra("didDocString", didString)
//            intent.putExtra("issuerDid","issuerDid")
//            Log.i("tag", didString)
            startActivity(intent)

//            getPermission()
//            checkForGooglePermissions()
//            uploadFileToDrive()

        })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onUploadButtonClicked(uploadButton: Button) {
        uploadButton.setOnClickListener(
            View.OnClickListener {
                val file: Intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
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
            requestPermissions(permission, REQUEST_ACCESS_STORAGE)

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

    private fun filePicker() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, 999)
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


            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        thread.start()
    }

    //write a string to a file
    private fun writeToFile(data: String, fileName: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput(fileName, MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
            Log.i("did", "Created a New file in the directory $fileName")

        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    //read the file content as a string
    private fun readFromFile(fileName: String, context: Context): String? {
        var mText = ""
        try {
            val inputStream: InputStream? = context.openFileInput(fileName)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append("\n").append(receiveString)
                }
                inputStream.close()
                mText = stringBuilder.toString()

            }
        } catch (e: FileNotFoundException) {
            Log.e("did", "File $fileName not found: $e")
        } catch (e: IOException) {
            Log.e("did", "Can not read file $fileName: $e")
        }
        Log.i("did", "$fileName contains : $mText")
        return mText
    }
}