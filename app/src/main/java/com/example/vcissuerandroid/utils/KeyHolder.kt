package com.example.vcissuerandroid.utils

import com.example.vcissuerandroid.MyDetails
import com.google.gson.JsonElement

object KeyHolder {

    var pKey : String = ""
    var isRegistered : Boolean? = null
    var dataObject : MyDetails = MyDetails("","","",null)

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

    fun getObject(): MyDetails {
        return dataObject
    }




}