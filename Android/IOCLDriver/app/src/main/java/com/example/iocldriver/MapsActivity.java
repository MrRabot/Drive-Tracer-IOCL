package com.example.iocldriver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.internal.location.zzbz;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private final float GEOFENCE_RADIUS = 2000;
    private final LatLng iocl = new LatLng(26.2229, 91.9348);
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Checks for proper permissions and starts LocationService
        getPermissions();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().position(iocl).title("Marker"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(iocl, 14));
        
        //getting the users location
        enableUserLocation();
        
        //adding visual circle for geofence area
        addCircle(iocl, GEOFENCE_RADIUS);

        //adding Geofence

        
        //Initialize MapOnclickListener for our map
        mMap.setOnMapLongClickListener(this);
    }




    @SuppressLint("MissingPermission")
    public void enableUserLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mMap.setMyLocationEnabled(true);
            } else {
                //enable location
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    private boolean isLocationEnabled() {

        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return isGPSEnabled && isNetworkEnabled;
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void addCircle(LatLng latLng, float radius) {

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(200, 10, 150, 230));
        circleOptions.fillColor(Color.argb(100, 10, 150, 230));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }



    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
        addCircle(iocl, GEOFENCE_RADIUS);
        addMarker(latLng);
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Travel Marker");
        mMap.addMarker(markerOptions);
    }

    @Override
    protected void onDestroy() {

        stopService();
        super.onDestroy();
    }

    private void stopService(){
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
    }

    public void getPermissions(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.POST_NOTIFICATIONS}, 30);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        }
        else{
            startService();
        }
    }

    private void startService() {
        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case 10:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startService();
                }
                else {
                    Toast.makeText(this, "This app cannot function properly without location permission", Toast.LENGTH_LONG).show();
                }

            case 30:
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "This app cannot function without notification permission", Toast.LENGTH_LONG).show();
                }
        }
    }
}

