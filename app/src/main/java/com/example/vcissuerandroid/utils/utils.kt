package com.example.vcissuerandroid.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.*

object utils {

    //write a string to a file
     fun writeToFile(data: String, fileName: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput(fileName, AppCompatActivity.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
            val name = java.io.File(fileName).path
            Log.i("did", "Created a New file in the directory $name")

        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    //read the file content as a string
     fun readFromFile(fileName: String, context: Context): String? {
        Log.e("did", "i am called")

        var mText = ""
        try {
            val inputStream: InputStream? = context.openFileInput(fileName)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String?
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append("\n").append(receiveString)
                }
                inputStream.close()
                mText = stringBuilder.toString()

            }
        } catch (e: FileNotFoundException) {
            Log.e("did", "File $fileName not found: $e")
        } catch (e: IOException) {
            Log.e("did", "Can not read file $fileName: $e")
        }
        Log.i("did", "$fileName contains : $mText")
        return mText
    }
}