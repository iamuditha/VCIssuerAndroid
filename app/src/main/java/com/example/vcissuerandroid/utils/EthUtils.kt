package com.example.vcissuerandroid.utils

import com.google.gson.Gson
import crypto.did.DID
import crypto.did.DIDDocument
import crypto.did.DIDDocumentGenerator

object EthUtils {

    //create a didDocument
     fun generateDidDoc(publicKey: String): String {
        val did: String = DID.getInstance().generateDID()
        val didDoc: DIDDocument =
            DIDDocumentGenerator.getInstance().generateDIDDocument(did, publicKey);
        val gson = Gson()
        return gson.toJson(didDoc)

    }
}