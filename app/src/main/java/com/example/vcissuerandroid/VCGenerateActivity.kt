package com.example.vcissuerandroid

import ContractorHandlers.IAMContractorHandler
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.vcissuerandroid.drive.DriveFileList
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
import crypto.VC.VerifiableClaim
import crypto.VC.VerifiableClaimGenerator
import crypto.did.DIDDocument
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class VCGenerateActivity : AppCompatActivity() {
    lateinit var didDoc:DIDDocument
    lateinit var didHash:String
    lateinit var gender: String

    lateinit var registerBtn:Button
    lateinit var fName:EditText
    lateinit var lName:EditText
    lateinit var email:EditText
    lateinit var hospital:EditText
    lateinit var speciality:EditText
    lateinit var genderRG:RadioGroup
    lateinit var male:RadioButton
    lateinit var female:RadioButton

    private lateinit var issuerDid:String

    private var RC_AUTHORIZE_DRIVE = 101
    private var ACCESS_DRIVE_SCOPE = Scope(Scopes.DRIVE_FILE)
    private var SCOPE_EMAIL = Scope(Scopes.EMAIL)
    private var SCOPE_APP_DATA = Scope(Scopes.DRIVE_APPFOLDER)



    private var googleDriveService: Drive? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vc_generator)
        checkForGooglePermissions()

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        //initialize xml
        registerBtn = findViewById(R.id.registerBtn)
        fName = findViewById(R.id.fNameET)
        lName = findViewById(R.id.lNameET)
        email = findViewById(R.id.emailET)
        hospital = findViewById(R.id.hospitalET)
        speciality = findViewById(R.id.specialityET)
        genderRG = findViewById(R.id.genderRG)
        male = findViewById(R.id.male)
        female = findViewById(R.id.female)

        var gson = Gson()
        val did = "mudiddocument"
        didDoc = DIDDocument("didididid", "eeddgfhkmk")
        issuerDid = "dfbkhkjkj"
//        didDoc = gson.fromJson(intent.getStringExtra("didDocString"), DIDDocument::class.java)
//        issuerDid = intent.getStringExtra("issuerDid")
//        var messageDigest:MessageDigest = MessageDigest.getInstance("sha-512")
//        didHash = String(messageDigest.digest(intent.getStringExtra("didDocString").toByteArray()),StandardCharsets.UTF_8)
        didHash = "ddkfnsdfd"


        onRegisterButtonClicked(registerBtn);
        onCheckedGenderRG(genderRG)

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


    private fun generateVC():String {

//        Log.i("did", didHash)

        val gson = Gson()
        val vc:VerifiableClaim = VerifiableClaimGenerator.generateVC(
            didDoc.did, fName.text.toString(),
            lName.text.toString(), email.text.toString(), "male", speciality.text.toString(),
            hospital.text.toString(), didHash, issuerDid
        )

        val vcString = gson.toJson(vc);
        return vcString
    }

    private fun onRegisterButtonClicked(regBtn: Button) {
        regBtn.setOnClickListener(View.OnClickListener {
            val vcString: String = generateVC()

            val writeThread = Thread {
                writeToFile(vcString, "VerifiableClaim.txt", this)
                writeToFile("mydid", "didDocument.txt", this)

            }
            writeThread.start()

            val uploadThread = Thread {
                writeThread.join()
                val dir = filesDir.absolutePath
                uploadFileToDrive(java.io.File("$dir/VerifiableClaim.txt"),"VC")
                uploadFileToDrive(java.io.File("$dir/didDocument.txt"),"did")

            }
            uploadThread.start()

            val ethThread = Thread{
                uploadThread.join()

                val prefs: SharedPreferences = getSharedPreferences("PROFILE_DATA", MODE_PRIVATE)
                val email: String? = prefs.getString("email", "no email")

                val messageDigest: MessageDigest = MessageDigest.getInstance("sha-512")
                var vcHash = String(
                    messageDigest.digest(vcString.toByteArray()),
                    StandardCharsets.UTF_8
                )

            }



            //upload did and vc doc to google drive and get downloadable links
            var vcLink: String = ""
            var didLink: String = ""
//            val web3j: Web3j = Web3j.build(HttpService("https://c7fc09575149.ngrok.io"))
//
//            val credentials = WalletUtils.loadCredentials("23456","./")

            val iamContractorHandler = IAMContractorHandler.getInstance()

//            val iamContract = iamContractorHandler.getWrapperForContractor(web3j,getString(R.string.main_contractor_address),credentials)

            Log.i("tag", vcString)
//            iamContract.registerDoctor(didHash, didLink, vcHash, vcLink, email.text.toString(), didDoc.did, "issuerDid" )
//
        })
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

    //upload a file to the drive
    private fun uploadFileToDrive(file: java.io.File, type:String) {
        mDriveServiceHelper!!.uploadFile(file, "text/plain", null)
            ?.addOnSuccessListener { googleDriveFileHolder ->
                val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                val v = gson.toJson(googleDriveFileHolder)
                val obj =  JSONObject(v)
                Log.i("logininfo", obj["webContentLink"].toString())

                if (type == "VC"){
                    DriveFileList.setVCLink(obj["webContentLink"].toString())
                }else if (type =="did"){
                    DriveFileList.setDidDocumentLink(obj["webContentLink"].toString())
                }
                Log.i(
                    "logininfo",
                    "on success File upload" + gson.toJson(googleDriveFileHolder)
                )
            }
            ?.addOnFailureListener { e ->
                Log.i(
                    "logininfo",
                    "on failure of file upload" + e.message
                )
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