package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class ConnectedActivity extends AppCompatActivity {
    static ConnectedActivity connectedActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        connectedActivity=this;
    }
    
    @Override
    public void onPause(){
        super.onPause();
    }


    @Override
    public void onResume(){
        super.onResume();
    }


    @Override
    public void onStop(){
        super.onStop();
    }


    public void disconnect(View view){
        MainActivity.wifiClient.disconnect(null);
        //event handler should handle the activity switch.
    }
}
