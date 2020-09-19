package com.my.ido4u

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class ChooseLocationActivity : FragmentActivity(), OnMapReadyCallback {

    private var centerLatLng = LatLng(31.772915, 35.218016)
    private var mapCircle: Circle? = null
    private var centerMarker: Marker? = null
    private var radiusSeekBar: SeekBar? = null
    private var seekBerMax = 0
    private var radius = DEFAULT_RADIUS
    private var fusedLocationClient: FusedLocationProviderClient? = null
    var mapAPI: GoogleMap? = null
    var mapFragment: SupportMapFragment? = null


    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_location)
        checkPermissions()
        setSeekBar()
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapAPI) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    /**
     * Updates the marker's location to be the last one known and set the circle's center to be it
     */
    private val lastLocation: Unit
        private get() {
            fusedLocationClient!!.lastLocation
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        centerLatLng = LatLng(location.latitude, location.longitude)
                        centerMarker!!.remove()
                        centerMarker = mapAPI!!.addMarker(
                            MarkerOptions().position(centerLatLng).draggable(true).title(
                                CENTER_MARKER
                            )
                        )
                        mapCircle!!.center = centerLatLng
                        mapAPI!!.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, 14.0f))
                        mapAPI!!.uiSettings.isZoomControlsEnabled = true
                    }
                }
        }

    /**
     * Sets the seek bar with which the user can define a radius
     */
    private fun setSeekBar() {
        radiusSeekBar = findViewById<View>(R.id.RadiusSeekBar) as SeekBar
        seekBerMax = radiusSeekBar!!.max
        radiusSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val cur = radiusSeekBar!!.progress.toFloat()
                radius = cur / seekBerMax * RADIUS_MAX_IN_METERS
                mapCircle!!.radius = radius.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val context = applicationContext
                showToast(context) // todo - remove
            }
        })
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mContext = applicationContext
            if (mContext.checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MAP_PIN_LOCATION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    /**
     * Creates the location marker with the last known location, and a circle around it.
     */
    private fun initializeCenterAndCircle() {
        centerMarker = mapAPI!!.addMarker(
            MarkerOptions().position(centerLatLng).draggable(true).title(CENTER_MARKER)
        )
        if(centerMarker != null) {
            mapCircle = mapAPI!!.addCircle(
                CircleOptions()
                    .center(centerMarker!!.getPosition())
                    .radius(radius.toDouble())
                    .strokeWidth(3f)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(70, 150, 50, 50))
            )
        }
    }

    private fun showToast(context: Context) { //todo - remove
        val duration = Toast.LENGTH_LONG
        val lat = centerMarker!!.position.latitude
        val lon = centerMarker!!.position.longitude
        val str = "lat: $lat\nlong: $lon\nradius: $radius"
        val toast = Toast.makeText(context, str, duration)
        toast.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapAPI = googleMap
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermissions()
        lastLocation
        initializeCenterAndCircle()
        mapAPI!!.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, 14.0f))
        mapAPI!!.uiSettings.isZoomControlsEnabled = true
        googleMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                val context = applicationContext
                centerLatLng = marker.position
                mapCircle!!.radius = radius.toDouble()
                mapCircle!!.center = centerMarker!!.position
                showToast(context)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == MAP_PIN_LOCATION_REQUEST_CODE) {
                lastLocation
            }
        }
    }

}