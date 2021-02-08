package com.example.vcissuerandroid

import ContractorHandlers.IAMContractorHandler
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.vcissuerandroid.utils.KeyHolder
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.toolbar.*
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import utilities.EthFunctions

class RegisterActivity : BaseActivity() {


    lateinit var progressUploadingToChain : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        sendEmail.visibility = View.GONE
        progressUploadingToChain = displayLoading(this, "Doctor is being Registered. Please Wait.....")

        val intent: Intent = intent
        val doctorDid = intent.getStringExtra("DoctorDid")
        val didLink = intent.getStringExtra("didLink")
        val vcLink = intent.getStringExtra("vcLink")
        val didHash = intent.getStringExtra("didHash")
        val vcHash = intent.getStringExtra("vcHash")
        val email = intent.getStringExtra("email")
        Log.i("blockChain", "vcLink: $vcLink")


        register.setOnClickListener {
            buttonEffect(register)
            register.isEnabled = false
            progressUploadingToChain.show()
            resister(doctorDid!!, didLink!!, vcLink!!, didHash!!, vcHash!!, email!!)
        }

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
    fun buttonEffect(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter(getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP)
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

    private fun resister(
        doctorDid: String,
        didLink: String,
        vcLink: String,
        didHash: String,
        vcHash: String,
        email: String
    ){
        val thread = Thread{
            try {
                val web3j: Web3j = Web3j.build(HttpService("http://35.238.77.15:8545"))
//                val web3j:Web3j = EthFunctions.connect("https://0cf4bdfe02ab.ngrok.io")
                val credentials = WalletUtils.loadCredentials(
                    "123456",
                    filesDir.absolutePath + "/wallet.json"
                )
                Log.i("blockChain",credentials.address)
                val iamContractorHandler = IAMContractorHandler.getInstance()
                val iamContract = iamContractorHandler.getWrapperForContractor(
                    web3j,
                    getString(R.string.main_contractor_address),
                    credentials
                )

                val status = iamContractorHandler.registerDoctor(iamContract,web3j,credentials,didLink,didHash,vcLink,vcHash,email,doctorDid,KeyHolder.getObject().did)
                Log.i("blockChain", "didHash: $didHash")
                Log.i("blockChain", "vcHash: $vcHash")
                Log.i("blockChain", "didLink: $didLink")
                Log.i("blockChain", "vcLink: $vcLink")
                Log.i("blockChain", "doctorDid: $doctorDid")
                Log.i("blockChain", "email: $email")
                Log.i("blockChain", "did: $KeyHolder.getObject().did")



                web3j.shutdown()
                Log.i("blockChain", "here is did : $doctorDid")
                if (status.isSuccess){
                    Log.i("blockChain", status.message)
                    runOnUiThread {
                        register.isEnabled = true
                        progressUploadingToChain.dismiss()
                        Toast.makeText(this,"Successfully Completed Registration ", Toast.LENGTH_SHORT).show()
                    }
                    sendEmail(doctorDid,email)

                }else{
                    runOnUiThread{
                        register.isEnabled = true
                        progressUploadingToChain.dismiss()
                        Toast.makeText(this,status.message, Toast.LENGTH_SHORT).show()
                    }
                    Log.i("blockChain", status.message)
                }

            }catch (e: Exception){
                Log.i("blockChain", e.toString())
                runOnUiThread {
                    register.isEnabled = true
                    progressUploadingToChain.dismiss()
                    Toast.makeText(this,"There is a problem. Please try Again", Toast.LENGTH_SHORT).show()
                }
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

//    fun composeEmail(addresses: Array<String?>?, subject: String?) {
//        val intent = Intent(Intent.ACTION_SENDTO)
//        intent.data = Uri.parse("mailto:")
//        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
//        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(intent)
//        }
//    }

    //send an email
    private fun sendEmail(publicKey: String, email: String){
        val selectorIntent = Intent(Intent.ACTION_SENDTO)
        selectorIntent.data = Uri.parse("mailto:")

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Public Key")
        emailIntent.putExtra(Intent.EXTRA_TEXT, publicKey)
        emailIntent.selector = selectorIntent

        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }
}