package com.my.ido4u

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.google.gson.Gson

/**
 * An activity that allows the user to choose an area on the map in which his\her task's actions
 * should be performed.
 */
class ChooseLocationActivity : FragmentActivity(), OnMapReadyCallback {

    private var centerLatLng = LatLng(31.772915, 35.218016) //todo
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

        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this@ChooseLocationActivity)
        setViewsAndFragment()
        createChooseLocationTutorial()

    }

    /**
     * Creates a tutorial for the ChooseLocationActivity
     */
    private fun createChooseLocationTutorial() {
        val listOfViews = arrayOf<View>(
            findViewById(R.id.fragmentLayout),
            findViewById(R.id.mapAPI),
            findViewById(R.id.RadiusSeekBar),
            findViewById(R.id.approveLocationButton)
        )
        val listOfStrings = listOf(
            getString(R.string.choose_location_tutorial),
            getString(R.string.drag_marker_tutorial),
            getString(R.string.radius_bar_tutorial),
            getString(R.string.approve_location_tutorial)
        )
        createTutorial(this@ChooseLocationActivity, listOfStrings, *listOfViews) //todo
    }

    /**
     * Sets the views of the activity and the map fragment.
     */
    private fun setViewsAndFragment() {
        setSeekBar()
        setOkButton()
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapAPI) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    /**
     * Sets the OK button and its' onClickListener.
     */
    private fun setOkButton() {
        val OkButton = findViewById<Button>(R.id.approveLocationButton)
        OkButton.setOnClickListener(View.OnClickListener {
            val resultIntent = Intent(MAP_LOCATION_ACTION)
            val locationConditionData =
                LocationConditionData(centerLatLng.longitude, centerLatLng.latitude, radius)
            val condition = Task.Condition(
                Task.ConditionEnum.LOCATION,
                Gson().toJson(locationConditionData),
                locationConditionData.toString()
            )
//            resultIntent.putExtra(MARKER_LAT_LNG, centerLatLng)
//            resultIntent.putExtra(RADIUS, radius)
            resultIntent.putExtra(CONDITION, Gson().toJson(condition))
            setResult(RESULT_OK, resultIntent)
            finish()
        })
    }

    /**
     * Sets the seek bar with which the user can define a radius.
     */
    private fun setSeekBar() {
        radiusSeekBar = findViewById<View>(R.id.RadiusSeekBar) as SeekBar
        seekBerMax = radiusSeekBar!!.max
        radiusSeekBar!!.progress = (radius / RADIUS_MAX_IN_METERS * seekBerMax ).toInt()
        radiusSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val cur = radiusSeekBar!!.progress.toFloat()
                radius = cur / seekBerMax * RADIUS_MAX_IN_METERS
                mapCircle!!.radius = radius.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /**
     * Updates the marker's location to be the last one known and set the circle's center to be it.
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
     * Creates the location marker with the last known location, and a circle around it.
     */
    private fun initializeCenterAndCircle() {
        centerMarker = mapAPI!!.addMarker(
            MarkerOptions().position(centerLatLng).draggable(true).title(CENTER_MARKER)
        )
        if (centerMarker != null) {
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

    override fun onMapReady(googleMap: GoogleMap) {
        mapAPI = googleMap
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this@ChooseLocationActivity)
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