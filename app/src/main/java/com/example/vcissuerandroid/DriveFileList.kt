package com.example.vcissuerandroid

import com.google.api.services.drive.model.File

object DriveFileList {

    private val uploadedFileList = ArrayList<File>()

    private var didDocumentLink = ""
    private var VCLink = ""


    fun setDidDocumentLink(link:String){
        didDocumentLink = ""
        didDocumentLink = link
    }
    fun setVCLink(link:String){
        VCLink = ""
        VCLink = link
    }
    fun getDidDocumentLink(): String {
        return didDocumentLink
    }

    fun setVCLink(): String {
        return VCLink
    }

    fun addFile(file: File): ArrayList<File> {
        uploadedFileList.add(file)
        return uploadedFileList
    }

    fun driveFileList(): ArrayList<File> {
        return uploadedFileList
    }

    fun isFileAvailable(fileName : String): Boolean {
        var isFileAvailable = false
        for (file in uploadedFileList){
            if(file.name == fileName){
                isFileAvailable = true
                break
            }
        }
        return isFileAvailable
    }
    fun getFolderId(fileName: String): String? {
        var fileId = ""
        for (file in uploadedFileList){
            if(file.name == fileName){
                fileId = file.id
                break
            }
        }
        return fileId
    }
}