package com.gmail.eatlinux.kapcodenetworkcommunication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

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
        AlphaAnimation alphaAnimation = MainActivity.buttonAnimation(view);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                disconnect();
                view.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(alphaAnimation);
    }
    public void disconnect(){
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
