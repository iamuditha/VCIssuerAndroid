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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.gson.Gson
import crypto.did.DID
import crypto.did.DIDDocument
import crypto.did.DIDDocumentGenerator
import kotlinx.android.synthetic.main.toolbar.*
import java.io.*
import java.util.*


class MainActivity : BaseActivity() {

    private val REQUEST_ACCESS_STORAGE: Int = 100
    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    private var publicKey: String = ""
    private lateinit var pubKeyET: TextView
    private lateinit var nextBtn: Button
    private lateinit var uploadButton: Button

//    @RequiresApi(Build.VERSION_CODES.Q)

//    var gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//        .requestEmail()
//        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        nextBtn = findViewById(R.id.next)
        pubKeyET = findViewById(R.id.pKeyET)
        uploadButton = findViewById(R.id.uploadBtn)

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
//        Glide.with(this).load(url).apply(RequestOptions.circleCropTransform()).into(navUserImage)

        getPermission()

//        checkForDidDocument()
        listFilesInDrive()

        val threadf = Thread{
            downloadFileFromDrive("1KAB-FGS4b5Xj2Qp8MjKr-YZhrDtntZ96")
        }
        threadf.start()
        uploadButton.setOnClickListener{
            filePicker()
        }

        nextBtn.setOnClickListener{
            if (publicKey == ""){
                Toast.makeText(this,"Could not find the Public Key", Toast.LENGTH_SHORT).show()
            }else{
                val didDocument = generateDidDoc(publicKey)
                val intent = Intent(this, NextActivity::class.java)
                intent.putExtra("didDocString", didDocument)
                startActivity(intent)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == Activity.RESULT_OK && data!= null){
            Log.i("filePicker", data.data!!.path!!)

            if (data.data != null){
                publicKey = readTextFromUri(data.data!!)!!
                Log.i("filePicker",publicKey)
            }
        }
    }

    private fun generateDidDoc(publicKey: String): String {
        val did: String = DID.getInstance().generateDID()
        val didDoc: DIDDocument =
            DIDDocumentGenerator.getInstance().generateDIDDocument(did, publicKey);
        val gson = Gson()
        return gson.toJson(didDoc)

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
    private fun downloadFileFromDrive(id:String) {
        val file = File(filesDir.absolutePath, "issuerDid.jpg")
        if (mDriveServiceHelper == null){
            checkForGooglePermissions()
        }
        mDriveServiceHelper!!.downloadFile(file, id)
            ?.addOnSuccessListener { googleDriveFileHolder ->
                val gson = Gson()
                Log.i(
                    "fileDownloading",
                    "on success File download" + gson.toJson(googleDriveFileHolder)
                )
            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "fileDownloading",
                    "on failure of file download" + e.message
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
                for (i in DriveFileList.driveFileList()) {
                    Log.i("myFileList", i.toString())
                }

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
                var receiveString: String?
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

    private fun readTextFromUri(uri: Uri): String?{
        return try {
            val `in`: InputStream? = contentResolver.openInputStream(uri)
            val r = BufferedReader(InputStreamReader(`in`))
            val total = StringBuilder()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append(line).append('\n')
            }
            total.toString()
        } catch (e: Exception) {
            Log.i("filePicker",e.toString())
            null
        }
    }

    private fun randomString(): String{
        return UUID.randomUUID().toString()
    }

    private fun checkForDidDocument(){
        val fileListingThread = Thread{
            listFilesInDrive()
        }
        fileListingThread.start()

        val creatingDidThread = Thread{
            if (DriveFileList.isFileAvailable("issuerDid.txt")){
                val fileId = DriveFileList.getFolderId("issuerDid.txt")
            }
        }
    }
}