package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiClient;
import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiScanner;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    final int port = 4006;
    static Handler handler;
    static MyWifiEventHandler eventHandler;
    static WifiClient wifiClient = null;
    View customIP_Layout, scannerProgressBar,inactiveText,connectButton;
    RadioGroup serverListRadioGroup = null;
    public static WifiScanner wifiScanner;
    static volatile AtomicBoolean startScanWhenWifiConnects,isVisible;
    static Thread wifiWatcherThread =null,userInteractionWatcherThread =null;
    static volatile long timeUserLastInteracted = 0;
    static final long userInteractionTimeout = 60000,userInteractionWatcherSleepTime = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        kapcodeNetworkOnCreate();
        /*
        todo when creating new app:
        todo change application name in strings.xml
        todo change port
        todo adjust timeouts for scanner, and wifiClient
         */
    }

    public void kapcodeNetworkOnCreate(){
        RelativeLayout rootLayout = findViewById(R.id.root_layout);//fade in rootLayout, avoid screen flash on start.
        if(rootLayout.getVisibility() == View.GONE){
            rootLayout.setAlpha(0);
            rootLayout.setVisibility(View.VISIBLE);
            rootLayout.animate().alpha(1.0f).setDuration(1000);
        }
        customIP_Layout = findViewById(R.id.custom_ip_layout_include);
        serverListRadioGroup = findViewById(R.id.server_list_radiogroup);
        connectButton=findViewById(R.id.connectButton);
        if(scannerProgressBar==null)scannerProgressBar=findViewById(R.id.sacnnerProgressBar);
        if(inactiveText==null)inactiveText=findViewById(R.id.inactiveText);
        if(handler==null) handler=new Handler();
        if(eventHandler==null) eventHandler= new MyWifiEventHandler(serverListRadioGroup,handler);
        eventHandler.setServerListRadioGroup(serverListRadioGroup);
        if(startScanWhenWifiConnects ==null)startScanWhenWifiConnects=new AtomicBoolean(false);
        if(isVisible==null)isVisible=new AtomicBoolean(true);
        setButtonEffect(findViewById(R.id.backButton1));
        setButtonEffect(findViewById(R.id.connectButton));
    }

    @Override
    public void onResume(){
        isVisible.set(true);
        connectButton.setEnabled(true);
        startScan();
        startWifiWatcher();
        onUserInteraction();
        startUserInactivityWatcher();
        super.onResume();
    }

    @Override
    public void onPause(){
        isVisible.set(false);
        wifiScanner.goal.set(WifiScanner.PAUSE);
        scannerProgressBar.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    public void onDestroy(){
        isVisible.set(false);
        wifiScanner.clearIdentifiedServersMap(eventHandler);
        super.onDestroy();
    }

    public synchronized void startScan(){
        if (wifiClient == null) { //if paused, un-pause
            if (wifiScanner != null) {
                wifiScanner.goal.set(WifiScanner.START);
                scannerProgressBar.setVisibility(View.VISIBLE);
            } else {//if null, create and start scan.
                boolean wifiIsConnected = false;
                try{
                    wifiIsConnected = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
                }catch (NullPointerException e){
                    //wifi is not present or not connected
                }
                if(wifiIsConnected){
                    wifiScanner= new WifiScanner(getDeviceAddress(), 3000,128, eventHandler);
                }else{
                    Toast.makeText(getApplicationContext(),"WIFI IS OFF: Please turn on wifi.",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void connectButtonOnClick(final View view) {
        view.setEnabled(false);

        //get selected radio button, parse info, connect...
        RadioButton rb = findViewById(serverListRadioGroup.getCheckedRadioButtonId());
        if (rb == null) {
            view.setEnabled(true);
        } else {
            if (rb == findViewById(R.id.custom_ip_radio_button)) {
                if (customIP_Layout.getVisibility() == View.VISIBLE) {
                    //try to connect with user input
                    String name = ((EditText) findViewById(R.id.user_input_server_name)).getText().toString();//todo assign var?
                    String ip = ((EditText) findViewById(R.id.user_input_server_ip)).getText().toString();
                    String port = ((EditText) findViewById(R.id.user_input_server_port)).getText().toString();
                    if (name != null & !name.isEmpty() & ip != null & !ip.isEmpty() & port != null & !port.isEmpty()) {
                        //try to connect
                        if (wifiClient == null) {
                            //pause scanner
                            wifiScanner.goal.set(WifiScanner.PAUSE);
                            scannerProgressBar.setVisibility(View.INVISIBLE);
                            wifiClient = new WifiClient(ip, Integer.parseInt(port), 10000, android.os.Build.MODEL, getResources().getString(R.string.app_name), false, eventHandler);
                        } else {
                            view.setEnabled(true);
                            toggleCustomIP_Layout();
                        }
                    }

                } else {
                    view.setEnabled(true);
                    toggleCustomIP_Layout();
                }


            } else if (rb == findViewById(R.id.usb_radio_button)) {//todo assign var
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
                    scannerProgressBar.setVisibility(View.INVISIBLE);
                    wifiClient = new WifiClient(ip, port, 5000, android.os.Build.MODEL, getResources().getString(R.string.app_name), false, eventHandler);
                }

            }
        }



    }

    public void backButtonOnClick(final View view){
        view.setEnabled(false);
        toggleCustomIP_Layout();
        view.setEnabled(true);
    }

    public static void setButtonEffect(final View button){
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("DOWN");
                        button.setAlpha(0.9f);
                        button.animate().alpha(0.5f).setDuration(100);
                        break;
                    case MotionEvent.ACTION_UP:
                        System.out.println("UP");
                        button.animate().alpha(1.0f).setDuration(100);
                        break;

                }
                return false;
            }
        });

    }

    public void toggleCustomIP_Layout(){
        if(customIP_Layout.getVisibility() == View.VISIBLE){
            customIP_Layout.setVisibility(View.GONE);
        }else{
            customIP_Layout.setVisibility(View.VISIBLE);
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
                                scannerProgressBar.setVisibility(View.INVISIBLE);
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

    @Override
    public void onUserInteraction(){
        timeUserLastInteracted = System.currentTimeMillis();

        if(isVisible.get()){//un-pause scanner,hide
            wifiScanner.goal.set(WifiScanner.START);
            scannerProgressBar.setVisibility(View.VISIBLE);
            inactiveText.setVisibility(View.GONE);
        }

    }

    public void startUserInactivityWatcher(){
        if(userInteractionWatcherThread==null){
            userInteractionWatcherThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {Thread.sleep(userInteractionWatcherSleepTime);}catch(InterruptedException e){e.printStackTrace();}
                        if(isVisible.get()&&(System.currentTimeMillis() - timeUserLastInteracted) > userInteractionTimeout){
                            wifiScanner.goal.set(WifiScanner.PAUSE);//pause scanner
                            if(findViewById(R.id.inactiveText).getVisibility() == View.GONE){
                                scannerProgressBar.setVisibility(View.INVISIBLE);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        inactiveText.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    }
                }
            });
            userInteractionWatcherThread.start();
        }
    }





}
