package com.example.vcissuerandroid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.vcissuerandroid.drive.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import crypto.did.DID
import crypto.did.DIDDocument
import crypto.did.DIDDocumentGenerator
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import java.io.*


class DidGenerateActivity : BaseActivity() {

    private val REQUEST_ACCESS_STORAGE: Int = 100
    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    private var publicKey: String = ""
    private lateinit var pubKeyET: TextView
    private lateinit var generateDid: Button
    private lateinit var uploadButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_did_genarator)
        checkForGooglePermissions()
        getPermission()

        generateDid = findViewById(R.id.generateDid)
        pubKeyET = findViewById(R.id.pKeyET)
        uploadButton = findViewById(R.id.uploadBtn)


        //toolbar and drawer setup
        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        uploadButton.setOnClickListener{
            filePicker()
        }

        generateDid.setOnClickListener{
            if (publicKey == "" && pubKeyET.text.toString().trim() == ""){
                Toast.makeText(this, "Could not find the Public Key", Toast.LENGTH_SHORT).show()
            }else{
                if (publicKey == ""){
                    publicKey = pubKeyET.text.toString().trim()
                }
                    Toast.makeText(this,"I am Pressed",Toast.LENGTH_SHORT).show()
                    val didDocument = generateDidDoc(publicKey)
                    Log.i("didGenerator", publicKey)
                    Log.i("didGenerator", didDocument)
                    val thread = Thread{
                        uploadFileToDrive(didDocument.toByteArray(),didDocument)
                    }
                    thread.start()
           }
        }

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == Activity.RESULT_OK && data!= null){
            Log.i("filePicker", data.data!!.path!!)

            if (data.data != null){
                publicKey = readTextFromUri(data.data!!)!!
                Log.i("filePicker", publicKey)
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
                this@DidGenerateActivity,
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
        val mAccount = GoogleSignIn.getLastSignedInAccount(this@DidGenerateActivity)
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

    private fun filePicker() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, 999)
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
            Log.i("filePicker", e.toString())
            null
        }
    }
    private fun uploadFileToDrive(byteArray: ByteArray,didDocument:String) {
        runOnUiThread{
//            progress.show()
        }

        mDriveServiceHelper!!.uploadFile(byteArray, "application/json", null,"didDocument.json")
            ?.addOnSuccessListener { googleDriveFileHolder ->
                val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                val v = gson.toJson(googleDriveFileHolder)
                val obj =  JSONObject(v)
                Log.i("fileUploading", obj["webContentLink"].toString())
                Log.i(
                    "fileUploading",
                    "on success File upload" + gson.toJson(googleDriveFileHolder)
                )
                runOnUiThread{
//                    progress.dismiss()
                }
                Toast.makeText(this,"Successfully Uploaded",Toast.LENGTH_SHORT).show()
                val intent = Intent(this, VCGenerateActivity::class.java)
                intent.putExtra("didDocString", didDocument)
                intent.putExtra("didDocLink",obj["webContentLink"].toString())
                startActivity(intent)
//                goToNextActivity()

            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "fileUploading",
                    "on failure of file upload" + e.message
                )
                runOnUiThread{
//                    progress.dismiss()
                }
                Toast.makeText(this,"Could Not Create Your File. Please Check Your Connection and Try Again.",Toast.LENGTH_SHORT).show()

            }
    }
}