package com.gmail.eatlinux.kapcodenetworkcommunication;

import android.os.Handler;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MyWifiEventHandler extends WifiEventHandler {
    RadioGroup serverListRadioGroup;
    Handler handler;
    public MyWifiEventHandler(RadioGroup serverListRadioGroup, Handler handler){
        this.serverListRadioGroup = serverListRadioGroup;
        this.handler=handler;
    }

    @Override
    public void scannerFoundServer(final WifiClient client){
        super.scannerFoundServer(client);
        //todo add to ui
        handler.post(new Runnable(){public void run(){
            String serverName = client.serverName;
            String ip = client.ip;
            int port = client.port;
            RadioButton rb = new RadioButton(serverListRadioGroup.getContext());
            rb.setText(serverName+":"+ip+":"+port);
            serverListRadioGroup.addView(rb);
        }});
    }

    @Override
    //todo could cause concurrency issues2 on a large list, could also block too long on ui thread, might want to syncronize found and remove... both actions need to be synchronized,,, all access to the radioGroup.
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

    public void clientDisconnected(WifiClient client, Exception[] exceptions){
        super.clientDisconnected(client,exceptions);
        //set null, because connection is now dead.
        MainActivity.wifiClient=null;
        //un-pause scanner on client disconnected, only if not a ping.
        if(!client.ping) WifiScanner.paused.set(false);
    }


}
