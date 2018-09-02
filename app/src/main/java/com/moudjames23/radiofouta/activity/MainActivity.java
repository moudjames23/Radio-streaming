package com.moudjames23.radiofouta.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.moudjames23.radiofouta.Constant;
import com.moudjames23.radiofouta.R;
import com.moudjames23.radiofouta.service.RadioService;

public class MainActivity extends AppCompatActivity {


    private RadioService mRadioService;
    private boolean boundService;

    private ImageView mPlayPause;

    private boolean rotateRunning = false;

    private BroadcastReceiver receiver;

    private ServiceConnection mConnexion = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            RadioService.RadioBinder binder = (RadioService.RadioBinder) service;
            mRadioService = binder.getService();

            if(mRadioService.isPlaying())
            {
                boundService = true;
                mRadioService.hideNotification();
                mPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
            }
            else
                mPlayPause.setImageResource(R.drawable.ic_play_arrow_white_24dp);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            boundService = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
        

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constant.FAILED_PLAY_RADIO))
                {
                    if(rotateRunning)
                    {
                        mPlayPause.clearAnimation();
                        mPlayPause.setImageResource(R.drawable.ic_play_arrow_white_24dp);

                        rotateRunning = false;
                        Log.d("RADIO_DEBUG", "STOP ANIM");
                    }
                }
            }
        };
    }

    private void init()
    {
        mPlayPause = (ImageView)findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mRadioService.isPlaying())
                {
                    mRadioService.pause();
                    mPlayPause.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                }
                else
                {
                    if(isOnline())
                    {
                        if (!mRadioService.isLoading()) {
                            if (mRadioService.isLoaded()) {
                                mPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
                                mPlayPause.clearAnimation();
                            } else {
                                mPlayPause.setImageResource(R.drawable.ic_loading_spinner);

                                // rotation to use for loading icon
                                RotateAnimation  rotate = new RotateAnimation(0, 360,
                                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                        0.5f);

                                rotate.setDuration(1000);
                                rotate.setRepeatCount(Animation.INFINITE);
                                rotate.setInterpolator(new LinearInterpolator());
                                rotate.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        rotateRunning = true;
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        rotateRunning = false;
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });

                                mPlayPause.startAnimation(rotate);
                            }
                            mRadioService.setRunOnStreamPrepared(new Runnable() {
                                @Override
                                public void run() {
                                    mPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
                                    mPlayPause.clearAnimation();
                                }
                            });
                            mRadioService.playRadio();
                        }

                    }
                    else
                        Toast.makeText(MainActivity.this, "Veuillez vous connectez à internet SVP", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        startActivity(new Intent(this, AboutActivity.class));

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, RadioService.class);
        startService(intent);
        bindService(intent, mConnexion, BIND_AUTO_CREATE);

        boundService = true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if(mRadioService != null)
        {
            if(rotateRunning)
                mPlayPause.clearAnimation();

            if(mRadioService.isPlaying())
                mPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
            else
                mPlayPause.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constant.FAILED_PLAY_RADIO));

    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mRadioService != null)
        {
            if(boundService && mRadioService.isPlaying())
                mRadioService.showNotification();
        }

        unbindService(mConnexion);
        boundService = false;
    }



    public boolean isOnline()
    {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        return info != null && info.isConnectedOrConnecting();
    }
}
