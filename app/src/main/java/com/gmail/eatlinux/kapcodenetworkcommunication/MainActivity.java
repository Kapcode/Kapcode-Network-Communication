package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final static WifiEventHandler eventHandler = new WifiEventHandler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startScan();
    }
    public void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)
        new Thread((new Runnable() {
            @Override
            public void run() {
                //start a scanner... blocks! (scans all ip's on network except this_system_ip, ) //
                WifiScanner.startScanningIpRange("192.168.0.1",4006,127,"AndroidClient1ScannersName","demo",3000,eventHandler,true);
                //get first found server, connect to it with new client
                //ArrayList<Object[]> listOfServers = WifiScanner.getCopyOfIdentifiedServersList();
                //Object[] serverInfo = listOfServers.get(0);
                //final String server_ip = (String)serverInfo[1];



                //create clients, handshake runs on new thread... so non blocking...
                // WifiClient client1 = new WifiClient(server_ip,4006,WifiClient.DEFAULT_TIMEOUT,"Android_client1","demo",false,eventHandler);
            }
        })).start();
    }
}
