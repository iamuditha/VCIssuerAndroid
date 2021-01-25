package com.example.vcissuerandroid

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
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
import java.util.*

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


    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null

    val gson = Gson().newBuilder().disableHtmlEscaping().create()

    lateinit var progressUploading : ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vc_generator)
        checkForGooglePermissions()

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        //get data from shared preference
        val prefs: SharedPreferences = getSharedPreferences("MY_DATA", MODE_PRIVATE)
//        didDocument = prefs.getString("myDid", null)
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

        progressUploading = displayLoading(this, "Uploading the Verifiable Claim. Please Wait...")


        val intent: Intent = intent
        val didDocument = intent.getStringExtra("didDocString")
        Log.i("blockchainnew", "intent checking in vc $didDocument")
        Log.i("verifiableClaim", "printed here" + didDocument!!)


        val didLink = intent.getStringExtra("didDocLink")
        did = gson.fromJson(didDocument, DIDDocument::class.java).did

        Log.i("verifiableClaim", "printed here" + didLink!!)
        Log.i("verifiableClaim", did)


        generateVC.setOnClickListener {
            buttonEffect(generateVC, R.color.gradient_end_color)
            generateVC.isEnabled = false
            if (fName.text.toString()=="" || lName.text.toString()=="" ||email.text.toString()==""||hospital.text.toString()==""||speciality.text.toString()==""){
                generateVC.isEnabled = true
                Toast.makeText(this, "Please fill All the Fields", Toast.LENGTH_SHORT).show()
            }
            else if (!isValidEmail(email.text)){
                generateVC.isEnabled = true
                Toast.makeText(this, "Wrong Email. Please Check",Toast.LENGTH_SHORT).show()
            }
            else{
                progressUploading.show()
                val thread = Thread {
                    val verifiableClaim = generateDocVC(didDocument)
                    val messageDigest: MessageDigest = MessageDigest.getInstance("sha-512")

                    val vc = gson.fromJson(verifiableClaim, VCCover::class.java)
                    val vcHash = String(
                        messageDigest.digest(gson.toJson(vc).toByteArray()),
                        StandardCharsets.UTF_8
                    )
                    uploadFileToDrive(verifiableClaim.toByteArray(), did, didLink, didHash, vcHash)
                }
                thread.start()
            }

        }

        onCheckedGenderRG(genderRG)

    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target!!).matches()
    }

    //display loading dialog
    private fun displayLoading(context: Context, message: String): ProgressDialog {
        val progress = ProgressDialog(context)
        progress.setMessage(message)
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progress.isIndeterminate = true
        progress.setCancelable(false)
        return progress
    }

    @SuppressLint("ClickableViewAccessibility")
    fun buttonEffect(button: View, color : Int) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter(getColor(color), PorterDuff.Mode.SRC_ATOP)
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


    private fun generateDocVC(didDocument: String): String {

        Log.i("downloading", "below gson" + KeyHolder.getPrivateKey())

        val data = KeyHolder.getObject()
        val privateKey = data.privateKey
        Log.i("downloading", privateKey)
//        val issuerDid
        val issuerDid = data.did

        Log.i("downloading", issuerDid)

        val messageDigest: MessageDigest = MessageDigest.getInstance("sha-512")
        didHash = Base64.getEncoder().encodeToString(messageDigest.digest(didDocument!!.toByteArray()))
        Log.i("blockchain", "didDocument " + didDocument)
        Log.i("blockchain", "didHash" + didHash)
        Log.i("blockchain", "didHash" + did)



        val vc: VerifiableClaim = VerifiableClaimGenerator.generateVC(
            did, fName.text.toString(),
            lName.text.toString(), email.text.toString(), "male", speciality.text.toString(),
            hospital.text.toString(), didHash, "issuerDid"
        )
        val vcHash = Base64.getEncoder().encodeToString(
            messageDigest.digest(
                gson.toJson(vc).toByteArray()
            )
        )
        Log.i("downloading", "below vc cover" + gson.toJson(vc).toString())
        val vcCover = VcSigner.signVC(
            vc,
            vcHash,
            KeyHandler.getInstance()
                .loadRSAPrivateFromPlainText(privateKey)
        )
        Log.i("downloading", "below vc cover" + KeyHolder.getPrivateKey())

        return gson.toJson(vcCover)

    }


    private fun uploadFileToDrive(
        byteArray: ByteArray,
        did: String,
        didLink: String,
        didHash: String,
        vcHash: String
    ) {
        Log.i("blockchain", "link " + didLink)
        mDriveServiceHelper!!.uploadFileToRoot(
            byteArray,
            "application/json",
            null,
            "verifiableClaim.json"
        )
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
                    progressUploading.dismiss()
                    generateVC.isEnabled = true
                }
                Toast.makeText(this, "Successfully Uploaded", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.putExtra("DoctorDid", did)
                intent.putExtra("didLink", didLink)
                intent.putExtra("vcLink", obj["webContentLink"].toString())
                intent.putExtra("didHash", didHash)
                intent.putExtra("vcHash", vcHash)
                intent.putExtra("email", email.text.toString())
                startActivity(intent)
//                goToNextActivity()

            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "fileUploading",
                    "on failure of file upload" + e.message
                )
                runOnUiThread {
                    progressUploading.dismiss()
                    generateVC.isEnabled = true
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