package com.my.ido4u

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.app.ActivityCompat
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

    private var centerLatLng = LatLng(31.776532, 35.198034)
    private var backupCenterLatLng = LatLng(0.0, 0.0)
    private var mapCircle: Circle? = null
    private var centerMarker: Marker? = null
    private var radiusSeekBar: SeekBar? = null
    private var seekBarMax = 0
    private var radius = MIN_RADIUS
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var mapAPI: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    var gson = Gson()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_location)

        setViewsAndFragment(savedInstanceState)
        createChooseLocationTutorial()
    }

    /**
     * Creates a tutorial for the ChooseLocationActivity
     */
    private fun createChooseLocationTutorial() {
        val sp = Ido4uApp.applicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val showedTutorial = sp.getBoolean(SHOWED_LOCATION_CHOICE_ACTIVITY_TUTORIAL, false)
        if (!showedTutorial) {
            val listOfViews = arrayOf<View>(
                findViewById(R.id.fragmentLayout),
                findViewById(R.id.mapAPI),
                findViewById(R.id.RadiusSeekBarLinearLayout),
                findViewById(R.id.approveLocationButton)
            )
            val listOfStrings = listOf(
                getString(R.string.choose_location_tutorial),
                getString(R.string.drag_marker_tutorial),
                getString(R.string.radius_bar_tutorial),
                getString(R.string.approve_location_tutorial)
            )
            createTutorial(
                this@ChooseLocationActivity,
                listOfStrings,
                SHOWED_LOCATION_CHOICE_ACTIVITY_TUTORIAL,
                *listOfViews
            )
        }
    }

    /**
     * Sets the views of the activity and the map fragment.
     */
    private fun setViewsAndFragment(savedInstanceState: Bundle?) {
        setSeekBar()
        setOkButton()
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapAPI) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        if (savedInstanceState != null) {
            val restoredBackupCenter = savedInstanceState.getString(BACKUP_CENTER_LOCATION)
            backupCenterLatLng = gson.fromJson(restoredBackupCenter, LatLng::class.java)
        }
    }

    /**
     * Sets the OK button and its' onClickListener.
     */
    private fun setOkButton() {
        val okButton = findViewById<Button>(R.id.approveLocationButton)
        okButton.setOnClickListener {
            val resultIntent = Intent(MAP_LOCATION_ACTION)
            val locationConditionData =
                LocationConditionData(centerLatLng.longitude, centerLatLng.latitude, radius)
            val condition = Task.Condition(
                Task.ConditionEnum.LOCATION,
                Gson().toJson(locationConditionData),
                locationConditionData.toString()
            )
            resultIntent.putExtra(CONDITION, Gson().toJson(condition))
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    /**
     * Sets the seek bar with which the user can define a radius.
     */
    private fun setSeekBar() {
        radiusSeekBar = findViewById<View>(R.id.RadiusSeekBar) as SeekBar
        seekBarMax = radiusSeekBar!!.max
//        radiusSeekBar!!.progress = (radius / RADIUS_MAX_IN_METERS * seekBarMax ).toInt()
        radiusSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val cur = radiusSeekBar!!.progress.toFloat()
                radius = (cur / seekBarMax * RADIUS_MAX_IN_METERS) + MIN_RADIUS
                if (mapCircle != null) {
                    mapCircle!!.radius = radius.toDouble()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /**
     * Updates the marker's location to be the last one known and set the circle's center to be it.
     */
    private val lastLocation: Unit
        get() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient!!.lastLocation
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        val defaultBackup = LatLng(0.0, 0.0)
                        centerLatLng = if (backupCenterLatLng == defaultBackup) {
                            LatLng(location.latitude, location.longitude)
                        } else {
                            backupCenterLatLng
                        }
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
                    .center(centerMarker!!.position)
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
        mapAPI!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                centerLatLng,
                14.0f
            )
        )
        mapAPI!!.uiSettings.isZoomControlsEnabled = true
        googleMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                centerLatLng = marker.position
                backupCenterLatLng = marker.position
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
            if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
                lastLocation
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(BACKUP_CENTER_LOCATION, gson.toJson(backupCenterLatLng))
    }

}