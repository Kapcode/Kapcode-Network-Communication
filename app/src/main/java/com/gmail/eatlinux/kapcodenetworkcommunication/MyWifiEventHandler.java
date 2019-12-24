package com.gmail.eatlinux.kapcodenetworkcommunication;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiClient;
import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiEventHandler;
import com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal.WifiScanner;

public class MyWifiEventHandler extends WifiEventHandler {
    RadioGroup serverListRadioGroup;
    Handler handler;
    Activity activity;
    public MyWifiEventHandler(RadioGroup serverListRadioGroup, Handler handler){
        this.serverListRadioGroup = serverListRadioGroup;
        this.handler=handler;
    }
    // needs to be called if MainActivity or the RadioGroup gets destroyed, so in MainActivity on create.
    public void setServerListRadioGroup(RadioGroup radioGroup){
        serverListRadioGroup = radioGroup;
    }

    @Override
    public void scannerFoundServer(final WifiClient client){
        super.scannerFoundServer(client);
        handler.post(new Runnable(){public void run(){
            String serverName = client.serverName;
            String ip = client.ip;
            int port = client.port;
            RadioButton rb = new RadioButton(serverListRadioGroup.getContext());
            rb.setText(serverName+":"+ip+":"+port);
            rb.setTextColor(serverListRadioGroup.getResources().getColor(R.color.textDark));//todo make adhere to current theme.
            serverListRadioGroup.addView(rb);
        }});
    }

    @Override
    //todo could cause concurrency issues2 on a large list, could also block too long on ui thread, might want to synchronize found and remove... both actions need to be synchronized,,, all access to the radioGroup.
    public void scannerLostServer(String ip,int port){
        super.scannerLostServer(ip,port);
        final String textToLookFor = ip+":"+port;
        handler.post(new Runnable(){public void run(){
            int index = 0;
            while(index<serverListRadioGroup.getChildCount()){
                RadioButton rb = (RadioButton) serverListRadioGroup.getChildAt(index);
                if(rb.getText().toString().contains(textToLookFor)){
                    serverListRadioGroup.removeView(rb);
                    break;
                }
                index++;
            }
        }});
    }

    @Override
    public void clientDisconnected(WifiClient client, Exception[] exceptions){
        super.clientDisconnected(client,exceptions);
        //set null, because connection is now dead.
        MainActivity.wifiClient=null;
        //un-pause scanner on client disconnected, only if not a ping.
        if(!client.ping){
            //finish connectedActivity, go back to MainActivity.
            if(activity!=null)activity.finish();
            //un-pause scanner
            MainActivity.wifiScanner.goal.set(WifiScanner.START);
        }
    }


    @Override
    public void clientHandshakeSuccessful(WifiClient client){
        super.clientHandshakeSuccessful(client);
        if(!client.ping){
            //go to ConnectedActivity
            Intent myIntent = new Intent(serverListRadioGroup.getContext(), ConnectedActivity.class);
            serverListRadioGroup.getContext().startActivity(myIntent);
        }
    }

    @Override
    public void clientFailedToConnect(WifiClient client,Exception e){

        super.clientFailedToConnect(client,e);

        //set null, because connection is now dead.

        //connect button needs to be enabled.
        //if not ping, finish ConnectedActivity, and enable connectButton -> on failing to connect,un-pause scanner
        if(!client.ping)handler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.wifiClient=null;
                //activity.findViewById(R.id.connectButton).setEnabled(true);
                if(activity!=null)activity.finish();
                MainActivity.wifiScanner.goal.set(WifiScanner.START);

            }
        });


    }


}
