package com.example.week2module;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.opencensus.tags.Tag;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    boolean isPermissionGranted;
    GoogleMap mGoogleMap;

    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    ListAdapter listAdapter;
    ListView networkListView;
    List<ScanResult> wifiList;
    EditText editText;
    Button buttonClick;

    private FusedLocationProviderClient fusedLocationProviderClient;
    TextView Mac_address;
    TextView LatLng;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
     Map<String, Object> networkData = new HashMap<>();
    Double[] geoPoint = new Double[2];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Mac_address = findViewById(R.id.Mac_address);
        LatLng = findViewById(R.id.Latlng);
        networkListView = (ListView)findViewById(R.id.networkListView);

        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();

        editText = (EditText)findViewById(R.id.manualText);
        buttonClick = (Button)findViewById(R.id.click);

        buttonClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = editText.getText().toString();

                networkData.put("manualCoordinate", data);

                db.collection("WBIP").document(networkData.get("macAddress").toString()).set(networkData, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "Manual location Saved", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull  Exception e) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });








          checkMyPermission();

          initMap();
          fusedLocationProviderClient = new FusedLocationProviderClient(this);


          getMacAddress();

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        } else {
            ScanWifiList();
            getCurrentLocation();

            saveDataToFirebase();


        }


    }



    private void ScanWifiList() {
        wifiManager.startScan();
        wifiList = wifiManager.getScanResults();
        setAdapter();
    }

    private void setAdapter() {

        listAdapter = new ListAdapter(getApplicationContext(), wifiList);

        networkListView.setAdapter(listAdapter);
    }

    private void getMacAddress() {
        try{
            List<NetworkInterface> networkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            String macAddress = "";
            for(NetworkInterface networkInterface: networkInterfaceList){
                if(networkInterface.getName().equalsIgnoreCase("wlan0")){
                    for(int i = 0; i<networkInterface.getHardwareAddress().length;i++){
                        String stringMacByte = Integer.toHexString(networkInterface.getHardwareAddress()[i] & 0xFF);

                        if(stringMacByte.length() == 1){
                            stringMacByte = "0" + stringMacByte;
                        }
                        macAddress = macAddress + stringMacByte.toUpperCase() + ":";
                        networkData.put("macAddress", macAddress);
                    //    networkData.put("GeoPoint", LatLng.getText().toString());

                    }
                    break;
                }
            }
            Mac_address.setText(macAddress);

        } catch (SocketException e){
            e.printStackTrace();
        }
    }


    private void initMap() {
        if(isPermissionGranted){
            SupportMapFragment supportMapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            supportMapFragment.getMapAsync(this::onMapReady);

        }
    }
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Location location = task.getResult();
                gotoLocation(location.getLatitude(), location.getLongitude());



                LatLng.setText(location.getLatitude() + ", " + location.getLongitude());


//                geoPoint[0] = location.getLongitude();
//                geoPoint[1] = location.getLongitude();




            }
        });
    }

    private void gotoLocation(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
        mGoogleMap.moveCamera(cameraUpdate);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //GeoPoint geoPoint = new GeoPoint(latitude, longitude);
        networkData.put("GeoPoint", new GeoPoint(latLng.latitude, latLng.longitude));

//        wifiList = wifiManager.getScanResults();
//
//        System.out.println("this is wifiList");
//
//        System.out.println(wifiList);

        db.collection("WBIP").document(networkData.get("macAddress").toString()).set(networkData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(MainActivity.this, "Note Saved", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull  Exception e) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();

            }
        });

       // saveGeoToMap(latitude, longitude);

     //   networkData.put("GeoPoint", geoPoint);


        //     networkData.put("GeoPoint", new GeoPoint(latLng.latitude, latLng.longitude));
     //   System.out.println(networkData);

        // mGoogleMap.setMapStyle(GoogleMap.MAP_TYPE_NORMAL);
    }



    public static GeoPoint saveGeoToMap(double latitude, double longitude) {
        System.out.println("this is latitude"+ Double.toString(latitude));
        System.out.println("this is longitude"+ Double.toString(longitude));



        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        System.out.println("this is longitude"+ Double.toString(geoPoint.getLatitude()));
        System.out.println("this is longitude"+ Double.toString(geoPoint.getLongitude()));

       // networkData.put("GeoPoint", new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude()));

        double coordinate[] = new double[2];
        coordinate[0] = geoPoint.getLatitude();
        coordinate[1] = geoPoint.getLongitude();
        System.out.println("-----");
        System.out.println(coordinate);
        return geoPoint;
    }

    private void checkMyPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                isPermissionGranted = true;
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady( GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);

    }

    private void saveDataToFirebase() {
        System.out.println(networkData);
        System.out.println("-----");
      //  System.out.println(geoPoint[0]);

        //  Toast.makeText(MainActivity.this, "test save to firebase", Toast.LENGTH_SHORT).show();
        //Toast.makeText(MainActivity.this, networkData.get("GeoPoint").toString(), Toast.LENGTH_SHORT).show();

    }

    class WifiReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

}