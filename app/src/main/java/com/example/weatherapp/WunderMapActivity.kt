package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class WunderMapActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wundermap)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupWebView()

        if (checkLocationPermission()) {
            loadMapWithLocation()
        } else {
            requestLocationPermission()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                // Inject CSS to hide header, footer, and ads
                val js = "javascript:(function() { " +
                        "var style = document.createElement('style');" +
                        "style.innerHTML = 'header, footer, .ad-container, .commercial-unit, [id^=\"google_ads\"], .wu-ad, .city-header { display: none !important; }';" +
                        "document.head.appendChild(style);" +
                        "})()"
                view?.loadUrl(js)
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun loadMapWithLocation() {
        progressBar.visibility = View.VISIBLE
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                // WunderMap URL standard format
                val url = "https://www.wunderground.com/wundermap?lat=$lat&lon=$lon&zoom=8&radar=1&wxstn=1"
                webView.loadUrl(url)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación. Cargando mapa por defecto.", Toast.LENGTH_LONG).show()
                webView.loadUrl("https://www.wunderground.com/wundermap")
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener ubicación.", Toast.LENGTH_SHORT).show()
            webView.loadUrl("https://www.wunderground.com/wundermap")
            progressBar.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMapWithLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Cargando mapa por defecto.", Toast.LENGTH_LONG).show()
                webView.loadUrl("https://www.wunderground.com/wundermap")
                progressBar.visibility = View.GONE
            }
        }
    }
}
