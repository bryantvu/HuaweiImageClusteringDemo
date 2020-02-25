package com.bvutest.hms.picturemap.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MotionEventCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.media.ExifInterface;
import android.net.LinkAddress;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;
import com.huawei.hms.maps.util.LogM;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, HuaweiMap.OnCameraMoveStartedListener{

    private static final String TAG = "MainActivity";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final int REQUEST_CODE = 1;

    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET
    };

    private HuaweiMap hMap;
    private MapView mMapView;
    private int currRoundPosition = 6;

    private ArrayList<PictureMarker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogM.d(TAG, "onCreate:hzj");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = findViewById(R.id.mapView);

        //check permissions first
        if (!hasPermissions(this, RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE);
        }

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(HuaweiMap map) {
        Log.d(TAG, "onMapReady");

        //get map instance in a callback method
        hMap = map;

        //set map UI settings
        hMap.setMyLocationEnabled(true);
        hMap.getUiSettings().setMyLocationButtonEnabled(true);
        hMap.getUiSettings().setTiltGesturesEnabled(false);
        hMap.getUiSettings().setRotateGesturesEnabled(false);
        hMap.getUiSettings().setZoomControlsEnabled(false);

        //set location to Santa Clara, CA USA
        LatLng latLng1 = new LatLng(37.373100, -121.963856);

        float tilt = 0.0f;
        float bearing = 0.0f;
        float zoom = 15.0f;

        //create camera position for location
        CameraPosition cameraPosition = new CameraPosition(latLng1, zoom, tilt, bearing);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);

        //move camera to new position
        hMap.animateCamera(cameraUpdate);

        //init listener to detect touch gestures on map
        hMap.setOnCameraMoveStartedListener(this);

        //load images from storage, extract GPS from EXIF, and put marker assets into memory
        loadMarkers();
        //load marker assets from memory onto map
        addMarkerToMap();
    }

    //load images from storage, extract GPS from EXIF, and put marker assets into memory
    public void loadMarkers(){

        //get directory
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/PictureMap";
        File f = new File(path);
        File[] files = f.listFiles();

        if(files!=null){
            for (int i = 0; i < files.length; i++) {

                String filename = files[i].getName();
                String filepath = path+"/"+ filename;
                try{
                    //get GPS from EXIF
                    ExifInterface exif = new ExifInterface(filepath);
                    float[] latLong = new float[2];
                    if(exif.getLatLong(latLong)){
                        //add new PictureMarker to memory
                        addNewMarker(filepath, latLong[0], latLong[1]);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }else{
            Log.d("Files", "Could not get files");
        }

    }

    //add new PictureMarker to memory
    public void addNewMarker(String path, double lati, double longi){
        Log.d(TAG, "addMarker path >> "+path);

        try{
            //read image from storage path
            FileInputStream in = new FileInputStream(path);
            BufferedInputStream buf = new BufferedInputStream(in);
            byte[] bMapArray= new byte[buf.available()];
            buf.read(bMapArray);

            //create bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);
            //resize bitmap
            bmp = getResizedBitmap(bmp, 200);
            //crop bitmap to square
            bmp = cropToSquare(bmp);

            //add new PictureMarker to ArrayList markers
            markers.add(new PictureMarker(bmp, lati, longi));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //clear existing markers and replace markers on map
    public void addMarkerToMap() {
        hMap.clear();
        try{
            for(PictureMarker marker:markers){

                //psuedo-image clustering is done via rounding latitude and longitude
                double lati = round(marker.lati,currRoundPosition);
                double longi = round(marker.longi, currRoundPosition);

                MarkerOptions options = new MarkerOptions()
                        .position(new LatLng(lati, longi))
                        .icon(BitmapDescriptorFactory.fromBitmap(marker.image));
                hMap.addMarker(options);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //resize bitmap to input parameter
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    //crop bitmap to square
    public static Bitmap cropToSquare(Bitmap bitmap){
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0)? 0: cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0)? 0: cropH;
        Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

        return cropImg;
    }

    //round value to decimal position, for psuedo-image clustering
    private static double round(double value, int places) {
        if(places ==-2){
            return 10*Math.ceil(value/10.0);
        }else if (places < 0) return (double)((int)value);

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    //triggered on touch gesture from user
    //define rounding factor here based on current zoom position
    @Override
    public void onCameraMoveStarted(int reason) {
        float zoom = hMap.getCameraPosition().zoom;
        LogM.i(TAG, "onCameraMove >> zoom >> " + zoom);
        int oldRoundPosition = currRoundPosition;
        int newRoundPosition;

        //higher zoom value = more zoomed in
        //less zoom, more image clustering
        if(zoom < 1.5f){
            newRoundPosition = -2;
        }else if(zoom < 3){
            newRoundPosition = -1;
        }else if(zoom <4.5f){
            newRoundPosition = 1;
        }else if(zoom <9.5f){
            newRoundPosition=2;
        }else if(zoom >15.5f){
            newRoundPosition=6;
        }else{
            newRoundPosition=3;
        }
        currRoundPosition = newRoundPosition;
        //update markers if rounding factor changes
        if(newRoundPosition!=oldRoundPosition){
            addMarkerToMap();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }
    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "Permission >> size: "+permissions);
        for(int i = 0; i<permissions.length; i++){
            Log.d(TAG, "Permission >> " + permissions[i] + " = " + grantResults[i]);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission >> "+ permission + " = not granted" );
                    return false;
                }else{
                    Log.d(TAG, "Permission >> "+ permission + " = granted" );
                }
            }
        }else{
            Log.d(TAG, "Permission >> checking conditions not met");
        }
        return true;
    }


}
