package com.example.getlocationfrommapexample;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int MAP_PIN_LOCATION_REQUEST_CODE = 5;
    public static final float DEFAULT_RADIUS = 500;
    public static final int RADIUS_MAX_IN_METERS = 5000;
    public static final String CENTER_MARKER = "centerMarker";

    private LatLng centerLatLng = new LatLng(31.772915, 35.218016);
    private Circle mapCircle;
    private Marker centerMarker;
    private SeekBar radiusSeekBar;
    private int seekBerMax;
    private float radius = DEFAULT_RADIUS;

    GoogleMap mapAPI;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapAPI);
        mapFragment.getMapAsync(this);
        setSeekBar();

    }

    private void setSeekBar() {
        radiusSeekBar = (SeekBar) findViewById(R.id.RadiusSeekBar);
        seekBerMax = radiusSeekBar.getMax();
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float cur = (float) radiusSeekBar.getProgress();
                radius = (cur / seekBerMax) * RADIUS_MAX_IN_METERS;
                mapCircle.setRadius(radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Context context = getApplicationContext();
                showToast(context);
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context mContext = getApplicationContext();
            if (mContext.checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                                    MAP_PIN_LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapAPI = googleMap;
        initializeCenterAndCircle();
        mapAPI.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, 14.0f));
        mapAPI.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Context context = getApplicationContext();
                centerLatLng = marker.getPosition();
                mapCircle.setRadius(radius);
                mapCircle.setCenter(centerMarker.getPosition());
                showToast(context);
            }
        });
    }

    private void initializeCenterAndCircle() {
        centerMarker = mapAPI.addMarker(
                new MarkerOptions().position(centerLatLng).draggable(true).title(CENTER_MARKER));

        mapCircle = mapAPI.addCircle(
                new CircleOptions()
                        .center(centerMarker.getPosition())
                        .radius(radius)
                        .strokeWidth(3f)
                        .strokeColor(Color.RED)
                        .fillColor(Color.argb(70, 150, 50, 50))
        );
    }

    private void showToast(Context context) {
        int duration = Toast.LENGTH_LONG;
        double lat = centerMarker.getPosition().latitude;
        double lon = centerMarker.getPosition().longitude;
        String str = "lat: " + lat + "\nlong: " + lon + "\nradius: " + radius;
        Toast toast = Toast.makeText(context, str, duration);
        toast.show();
    }
}