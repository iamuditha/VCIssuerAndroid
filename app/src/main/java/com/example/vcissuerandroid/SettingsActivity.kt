package com.example.vcissuerandroid

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.vcissuerandroid.drive.DriveFileList
import com.example.vcissuerandroid.drive.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnSuccessListener
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_settings_acttivity)

        checkForGooglePermissions()

//        val thread = Thread {
//            getFolderID()
//        }
//        thread.start()

        getFolderID()
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

}