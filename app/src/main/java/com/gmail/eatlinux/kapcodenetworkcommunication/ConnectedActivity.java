package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class ConnectedActivity extends AppCompatActivity {
    boolean disconnectCalled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        MainActivity.eventHandler.activity = this;
        MainActivity.setButtonEffect(findViewById(R.id.disconnectButton));
    }
    
    @Override
    public void onPause(){
        disconnect();
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onStop(){
        disconnect();
        super.onStop();
    }

    public void disconnect(final View view){
        view.setEnabled(false);
        disconnect();
        view.setEnabled(true);
    }
    public void disconnect(){
        if(!disconnectCalled){
            disconnectCalled=true;
            if(MainActivity.wifiClient!=null){
                MainActivity.wifiClient.disconnect(null);
            }else{
                System.out.println("wifiClient is null?");
            }            //event handler should handle the activity switch.
        }
    }
}
