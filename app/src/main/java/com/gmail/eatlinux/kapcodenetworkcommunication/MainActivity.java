package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    final static WifiEventHandler eventHandler = new WifiEventHandler();
    static Thread startScannerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)
        startScannerThread= new Thread((new Runnable() {
            @Override
            public void run() {
                try{
                    WifiScanner.startScanningIpRange("192.168.0.1",4006,127,"AndroidClient1ScannersName","demo",3000,eventHandler,true);
                }catch (java.util.concurrent.RejectedExecutionException e){
                    e.printStackTrace();
                }
                //get first found server, connect to it with new client
                        //ArrayList<Object[]> listOfServers = WifiScanner.getCopyOfIdentifiedServersList();
                        //Object[] serverInfo = listOfServers.get(0);
                        //final String server_ip = (String)serverInfo[1];

                                            //create clients, handshake runs on new thread... so non blocking...
                                            // WifiClient client1 = new WifiClient(server_ip,4006,WifiClient.DEFAULT_TIMEOUT,"Android_client1","demo",false,eventHandler);
            }
        }));
        //if paused, un-pause
        if(WifiScanner.executorService!=null){
            WifiScanner.paused.set(false);
        }else{//if null, create and start scan.
            startScannerThread.start();
        }

    }
    public void onResume(){
        startScan();
        super.onResume();
    }
    public void onPause(){
        WifiScanner.paused.set(true);
        super.onPause();
    }
    public void onStop(){
        super.onStop();
    }



    public void connectButtonOnClick(View view){
        //get selected radio button, parse info, connect...
    }
}
