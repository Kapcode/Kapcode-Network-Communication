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

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    final int port = 4006;
    static Handler handler;
    static MyWifiEventHandler eventHandler;
    static WifiClient wifiClient = null;
    View customIP_Layout;
    static RadioGroup serverListRadioGroup = null;
    static Button connectButton;
    public static WifiScanner wifiScanner;
    static AtomicBoolean startScanWhenWifiConnects;
    static Thread wifiWatcherThread =null;
    static volatile AtomicBoolean isVisible;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        customIP_Layout = findViewById(R.id.custom_ip_layout_include);
        serverListRadioGroup = findViewById(R.id.server_list_radiogroup);
        connectButton=findViewById(R.id.connectButton);
        if(handler==null){
            handler=new Handler();
        }
        if(eventHandler==null){
            eventHandler= new MyWifiEventHandler(serverListRadioGroup,handler);
        }
        eventHandler.setServerListRadioGroup(serverListRadioGroup);
        if(startScanWhenWifiConnects ==null)startScanWhenWifiConnects=new AtomicBoolean(false);
        if(isVisible==null)isVisible=new AtomicBoolean(true);
    }


    public synchronized void startScan(){
        //new Thread // don't block UI thread. (Scanner process blocks.)
        // If Wi-Fi connected
        //if paused, un-pause
        if (wifiClient == null) {
            if (wifiScanner != null) {
                //un-pause
                wifiScanner.goal.set(WifiScanner.START);
            } else {//if null, create and start scan.
                boolean wifiIsConnected = false;

                try{
                    wifiIsConnected = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
                }catch (NullPointerException e){
                    //wifi is assumed to be not connected, so scanner will not start.
                    //or wifi not present.
                }

                if(wifiIsConnected){
                    wifiScanner= new WifiScanner(getDeviceAddress(), 3000,128, eventHandler);
                }else{
                    //toast
                    Toast.makeText(getApplicationContext(),"WIFI IS OFF: Please turn on wifi.",Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            //make sure correct connection layout is visible.. mouse-pad, macro-pad,...where user left off, ... is connected.
            //do not scan.
        }


    }
    public void onResume(){
        isVisible.set(true);
        findViewById(R.id.connectButton).setEnabled(true);
        startScan();
        startWifiWatcher();
        super.onResume();
    }
    public void onPause(){
        isVisible.set(false);
        wifiScanner.goal.set(WifiScanner.PAUSE);
        super.onPause();
    }
    public void onStop(){
        super.onStop();
    }
    @Override
    public void onDestroy(){
        //must clear list in memory, because the UI list will be cleared by super.onDestroy.
        isVisible.set(false);
        //wifiScanner.clearIdentifiedServersList();
        wifiScanner.clearIdentifiedServersMap(eventHandler);
        super.onDestroy();
    }



    public void connectButtonOnClick(View view) {
        view.setEnabled(false);
        //get selected radio button, parse info, connect...
        RadioButton rb = findViewById(((RadioGroup) findViewById(R.id.server_list_radiogroup)).getCheckedRadioButtonId());
        if (rb == null) {
            view.setEnabled(true);
        } else {
            if (rb == findViewById(R.id.custom_ip_radio_button)) {
                if (customIP_Layout.getVisibility() == View.VISIBLE) {
                    //try to connect with user input
                    String name = ((EditText) findViewById(R.id.user_input_server_name)).getText().toString();
                    String ip = ((EditText) findViewById(R.id.user_input_server_ip)).getText().toString();
                    String port = ((EditText) findViewById(R.id.user_input_server_port)).getText().toString();
                    if (name != null & !name.isEmpty() & ip != null & !ip.isEmpty() & port != null & !port.isEmpty()) {
                        //try to connect
                        if (wifiClient == null) {
                            //pause scanner
                            wifiScanner.goal.set(WifiScanner.PAUSE);
                            wifiClient = new WifiClient(ip, Integer.parseInt(port), 10000, android.os.Build.MODEL, getResources().getString(R.string.app_name), false, eventHandler);
                        } else {
                            view.setEnabled(true);
                            toggleCustomIP_Layout(null);
                        }
                    }

                } else {
                    view.setEnabled(true);
                    toggleCustomIP_Layout(null);
                }


            } else if (rb == findViewById(R.id.usb_radio_button)) {
                connectButton.setEnabled(true);
                //todo make usb information appear
            } else if (rb.getText().toString().contains(":" + port)) {
                //connect to selected server
                String[] textParts = rb.getText().toString().split(":");
                //String serverName = textParts[0];
                String ip = textParts[1];
                //port is in array too, but above if statement needs it to be this.port
                System.out.println(rb.getText().toString());
                if (wifiClient == null) {
                    //pause scanner
                    wifiScanner.goal.set(WifiScanner.PAUSE);
                    wifiClient = new WifiClient(ip, port, 5000, android.os.Build.MODEL, getResources().getString(R.string.app_name), false, eventHandler);
                }

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


    //watches wifi connectivity state.

    //if was connected, and just disconnected -> pause scanner, clear the IdentifiedServersList
    //if was disconnected, and just connected -> setDeviceAddress, and un-pause scanner
    public void startWifiWatcher(){// watches wifi
        if(wifiWatcherThread==null){

            wifiWatcherThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    boolean lastIsConnected = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
                    //check if connection changes
                    while(true){
                        boolean wifiIsConnected = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
                        //if was disconnected, and just connected, and MainActivity is visible, start scan
                        if(!lastIsConnected && wifiIsConnected && isVisible.get()){
                            //if scanner has already been created, must set device address in-case it has changed
                            if(wifiScanner!=null)wifiScanner.setDeviceAddress(getDeviceAddress());
                            startScan();
                        }else if(lastIsConnected &! wifiIsConnected){
                            //if just disconnected, pause scanner, clear list
                            if(wifiScanner!=null){
                                wifiScanner.goal.set(WifiScanner.PAUSE);
                                //wifiScanner.clearIdentifiedServersList();
                                wifiScanner.clearIdentifiedServersMap(eventHandler);
                            }
                        }
                        lastIsConnected=wifiIsConnected;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }



                }
            });
            wifiWatcherThread.start();
        }

    }

    //concat system name, application name, wifi ip address, port
    public String getDeviceAddress(){
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        String name =android.os.Build.MODEL.replace(":","");
        String application = getResources().getString(R.string.app_name).replace(":","");
        return name+":"+application+":"+ip+":"+port;
    }




}
