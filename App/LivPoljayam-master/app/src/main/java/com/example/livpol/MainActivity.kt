package com.example.livpol

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.livpol.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var allPoints: ArrayList<LatLng> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        binding.designBar.setOnClickListener {
            val intent = Intent(this, SearchByEpicActivity::class.java)
            startActivity(intent)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val handler = Handler(Looper.myLooper()!!)
        val runnable = Runnable{
            //update the ui here
            Log.d("onside the runnable","here we are")
            Thread.sleep(2000)

        }
        val runnable2 = Runnable{
            //update the ui here
            Log.d("onside the runnable 2","here we go")
            Thread.sleep(3000)

        }
//        handler.postDelayed(runnable,2000)
//        var i = 0
//        while (i<10){
//            Thread.sleep(1500)
//            Log.d("sleeping",i.toString())
//            i=i+1
//        }
        AsyncTask.execute(runnable)
        AsyncTask.execute(runnable2)

        //Queue [R1,ER1,
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get user's location
                getDeviceLocation()
            } else {
                // Handle permission denied case (optional)
            }
        }
    }

    private fun getDeviceLocation() {
        /*
         * Get the user's location using FusedLocationProviderClient
         */
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {

                val markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.polling_station)


                val ps1 = LatLng(location.latitude+0.012, location.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ps1)
                        .icon(markerIcon)
                )
                val ps2 = LatLng(location.latitude+0.0099, location.longitude+0.018)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ps2)
                        .icon(markerIcon)
                )
                val ps3 = LatLng(location.latitude+0.0002, location.longitude+0.010)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ps3)
                        .icon(markerIcon)
                )
                val ps4 = LatLng(location.latitude-0.009, location.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ps4)
                        .icon(markerIcon)
                )
                val ps5 = LatLng(location.latitude+0.002, location.longitude-0.01)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ps5)
                        .icon(markerIcon)
                )
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.isMyLocationEnabled = true
                val cameraPosition = CameraPosition.Builder()
                    .target(userLocation)
                    .zoom(14f)
                    .bearing(90f)
                    .tilt(40f)
                    .build()
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mMap: GoogleMap) {
        this.mMap = mMap
        getDeviceLocation()  // Call getDeviceLocation here to set initial user location

        mMap.setOnMarkerClickListener { marker ->
            startActivity(Intent(this, DataActivity::class.java))
            true
        }
    }

    override fun onBackPressed() {
        mMap.clear()
        super.onBackPressed()
    }
}
