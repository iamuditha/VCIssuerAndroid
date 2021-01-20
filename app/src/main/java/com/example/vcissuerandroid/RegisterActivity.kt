package com.example.vcissuerandroid

import ContractorHandlers.IAMContractorHandler
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.toolbar.*
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import utilities.EthFunctions
import java.lang.Exception

class RegisterActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val intent: Intent = intent
        val doctorDid = intent.getStringExtra("DoctorDid")
        val didLink = intent.getStringExtra("didLink")
        val vcLink = intent.getStringExtra("vcLink")
        val didHash = intent.getStringExtra("didHash")
        val vcHash = intent.getStringExtra("vcHash")
        val email = intent.getStringExtra("email")

        register.setOnClickListener {
            resister(doctorDid!!,didLink!!,vcLink!!,didHash!!,vcHash!!,email!!)
        }

    }

    private fun resister(doctorDid:String, didLink:String, vcLink:String, didHash:String, vcHash:String,email:String){
        val thread = Thread{
            try {
                val web3j: Web3j = Web3j.build(HttpService("https://20c07c201b7f.ngrok.io"))
//                val web3j:Web3j = EthFunctions.connect("https://20c07c201b7f.ngrok.io")
                val credentials = WalletUtils.loadCredentials("123456",filesDir.absolutePath+"/wallet.json")
                val iamContractorHandler = IAMContractorHandler.getInstance()
                val iamContract = iamContractorHandler.getWrapperForContractor(web3j,getString(R.string.main_contractor_address),credentials)
                iamContract.registerDoctor(didHash, didLink, vcHash, vcLink, email, doctorDid, "issuerDid" ).send()
            }catch (e : Exception){
                Log.i("blockChain",e.toString())
            }
        }
        thread.start()
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
}