package com.adjm.aplikasibaru

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var menyala = false
    private var camId: String? = null
    private lateinit var cm: CameraManager
    private lateinit var tombol: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tombol = findViewById(R.id.tombol)
        status = findViewById(R.id.status)
        cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        camId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        tombol.setOnClickListener {
            val id = camId
            if (id == null) {
                Toast.makeText(this, "HP ini tidak punya lampu flash", Toast.LENGTH_LONG).show()
            } else {
                setSenter(id, !menyala)
            }
        }
    }

    private fun setSenter(id: String, on: Boolean) {
        try {
            cm.setTorchMode(id, on)
            menyala = on
            tombol.text = if (on) "MATIKAN" else "NYALAKAN"
            status.text = if (on) "Senter menyala" else "Senter mati"
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        val id = camId
        if (menyala && id != null) setSenter(id, false)
    }
}
