package com.example.andrii.lostandfound;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.andrii.lostandfound.Utils.BitmapUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ItemFormActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String TAG = "ItemFormActivity";

    private DatabaseReference databaseReference;

    private ImageView formImageView;
    private EditText postTitle, userName, itemDescription, userPhoneNumber;
    private Uri uriItemImage;
    private Button saveButton, cancelButton, addPhotoFromGallery, takePhoto;

    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng itemLocation;
    private boolean hasPhoto =false;

    private static final float DEFAULT_ZOOM = 13f;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int GALLERY_REQUEST_CODE = 100;

    private String createTimeMillis;
    private String mCurrentPhotoPath;
    private String imageFileName;
    private byte[] mUploadBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_filling_form);

        formImageView = findViewById(R.id.iv_form_item_image);
        Picasso.with(this).load(R.drawable.ic_action_add_from_camera)
                .resizeDimen(R.dimen.iv_item_form_image_width, R.dimen.iv_item_form_image_height).centerCrop().into(formImageView);

        postTitle = findViewById(R.id.et_post_title);
        userName = findViewById(R.id.et_name);
        itemDescription = findViewById(R.id.et_description);
        userPhoneNumber = findViewById(R.id.et_phone_number);

        saveButton = findViewById(R.id.bt_save);
        cancelButton = findViewById(R.id.bt_cancel);
        addPhotoFromGallery = findViewById(R.id.bt_add_photo_from_gallery);
        takePhoto = findViewById(R.id.bt_take_photo);

        saveButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        addPhotoFromGallery.setOnClickListener(this);
        takePhoto.setOnClickListener(this);

        createTimeMillis = String.valueOf(System.currentTimeMillis());

        databaseReference = FirebaseDatabase.getInstance().getReference();
        getLocationPermission();
    }

    public void saveItemData(View view){
        String title = postTitle.getText().toString().trim();
        String name = userName.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        String phoneNumber = userPhoneNumber.getText().toString().trim();

        if(title.isEmpty()){
            postTitle.setError("Title required");
        }
        if(name.isEmpty()){
            userName.setError("Name required");
        }
        if(description.isEmpty()){
            postTitle.setError("Description required");
        }
        if(phoneNumber.isEmpty()){
            userPhoneNumber.setError("Phone number required");
        }
        if(itemLocation == null){
            Toast.makeText(this, "Please choose location!", Toast.LENGTH_SHORT).show();
        }
        if(hasPhoto == false){
            Toast.makeText(this, "Please choose photo!", Toast.LENGTH_SHORT).show();
        }

        if(postTitle.getError() != null || userName.getError() != null || postTitle.getError() != null
                || userPhoneNumber.getError() != null || itemLocation == null || hasPhoto == false){
            return;
        }

        ItemInformation itemInformation = new ItemInformation(createTimeMillis, title, name, description, phoneNumber, itemLocation.latitude, itemLocation.longitude);


        databaseReference.child("Items").child(createTimeMillis).setValue(itemInformation);

        Toast.makeText(this, "Information saved...", Toast.LENGTH_LONG).show();
        finish();
    }

    private void uploadImageToFirebaseStorage() throws IOException {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference itemImageReference = storage.getReference().child("itempics/" + createTimeMillis + ".jpg");

        itemImageReference.putBytes(mUploadBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
         //           itemImageURL = taskSnapshot.getMetadata().getDownloadUrl().toString();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ItemFormActivity.this, e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
    }

    public void openGallery(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Item Image"), GALLERY_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {

                uriItemImage = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriItemImage);
                setResult(RESULT_OK, takePictureIntent);
                hasPhoto = true;
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == GALLERY_REQUEST_CODE && data != null && data.getData() != null) {
            uriItemImage = data.getData();
            Uri selectedImage = data.getData();
            formImageView.setImageBitmap(getReadyImage(selectedImage));
            hasPhoto = true;
        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK  ) {
            formImageView.setImageBitmap(getReadyImage(uriItemImage));
        }

        BackgroundImageResize resize = new BackgroundImageResize(null);
        resize.execute(uriItemImage);

        super.onActivityResult(requestCode, resultCode, data);

    }

    private Bitmap getReadyImage(Uri uri){
        InputStream ims;
        Bitmap original = null;
        try {
            ims = getContentResolver().openInputStream(uri);
            original = BitmapFactory.decodeStream(ims);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "ItemFormActivity: " + e);
        }finally {
            original = BitmapUtils.cropCenterBitmap(original);
            original = BitmapUtils.rotateBitmap(original, 90);

        }
        return original;
    }

    private Bitmap compressImage(Uri selectedImage){
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap original = BitmapFactory.decodeStream(imageStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, 50, out);
        Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
        return decoded;
    }

    //view ????
    @Override
    public void onClick(View view) {
        if(view == saveButton){

            saveItemData(view);

        }
        if(view == cancelButton){
            finish();
        }
        if(view == addPhotoFromGallery){
            openGallery(view);
        }
        if(view == takePhoto){
            dispatchTakePictureIntent();
        }

    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.form_map);

        mapFragment.getMapAsync(ItemFormActivity.this);
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
                            Toast.makeText(ItemFormActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
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
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.clear();
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.addMarker(markerOptions);
                itemLocation = latLng;
            }
        });
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

    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]>{
        Bitmap mBitmap;

        public BackgroundImageResize(Bitmap mBitmap) {
            if(mBitmap != null){
                this.mBitmap = mBitmap;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected byte[] doInBackground(Uri... uris) {
            Log.d(TAG, "doInBackground: started");
            if(mBitmap == null){

                mBitmap = getReadyImage(uriItemImage);

                Log.e(TAG, "doInBackground: ");

            }
            byte[] bytes = null;
            Log.e(TAG, "doInBackground: megabytes before compression: " + mBitmap.getByteCount() / 1000000);
            bytes = BitmapUtils.getBytesFromBitmap(mBitmap, 10);
            Log.e(TAG, "doInBackground: megabytes before compression: " + bytes.length / 1000000);
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            mUploadBytes = bytes;
            try {
                uploadImageToFirebaseStorage();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}