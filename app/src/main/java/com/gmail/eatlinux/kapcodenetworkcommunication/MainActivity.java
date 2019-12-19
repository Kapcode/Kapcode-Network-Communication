package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiClient;
import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiScanner;

public class MainActivity extends AppCompatActivity {
    final int port = 4006;
    static Handler handler;
    static MyWifiEventHandler eventHandler;
    static Thread startScannerThread;
    static WifiClient wifiClient = null;
    View customIP_Layout;
    static RadioGroup serverListRadioGroup = null;
    static Button connectButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //remove any identified servers from list
        //synchronized, should block until destroy finishes clearing list.
        WifiScanner.getIdentifiedServersListSize();
        //load in views
        customIP_Layout = findViewById(R.id.custom_ip_layout_include);
        serverListRadioGroup = findViewById(R.id.server_list_radiogroup);
        connectButton=findViewById(R.id.connectButton);
        //create event handler if null
        if(handler==null){
            handler=new Handler();
            System.out.println("MADE NEW HANDLER");
        }
        //create eventHandler if null
        if(eventHandler==null){
            eventHandler= new MyWifiEventHandler(serverListRadioGroup,handler);
            System.out.println("MADE NEW EVNT HANDLER");
        }
        eventHandler.setServerListRadioGroup(serverListRadioGroup);


        System.out.println("CREATE");
    }


    public void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)

            // If Wi-Fi connected

            //if paused, un-pause
            if (wifiClient == null) {
                if (WifiScanner.executorService != null) {
                    WifiScanner.paused.set(false);
                } else {//if null, create and start scan.
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if(wifi.isConnected()){
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
                            }
                        }));
                        startScannerThread.start();
                    }else{
                        //toast
                        //todo set goal of scanner object to get ip and start


                        Toast.makeText(getApplicationContext(),"WIFI IS OFF: Turn on wifi, and restart app.",Toast.LENGTH_LONG).show();
                        this.finish();
                    }
                }
            } else {
                //make sure correct connection layout is visible.. mouse-pad, macro-pad,...where user left off, ... is connected.
                //do not scan.
            }


    }
    public void onResume(){
        findViewById(R.id.connectButton).setEnabled(true);
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
    @Override
    public void onDestroy(){
        //must clear list in memory, because the UI list will be cleared by super.onDestroy.
        WifiScanner.clearIdentifiedServersList();
        super.onDestroy();
    }



    //todo disable button after click, until action is complete!
    public void connectButtonOnClick(View view){
        view.setEnabled(false);
        //get selected radio button, parse info, connect...
        RadioButton rb = findViewById(((RadioGroup)findViewById(R.id.server_list_radiogroup)).getCheckedRadioButtonId());
        if(rb == findViewById(R.id.custom_ip_radio_button)){
            //todo make ui appear for entering ip:port...
            if(customIP_Layout.getVisibility() == View.VISIBLE){
                //try to connect with user input
                String name = ((EditText)findViewById(R.id.user_input_server_name)).getText().toString();
                String ip = ((EditText)findViewById(R.id.user_input_server_ip)).getText().toString();
                String port = ((EditText)findViewById(R.id.user_input_server_port)).getText().toString();
                if(name!=null &! name.isEmpty() & ip!=null &! ip.isEmpty() & port!=null &! port.isEmpty()){
                    //try to connect
                    if(wifiClient==null){
                        System.out.println("NULL");
                        //pause scanner
                        WifiScanner.paused.set(true);
                        //todo use application name from strings.xml
                        wifiClient = new WifiClient(ip,Integer.parseInt(port),3000,android.os.Build.MODEL,getResources().getString(R.string.app_name),false,eventHandler);
                    }else{System.out.println("NOT NULL");
                        view.setEnabled(true);
                        toggleCustomIP_Layout(null);
                    }
                }

            }else{
                view.setEnabled(true);
                toggleCustomIP_Layout(null);
            }



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
                wifiClient = new WifiClient(ip,port,3000,android.os.Build.MODEL,getResources().getString(R.string.app_name),false,eventHandler);
            }

        }

    }


    public void toggleCustomIP_Layout(View view){
        if(customIP_Layout.getVisibility() == View.VISIBLE){
            customIP_Layout.setVisibility(View.GONE);
        }else{
            customIP_Layout.setVisibility(View.VISIBLE);
        }
    }





}
