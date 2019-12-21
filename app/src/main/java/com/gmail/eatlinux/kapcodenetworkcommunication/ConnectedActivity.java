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
    }
    
    @Override
    public void onPause(){
        disconnect(null);
        super.onPause();
    }


    @Override
    public void onResume(){
        super.onResume();
    }


    @Override
    public void onStop(){
        disconnect(null);
        super.onStop();
    }


    public void disconnect(View view){
        if(!disconnectCalled){
            disconnectCalled=true;
            if(MainActivity.wifiClient!=null){
                MainActivity.wifiClient.disconnect(null);
            }else{
                System.out.println("wifiClient is null?");
            }
            //event handler should handle the activity switch.
        }

    }
}
