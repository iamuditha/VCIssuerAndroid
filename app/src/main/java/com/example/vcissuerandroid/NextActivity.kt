package com.example.vcissuerandroid

import ContractorHandlers.IAMContractorHandler
import ContractorHandlers.MainContractorHandler
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import crypto.VC.VerifiableClaim
import crypto.VC.VerifiableClaimGenerator
import crypto.did.DID
import crypto.did.DIDDocument
import org.bouncycastle.asn1.cms.CMSAttributes.messageDigest
import org.web3j.crypto.WalletUtils
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import utilities.EthFunctions

class NextActivity : AppCompatActivity() {
    lateinit var didDoc:DIDDocument
    lateinit var didHash:String
    lateinit var gender: String

    lateinit var registerBtn:Button
    lateinit var fName:EditText;
    lateinit var lName:EditText;
    lateinit var email:EditText;
    lateinit var hospital:EditText;
    lateinit var speciality:EditText;
    lateinit var genderRG:RadioGroup;
    lateinit var male:RadioButton
    lateinit var female:RadioButton;

    lateinit var issuerDid:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

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
//        didDoc = gson.fromJson(intent.getStringExtra("didDocString"), DIDDocument::class.java)
//        issuerDid = intent.getStringExtra("issuerDid")
//        var messageDigest:MessageDigest = MessageDigest.getInstance("sha-512")
//        didHash = String(messageDigest.digest(intent.getStringExtra("didDocString").toByteArray()),StandardCharsets.UTF_8)


        onRegisterButtonClicked(registerBtn);
        onCheckedGenderRG(genderRG)

    }
    fun onCheckedGenderRG(genderRG:RadioGroup) {
        genderRG.setOnCheckedChangeListener { group, checkedId ->
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


    fun generateVC():String {

        Log.i("did", didHash)

        var gson = Gson()
        var vc:VerifiableClaim = VerifiableClaimGenerator.generateVC(didDoc.did, fName.text.toString(),
            lName.text.toString(),email.text.toString(),"male",speciality.text.toString(),
            hospital.text.toString(), didHash,issuerDid)

        var vcString = gson.toJson(vc);
        return vcString
    }

    fun onRegisterButtonClicked(regBtn:Button) {
        regBtn.setOnClickListener(View.OnClickListener{
            var vcString:String = generateVC()

            //upload did and vc doc to google drive and get downloadable links
            var vcLink:String  =""
            var didLink:String= ""
//            val web3j: Web3j = Web3j.build(HttpService("https://c7fc09575149.ngrok.io"))
//
//            val credentials = WalletUtils.loadCredentials("23456","./")

            val iamContractorHandler = IAMContractorHandler.getInstance()

//            val iamContract = iamContractorHandler.getWrapperForContractor(web3j,getString(R.string.main_contractor_address),credentials)
            var messageDigest:MessageDigest = MessageDigest.getInstance("sha-512")
            var vcHash = String(messageDigest.digest(vcString.toByteArray()),StandardCharsets.UTF_8)
            Log.i("tag",vcString)
//            iamContract.registerDoctor(didHash, didLink, vcHash, vcLink, email.text.toString(), didDoc.did, "issuerDid" )
//
        })
    }
}