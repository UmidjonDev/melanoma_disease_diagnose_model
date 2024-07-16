package com.umidjon.melapp

import android.app.Activity
import android.content.Context
import android.os.Environment
import kotlinx.coroutines.withContext
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.provider.MediaStore
import android.widget.ImageView
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.media.MediaScannerConnection
import java.lang.Exception
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.DecimalFormat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

interface ApiService {

    @Multipart
    @POST("/")
    suspend fun uploadImage(
        @Part filePart: MultipartBody.Part
    ): ResponseBody
}


class Melanoma_diagnosis : AppCompatActivity() {

    private lateinit var camera:Button
    private lateinit var Image_captured : ImageView

    private val retrofit : Retrofit = Retrofit.Builder()
        .baseUrl("https://79ce-37-110-210-91.ngrok-free.app")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiService : ApiService = retrofit.create((ApiService::class.java))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_melanoma_diagnosis)

        camera = findViewById(R.id.button2)
        Image_captured = findViewById(R.id.imageView6)

        camera.setOnClickListener{
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 123)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123 && resultCode == Activity.RESULT_OK){
            val photo = data?.extras?.get("data") as Bitmap

            Image_captured.setImageBitmap(photo)

            GlobalScope.launch(Dispatchers.IO){
                val apiResponse = performNetworkRequest(photo = photo)
                val resultDict = parseApiResponse(apiResponse)
                updateViews(resultDict)
            }
        }
    }

    //Kotlin coroutines instead of Asynchronous background performance like happened in Java
    private suspend fun performNetworkRequest(photo: Bitmap): String? {
        try {
            val savedImagePath = saveImageLocally(this@Melanoma_diagnosis, photo)

            val imageFile = File(savedImagePath)

            val imageRequestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

            val filePart = MultipartBody.Part.createFormData("file", imageFile.name, imageRequestBody)

            val response = apiService.uploadImage(filePart)
            val result = response.string()
            println(result)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseApiResponse(apiResponse : String?) : Map<String, Double>{
        val gson = Gson()
        return try {
            val resultType = object : TypeToken<Map<String, Double>>() {}.type
            val result = gson.fromJson<Map<String, Double>>(apiResponse, resultType)
            result ?: emptyMap()  // Return an empty map if result is null
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            emptyMap()  // Return an empty map if there is a parsing error
        }
    }

    private fun updateViews(resultDict : Map<String, Double>){
        runOnUiThread {
            val resultTextView_class = findViewById<TextView>(R.id.textView)
            val resultTextView_prob = findViewById<TextView>(R.id.textView3)

            if (resultDict.isNotEmpty()) {
                val entry = resultDict.entries.first()
                val className = entry.key
                val melanomaProbability = entry.value
                val formattedProbability = DecimalFormat("0.##").format(melanomaProbability * 100)

                resultTextView_class.text = className
                resultTextView_prob.text = "$formattedProbability%"
            } else {
                resultTextView_class.text = "Result Not Available"
                resultTextView_prob.text = ""
            }
        }
    }

    private suspend fun saveImageLocally(context : Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_$timeStamp.jpg"

        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        try {
            val file = File(storageDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            // Notify the MediaScanner about the new file so it appears in the gallery
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)

            // Print the absolute path for debugging
            println("Image saved at: ${file.absolutePath}")

            return@withContext file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the exception
            return@withContext ""
        }
    }
}