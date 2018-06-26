package com.geofencesample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private List<Geofence> geofences;
    private PendingIntent mGeofencePendingIntent;
    private Circle geoFenceLimits;
    private Marker currentLocationMarker;
    private Marker current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        addGeofenceClient();

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(geofenceReceiver, new IntentFilter("geofence"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(geofenceReceiver);
    }

    @SuppressLint("MissingPermission")
    private void addGeofenceClient() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("1")

                .setCircularRegion(
                        22.7533,
                        75.8937,
                        3000
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        geofences = new ArrayList<>();
        geofences.add(geofence);
        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        drawGeofenceCircle();
                        startLocationMonitor();
                        Toast.makeText(MainActivity.this, "Geofence added successfully", Toast.LENGTH_LONG).show();
                        // ...
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        // ...
                        Toast.makeText(MainActivity.this, "Soryy geofence can not add.", Toast.LENGTH_LONG).show();
                    }
                });

    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }


    @SuppressLint("MissingPermission")
    private void startLocationMonitor() {
        Log.d("MainActvitu", "start location monitor");
        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
                    if (current != null) {
                        current.setPosition(sydney);
                    } else {
                        current = mMap.addMarker(new MarkerOptions().position(sydney).title("current"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                        float zoom = 13f;
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(sydney, zoom);
                        mMap.animateCamera(cameraUpdate);
                    }
                    Log.d("LAST LOCATION", "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                }
            }
        }, null);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    /*    // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(22.7533, 75.8937);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Indore"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        float zoom = 13f;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(sydney, zoom);
        googleMap.animateCamera(cameraUpdate);*/
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addGeofenceClient();
        }
    }


    private void removeGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        // ...
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to remove geofences
                        // ...
                    }
                });
    }


    BroadcastReceiver geofenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra("type", -11);
            switch (type) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    Toast.makeText(MainActivity.this, "Enter", Toast.LENGTH_LONG).show();
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    Toast.makeText(MainActivity.this, "Exit", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    public void drawGeofenceCircle() {
        if (geoFenceLimits != null)
            geoFenceLimits.remove();
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(22.7533, 75.8937))
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(3000);
        geoFenceLimits = mMap.addCircle(circleOptions);
    }
}
