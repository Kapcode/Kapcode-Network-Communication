package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiClient;
import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiScanner;

public class MainActivity extends AppCompatActivity {
    final int port = 4006;
    final Handler handler=new Handler();
    MyWifiEventHandler eventHandler;
    static Thread startScannerThread;
    static WifiClient wifiClient = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //create event handler if null
         if(eventHandler==null)eventHandler= new MyWifiEventHandler((RadioGroup)findViewById(R.id.server_list_radiogroup),handler);
    }


    public void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo ethernet = connManager .getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        if (wifi.isConnected() || ethernet.isConnected()) {
            // If Wi-Fi connected

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            final String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            startScannerThread = new Thread((new Runnable() {
                @Override
                public void run() {
                    try {
                        WifiScanner.startScanningIpRange(ip, port, 127, android.os.Build.MODEL + "scanner", getResources().getString(R.string.app_name), 3000, eventHandler, true);
                    } catch (java.util.concurrent.RejectedExecutionException e) {
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
            if (wifiClient == null) {
                if (WifiScanner.executorService != null) {
                    WifiScanner.paused.set(false);
                } else {//if null, create and start scan.
                    startScannerThread.start();
                }
            } else {
                //make sure correct connection layout is visible.. mouse-pad, macro-pad,...where user left off, ... is connected.
                //do not scan.
            }
        }else{
            //todo not connected to wifi or ethernet.... tell user
            System.out.println("todo not connected to wifi or ethernet.... tell user, CALLING FINISH!");
            this.finish();
            //listen for wifi to be connected..
            //startScan()
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
        RadioButton rb = findViewById(((RadioGroup)findViewById(R.id.server_list_radiogroup)).getCheckedRadioButtonId());
        if(rb == findViewById(R.id.custom_ip_radio_button)){
            //todo make ui appear for entering ip:port...
        }else if(rb == findViewById(R.id.usb_radio_button)){
            //todo make usb information appear
        }else if(rb.getText().toString().contains(":"+port)){
            //connect to selected server
            String[] textParts = rb.getText().toString().split(":");
            //String serverName = textParts[0];
            String ip = textParts[1];
            //port is in array too, but above if statement needs it to be this.port
            System.out.println(rb.getText().toString());

            if(wifiClient==null){
                //pause scanner
                WifiScanner.paused.set(true);
                //todo use application name from strings.xml
                wifiClient = new WifiClient(ip,port,3000,android.os.Build.MODEL,"demo",false,eventHandler);
            }

        }
    }


}
