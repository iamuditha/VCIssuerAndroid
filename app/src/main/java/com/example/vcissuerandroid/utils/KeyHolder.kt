package com.example.vcissuerandroid.utils

import com.example.vcissuerandroid.myDetails
import com.google.gson.JsonElement
import java.security.PrivateKey

object KeyHolder {

    var pKey : String = ""

    var dataObject : myDetails = myDetails("","","",null)

    fun setPrivateKey(key:String){
        pKey = key
    }

    fun getPrivateKey(): String {
        return pKey
    }

    fun setObject(did:String,privateKey: String, publicKey:String, wallet : JsonElement){
        dataObject.did = did
        dataObject.privateKey = privateKey
        dataObject.publicKey = publicKey
        dataObject.wallet = wallet

    }

    fun getObject(): myDetails {
        return dataObject
    }


}