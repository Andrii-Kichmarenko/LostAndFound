package com.example.andrii.lostandfound;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    private static final String TAG = "MapsActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 13f;

    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private FloatingActionButton fab;

    private DatabaseReference databaseItems;
    private StorageReference mStorageRef;
    private File file;
    public static Map<Marker, ItemInformation> markerMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        databaseItems = FirebaseDatabase.getInstance().getReference("Items");
        mStorageRef = FirebaseStorage.getInstance().getReference();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        markerMap = new HashMap<>();

        checkGPSandNetworkProviders();
        getLocationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseItems.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot artistSnapshot : dataSnapshot.getChildren()){
                    ItemInformation item = artistSnapshot.getValue(ItemInformation.class);
                    if(!markerMap.containsValue(item) && item != null){
                        Log.d(TAG, "databaseItems onDataChange: itemId = " + item.getId());
                        downladFile(item.getId());
                        Log.d(TAG, "databaseItems onDataChange: file = " + file );
                        item.setImageFile(file);
                        Log.d(TAG, "databaseItems onDataChange: item key: " + artistSnapshot.getKey() );
                           try{
                               String snippet = "Description: " + item.getDescription() + "\n" +
                                       "Name: " + item.getName() + "\n" +
                                       "Phone Number: " + item.getPhoneNumber() + "\n";
                               MarkerOptions options = new MarkerOptions()
                                       .position(new LatLng(item.getLatitude(), item.getLongitude()))
                                       .title(item.getTitle()).snippet(snippet);

                               markerMap.put(mMap.addMarker(options), item);
                               Log.d(TAG, "onDataChange: Load Title:" + item.getTitle() + ". Description: " + item.getDescription());

                           }catch (NullPointerException e){
                               Log.e(TAG, "databaseItems onDataChange: NullPointerException: " + e.getMessage() );
                           }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "databaseItems onCancelled: " + databaseError.getMessage() );
            }
        });
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }


    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM);
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        mMap.setOnMarkerClickListener(this);
        composeFAB();
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void checkGPSandNetworkProviders(){
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(this.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(this.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    System.exit(0);
                }
            });
            dialog.show();
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        ItemInformation tmpItemInformation = markerMap.get(marker);

        if(tmpItemInformation != null){
            Log.d(TAG, "onMarkerClick: lat: " + tmpItemInformation.getLatitude() + ", lng: " + tmpItemInformation.getLongitude() );
            moveCamera(new LatLng(tmpItemInformation.getLatitude(), tmpItemInformation.getLongitude()));
            marker.setTag(tmpItemInformation);
            marker.showInfoWindow();
            return true;
        }
        Log.d(TAG, "onMarkerClick: null = markerMap.get(marker)" );
        return false;
    }

    private void moveCamera(LatLng latLng){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));
    }

    private void downladFile(String id){
        StorageReference riversRef = mStorageRef.child("itempics/" + id + ".jpg");
        try {
            file = File.createTempFile("image_" + id + "_", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        riversRef.getFile(file)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "downloadFile: Success " + file.getAbsolutePath());
                    }

                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "downloadFile: Failure " + file.getAbsolutePath());
            }
        });
    }

    public static ItemInformation getItemInfotmation(Marker marker){
        return markerMap.get(marker);
    }

    private void composeFAB(){
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, ItemFormActivity.class);
                startActivity(intent);
            }
        });
    }
}