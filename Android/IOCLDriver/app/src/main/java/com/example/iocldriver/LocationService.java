package com.example.iocldriver;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class LocationService extends Service {

    private BroadcastReceiver br;

    private static final String TAG = "LocationService";

    String tripID;
    NotificationCompat.Builder notification;

    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    NotificationManager notificationManager;
    DatabaseReference databaseReference;
    FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng iocl = new LatLng(26.1828, 91.8038);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        String deviceName = Build.MODEL;

        databaseReference = FirebaseDatabase.getInstance().getReference(deviceName); //Database Reference URL Here

        startNotificationService();

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                getLocationLast();
                String[] arr1 = tripID.split("\\|", 0);
                String flag = arr1[2];
                if(flag.equals("OUT")){
                    tripID = getDateNTime()+"|IN";
                } else if (flag.equals("IN")) {
                    tripID = getDateNTime()+"|OUT";
                }
                getLocationLast();
            }
        };
        registerReceiver(br, new IntentFilter("IN"));
        getLocationLast();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        tripID = getDateNTime()+"|OUT";

        addGeofence(iocl, 2000);

        //initialize notification manager

        startLocationUpdates();

        return START_STICKY;
    }





    @SuppressLint("MissingPermission")
    private void addGeofence(LatLng latLng, float radius) {

        String GEOFENCE_ID = "";
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(unused -> Log.d(TAG, "onSuccess: Geofence Added"))
                .addOnFailureListener(e -> {
                    String errorMessage = geofenceHelper.getErrorString(e);
                    Log.d(TAG, "onFailure: "+errorMessage);
                });
    }

    /*public class broadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("IN")){
                tripID = getDateNTime()+"|IN";
            }
        }
    }*/



    @Override
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("onTaskRemoved called");
        super.onTaskRemoved(rootIntent);
        //do something you want
        //stop service
        stopForeground(true);
        this.stopSelf();
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(br);

        super.onDestroy();

        getLocationLast();

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    private void getLocationLast(){
        if(checkPermissions()){
            if(isLocationEnabled()){

                fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location==null){
                            startLocationUpdates();
                        }else{
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            //get current time
                            String[] arr1 = getDateNTime().split("\\|", 0);
                            String time = arr1[1];

                            //update notification

                            updateNotification(latitude + "\n" + longitude);

                            uploadToRTD(time, latitude + "$" + longitude);
                        }
                    }
                });
            }else{
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }
    }

    private boolean isLocationEnabled(){

        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return isGPSEnabled && isNetworkEnabled;
    }


    private boolean checkPermissions(){
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    protected void startNotificationService(){

        /*Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);*/

        notification = new NotificationCompat.Builder(this, "Channel_ID")
                .setContentTitle("Current Location...")
                .setContentText("Location: null")
                .setSmallIcon(R.drawable.ic_launcher_background)
                //.setContentIntent(pendingIntent)
                .setOngoing(true);

        startForeground(1, notification.build());
    }

    protected void updateNotification(String message){

        if(notificationManager==null){
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        NotificationCompat.Builder updatedNotification = notification.setContentText(message);

        notificationManager.notify(1,  updatedNotification.build());
    }

    protected void uploadToRTD(String time1, String location){

        HashMap<String, Object> map= new HashMap<>();
        map.put(time1, location);

        databaseReference.child(tripID).updateChildren(map);

    }

    protected String getDateNTime(){

        SimpleDateFormat simpleDateTime = new SimpleDateFormat("HH:mm:ss");

        SimpleDateFormat simpleDateDate = new SimpleDateFormat("dd:MM:yyyy");

        String time = simpleDateTime.format(new Date());

        String date = simpleDateDate.format(new Date());

        return date+"|"+time;

    }

    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(30000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        /* LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);*/

        if (!checkPermissions()) {

            Toast.makeText(this, "Mission Location permissions", Toast.LENGTH_LONG).show();
        }


        if(!isLocationEnabled()){
            Toast.makeText(this, "GPS is off. Please Turn on GPS", Toast.LENGTH_LONG).show();

        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    protected LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);

            //Get location data in lat and long

            double latitude = locationResult.getLastLocation().getLatitude();
            double longitude = locationResult.getLastLocation().getLongitude();

            //get current time
            String[] arr1 = getDateNTime().split("\\|", 0);
            String time = arr1[1];

            //update notification

            updateNotification(latitude+"\n"+longitude);

            //Upload to rt database

            uploadToRTD(time, latitude +"$"+ longitude);

        }
    };
}


