package com.example.week2module;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkActivity extends AppCompatActivity {

    TextView NetworkSSID;
    TextView NetworkRSSI;
    TextView NetworkFrequency;
    TextView Distance;
    Map<String, Object> networkData = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        NetworkSSID = findViewById(R.id.NetworkSSID);
        NetworkRSSI = findViewById(R.id.NetworkRSSI);
        NetworkFrequency = findViewById(R.id.NetworkFrequency);
        Distance = findViewById(R.id.distance);

        Intent intent = this.getIntent();

        if(intent != null){
            String SSID = intent.getStringExtra("SSID");
            String RSSI = intent.getStringExtra("RSSI");
          //  int Frequency = intent.getIntExtra("Frequency");
            String Frequency = intent.getStringExtra("Frequency");

            // String MacAddress = intent.getStringExtra("macAddress");


            NetworkSSID.setText(SSID);
            NetworkRSSI.setText(RSSI);
            NetworkFrequency.setText(Frequency);

           //Toast.makeText(NetworkActivity.this, MacAddress, Toast.LENGTH_SHORT).show();
            Distance.setText(Double.toString(calculateDistance(Double.parseDouble(RSSI), Double.parseDouble(Frequency))));


            getMacAddress();

            updateValue();




        }
    }

    private void updateValue() {
     //   Toast.makeText(NetworkActivity.this, networkData.get("macAddress").toString(), Toast.LENGTH_SHORT).show();

    }

    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);
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
                        //  newIntent.putExtra("macAddress", networkData.get("macAddress").toString());

                        //    networkData.put("GeoPoint", LatLng.getText().toString());

                    }
                    break;
                }
            }
         //   Mac_address.setText(macAddress);

        } catch (SocketException e){
            e.printStackTrace();
        }
    }
}