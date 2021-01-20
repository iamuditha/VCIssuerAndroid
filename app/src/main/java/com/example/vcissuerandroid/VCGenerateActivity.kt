package com.example.vcissuerandroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import com.example.vcissuerandroid.drive.DriveServiceHelper
import com.example.vcissuerandroid.utils.KeyHolder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import crypto.KeyHandler
import crypto.VC.VCCover
import crypto.VC.VcSigner
import crypto.VC.VerifiableClaim
import crypto.VC.VerifiableClaimGenerator
import crypto.did.DIDDocument
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class VCGenerateActivity : BaseActivity() {
    lateinit var gender: String
    lateinit var generateVC: Button
    lateinit var fName: EditText
    lateinit var lName: EditText
    lateinit var email: EditText
    lateinit var hospital: EditText
    lateinit var speciality: EditText
    lateinit var genderRG: RadioGroup
    lateinit var male: RadioButton
    lateinit var female: RadioButton

    lateinit var did: String
    lateinit var didHash:String
    lateinit var vcHash: String

    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)

    private var privateKeyId: String? = null
    private var didDocument: String? = null


    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vc_generator)
        checkForGooglePermissions()

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        //get data from shared preference
        val prefs: SharedPreferences = getSharedPreferences("MY_DATA", MODE_PRIVATE)
        didDocument = prefs.getString("myDid", null)
        privateKeyId = prefs.getString("privateKeyId", null)

        //initialize xml
        generateVC = findViewById(R.id.vcGenerator)
        fName = findViewById(R.id.fNameET)
        lName = findViewById(R.id.lNameET)
        email = findViewById(R.id.emailET)
        hospital = findViewById(R.id.hospitalET)
        speciality = findViewById(R.id.specialityET)
        genderRG = findViewById(R.id.genderRG)
        male = findViewById(R.id.male)
        female = findViewById(R.id.female)


        val intent: Intent = intent
        val didDocument = intent.getStringExtra("didDocString")
        Log.i("verifiableClaim", "printed here" + didDocument!!)


        val didLink = intent.getStringExtra("didDocLink")
        val gson = Gson()
        did = gson.fromJson(didDocument, DIDDocument::class.java).did

        Log.i("verifiableClaim", "printed here"+didLink!!)
        Log.i("verifiableClaim", did)


        generateVC.setOnClickListener {
            val thread = Thread {
                val verifiableClaim = generateDocVC()
                val messageDigest: MessageDigest = MessageDigest.getInstance("sha-512")

                val gson = Gson()
                val vc = gson.fromJson(verifiableClaim,VCCover::class.java)
                val vcHash = String(
                    messageDigest.digest(gson.toJson(vc).toByteArray()),
                    StandardCharsets.UTF_8
                )
                uploadFileToDrive(verifiableClaim.toByteArray(),did,didLink,didHash,vcHash)
            }
            thread.start()
        }

        onCheckedGenderRG(genderRG)

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

    private fun onCheckedGenderRG(genderRG: RadioGroup) {
        genderRG.setOnCheckedChangeListener { _, checkedId ->
            run {
                if (checkedId == R.id.male) {
                    gender = "male"
                }
                if (checkedId == R.id.female) {
                    gender = "female"
                }
            }
        }
    }


    private fun generateDocVC(): String {

        val gson = Gson()
        Log.i("downloading", "below gson" + KeyHolder.getPrivateKey())

        val data = KeyHolder.getObject()
        val privateKey = data.privateKey
        Log.i("downloading",privateKey )
//        val issuerDid
        val issuerDidDocument = data.did
//
        val issuerDid = gson.fromJson(issuerDidDocument,DIDDocument::class.java).did
        Log.i("downloading",issuerDid)

        val messageDigest: MessageDigest = MessageDigest.getInstance("sha-512")
        didHash = messageDigest.digest(did.toByteArray()).toString()


        val vc: VerifiableClaim = VerifiableClaimGenerator.generateVC(
            did, fName.text.toString(),
            lName.text.toString(), email.text.toString(), "male", speciality.text.toString(),
            hospital.text.toString(), didHash, "issuerDid"
        )
        val vcHash = String(
            messageDigest.digest(gson.toJson(vc).toByteArray()),
            StandardCharsets.UTF_8
        )
        Log.i("downloading", "below vc cover" + KeyHolder.getPrivateKey())
        val vcCover = VcSigner.signVC(
            vc,
            vcHash,
            KeyHandler.getInstance()
                .loadRSAPrivateFromPlainText(privateKey)
        )
        Log.i("downloading", "below vc cover" + KeyHolder.getPrivateKey())

        return gson.toJson(vcCover)

    }


    private fun uploadFileToDrive(byteArray: ByteArray, did: String, didLink:String, didHash:String, vcHash:String) {
        runOnUiThread {
//            progress.show()
        }

        mDriveServiceHelper!!.uploadFile(byteArray, "application/json", null, "verifiableClaim.json")
            ?.addOnSuccessListener { googleDriveFileHolder ->
                val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                val v = gson.toJson(googleDriveFileHolder)
                val obj = JSONObject(v)
                Log.i("fileUploading", obj["webContentLink"].toString())
                Log.i(
                    "fileUploading",
                    "on success File upload" + gson.toJson(googleDriveFileHolder)
                )
                runOnUiThread {
//                    progress.dismiss()
                }
                Toast.makeText(this, "Successfully Uploaded", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.putExtra("DoctorDid", did)
                intent.putExtra("didLink",didLink)
                intent.putExtra("vcLink", obj["webContentLink"].toString())
                intent.putExtra("didHash",didHash)
                intent.putExtra("vcHash",vcHash)
                intent.putExtra("email",email.text.toString())
                startActivity(intent)
//                goToNextActivity()

            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "fileUploading",
                    "on failure of file upload" + e.message
                )
                runOnUiThread {
//                    progress.dismiss()
                }
                Toast.makeText(
                    this, "Could Not Create Your File. Please Check Your Connection and Try Again.",
                    Toast.LENGTH_SHORT
                ).show()

            }
    }

    //write a string to a file
    private fun writeToFile(data: String, fileName: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput(fileName, MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
            val name = java.io.File(fileName).path
            Log.i("did", "Created a New file in the directory $name")

        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }


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
        val mGoogleDriveService = googleDriveService
        mDriveServiceHelper = mGoogleDriveService?.let { DriveServiceHelper(it) }
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
                this@VCGenerateActivity,
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
}