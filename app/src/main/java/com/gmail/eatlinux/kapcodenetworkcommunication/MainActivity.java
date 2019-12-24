package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
    View customIP_Layout;
    static RadioGroup serverListRadioGroup = null;
    static Button connectButton;
    public static WifiScanner wifiScanner;
    static volatile AtomicBoolean startScanWhenWifiConnects,isVisible;
    static Thread wifiWatcherThread =null;
    static Thread userInteractionWatcherThread =null;
    static volatile long timeUserLastInteracted = 0;
    static final long userInteractionTimeout = 60000;//will be considered inactive after 60 seconds
    static final long userInteractionWatcherSleepTime = 10000;//check every 10 seconds
    @Override
    public void onUserInteraction(){
        timeUserLastInteracted = System.currentTimeMillis();
        //un-pause scanner
        if(isVisible.get()){
            wifiScanner.goal.set(WifiScanner.START);
            findViewById(R.id.sacnnerProgressBar).setVisibility(View.VISIBLE);
            findViewById(R.id.inactiveText).setVisibility(View.GONE);
        }

    }
    public void checkForUserInactivity(){
        if(userInteractionWatcherThread==null){
            userInteractionWatcherThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {
                            Thread.sleep(userInteractionWatcherSleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(isVisible.get()&&(System.currentTimeMillis() - timeUserLastInteracted) > userInteractionTimeout){
                            System.out.println("IN ACTIVE");
                            //pause scanner
                            wifiScanner.goal.set(WifiScanner.PAUSE);
                            if(findViewById(R.id.inactiveText).getVisibility() == View.GONE){
                                findViewById(R.id.sacnnerProgressBar).setVisibility(View.INVISIBLE);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.inactiveText).setVisibility(View.VISIBLE);
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
        //fade in/out rootLayout
        RelativeLayout rootLayout = findViewById(R.id.root_layout);
        if(rootLayout.getVisibility() == View.GONE){
            rootLayout.setAlpha(0);
            rootLayout.setVisibility(View.VISIBLE);
            rootLayout.animate().alpha(1.0f).setDuration(1000);
        }
        customIP_Layout = findViewById(R.id.custom_ip_layout_include);
        serverListRadioGroup = findViewById(R.id.server_list_radiogroup);
        connectButton=findViewById(R.id.connectButton);
        if(handler==null) handler=new Handler();
        if(eventHandler==null) eventHandler= new MyWifiEventHandler(serverListRadioGroup,handler);
        eventHandler.setServerListRadioGroup(serverListRadioGroup);
        if(startScanWhenWifiConnects ==null)startScanWhenWifiConnects=new AtomicBoolean(false);
        if(isVisible==null)isVisible=new AtomicBoolean(true);
        setButtonEffect(findViewById(R.id.backButton1));
        setButtonEffect(findViewById(R.id.connectButton));
        checkForUserInactivity();
    }

    public void onResume(){
        isVisible.set(true);
        findViewById(R.id.connectButton).setEnabled(true);
        startScan();
        startWifiWatcher();
        onUserInteraction();
        super.onResume();
    }
    public void onPause(){
        isVisible.set(false);
        wifiScanner.goal.set(WifiScanner.PAUSE);
        findViewById(R.id.sacnnerProgressBar).setVisibility(View.INVISIBLE);
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
                findViewById(R.id.sacnnerProgressBar).setVisibility(View.VISIBLE);
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
                            findViewById(R.id.sacnnerProgressBar).setVisibility(View.INVISIBLE);
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
                    findViewById(R.id.sacnnerProgressBar).setVisibility(View.INVISIBLE);
                    wifiClient = new WifiClient(ip, port, 5000, android.os.Build.MODEL, getResources().getString(R.string.app_name), false, eventHandler);
                }

            }
        }



    }


    public void toggleCustomIP_Layout(){
        if(customIP_Layout.getVisibility() == View.VISIBLE){
            customIP_Layout.setVisibility(View.GONE);
        }else{
            customIP_Layout.setVisibility(View.VISIBLE);
        }
    }

    public void backButtonOnClick(final View view){
        view.setEnabled(false);
        toggleCustomIP_Layout();
        view.setEnabled(true);
    }



    //user inactivity check is in this thread.
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
                                findViewById(R.id.sacnnerProgressBar).setVisibility(View.INVISIBLE);
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

}
