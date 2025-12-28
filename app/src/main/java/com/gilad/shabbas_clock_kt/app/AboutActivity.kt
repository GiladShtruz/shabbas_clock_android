package com.gilad.shabbas_clock_kt.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.gilad.shabbas_clock_kt.R
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setupToolbar()
        setupButtons()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }


    private fun setupButtons() {
        val btnContactDeveloper = findViewById<MaterialButton>(R.id.btnContactDeveloper)
        val btnPrivacyPolicy = findViewById<MaterialButton>(R.id.btnPrivacyPolicy)

        btnContactDeveloper.setOnClickListener {
            sendEmailToDeveloper()
        }

        btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }
    }

    private fun sendEmailToDeveloper() {
        val subject = Uri.encode("בנוגע לאפליקציית מעורר לשבת")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:giladsh22@gmail.com?subject=$subject")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // אם אין אפליקציית מייל
        }
    }

    private fun showPrivacyPolicy() {
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = false
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/privacy_policy.html")
        }

        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setView(webView)
            .setPositiveButton("סגור", null)
            .show()
    }
}
