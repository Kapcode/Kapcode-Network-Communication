package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final static WifiEventHandler eventHandler = new WifiEventHandler();
    static int debugCounter = 0;
    static Thread startScannerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)
        debugCounter++;
        startScannerThread= new Thread((new Runnable() {
            @Override
            public void run() {

                //shutdown scanner and wait for finish
                //WifiScanner.shutDownNow();
                //start a scanner... blocks! (scans all ip's on network except this_system_ip, ) //
                try{
                    WifiScanner.startScanningIpRange(debugCounter,"192.168.0.1",4006,127,"AndroidClient1ScannersName","demo",3000,eventHandler,true);


                }catch (java.util.concurrent.RejectedExecutionException e){

                    System.out.println("oops");
                    startScan();

                }


                //get first found server, connect to it with new client
                        //ArrayList<Object[]> listOfServers = WifiScanner.getCopyOfIdentifiedServersList();
                        //Object[] serverInfo = listOfServers.get(0);
                        //final String server_ip = (String)serverInfo[1];

                                            //create clients, handshake runs on new thread... so non blocking...
                                            // WifiClient client1 = new WifiClient(server_ip,4006,WifiClient.DEFAULT_TIMEOUT,"Android_client1","demo",false,eventHandler);
            }
        }));
        WifiScanner.waitForShutdown();
        startScannerThread.start();
    }
    public void onResume(){
        //todo still getting oops..above print out java.util.concurrent.RejectedExecutionException
        startScan();
        super.onResume();
    }
    public void onPause(){
        //System.out.println("STOPPING");
        WifiScanner.shutDownNow();
        //System.out.println("Stopped");
        super.onPause();
    }
    public void onStop(){
        //WifiScanner.shutDownNow();
        super.onStop();
    }
}
