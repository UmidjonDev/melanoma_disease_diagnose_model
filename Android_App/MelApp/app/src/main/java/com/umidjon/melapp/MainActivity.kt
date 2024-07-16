package com.umidjon.melapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var melanomaDirection: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        melanomaDirection = findViewById(R.id.direction)

        melanomaDirection.setOnClickListener{
            val intent = Intent(this, Melanoma_diagnosis::class.java)
            startActivity(intent)
        }
    }
}