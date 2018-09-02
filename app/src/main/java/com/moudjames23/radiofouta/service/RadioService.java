package com.moudjames23.radiofouta.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;



import com.moudjames23.radiofouta.Constant;
import com.moudjames23.radiofouta.R;
import com.moudjames23.radiofouta.activity.MainActivity;

import java.io.IOException;

/**
 * Created by Moudjames23 on 5/1/2016.
 */
public class RadioService extends Service {

    public MediaPlayer mediaPlayer;

    private boolean isLoaded;
    private boolean isLoading;

    private Runnable runOnStreamPrepared;

    private IBinder mBinder = new RadioBinder();


    public class RadioBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupMediaPlayer();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constant.RADIO_ACTION_PLAY_PAUSE:

                    if (isPlaying()) {
                        pause();
                        showNotification();
                        stopForeground(false);
                    } else {
                        playRadio();
                        showNotification();
                    }

                    break;

                case Constant.RADIO_ACTION_STOP:

                    hideNotification();
                    stopService();

                    break;


                default:
                    break;
            }
        }


        return super.onStartCommand(intent, flags, startId);
    }


    public void prepStreamAndPlay()
    {
        if(mediaPlayer != null)
        {
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    isLoading = false;
                    isLoaded = true;

                    playRadio();

                    if(runOnStreamPrepared != null)
                        runOnStreamPrepared.run();
                }
            });

            try
            {
                mediaPlayer.setDataSource(Constant.STREAM_URL);
                isLoading = true;
                mediaPlayer.prepareAsync();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            setupMediaPlayer();
            prepStreamAndPlay();
        }
    }




    public void playRadio()
    {
        if(isLoaded)
            mediaPlayer.start();
        else
            prepStreamAndPlay();

    }

    public void setupMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                Toast.makeText(RadioService.this, "Une erreur s'est produite", Toast.LENGTH_LONG).show();
                Log.d("RADIO_DEBUG", "ERROR");
                LocalBroadcastManager.getInstance(RadioService.this).sendBroadcast(new Intent(Constant.FAILED_PLAY_RADIO));
                return false;
            }
        });

    }



    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    public boolean isPlaying() {
        return mediaPlayer != null && (mediaPlayer.isPlaying() || isLoading);
    }

    public boolean isLoading()
    {
        return isLoading;
    }

    public boolean isLoaded()
    {
        return isLoaded;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void showNotification() {

        Intent intentPlayPause = new Intent(this, RadioService.class);
        intentPlayPause.setAction(Constant.RADIO_ACTION_PLAY_PAUSE);

        PendingIntent pendingPlayPause = PendingIntent.getService(this, 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentClose = new Intent(this, RadioService.class);
        intentClose.setAction(Constant.RADIO_ACTION_STOP);

        PendingIntent pendingIntentClose = PendingIntent.getService(this, 0, intentClose, PendingIntent.FLAG_ONE_SHOT);

        Intent intentOpen = new Intent(this, MainActivity.class);
        intentOpen.putExtra("isPlaying", isPlaying());

        PendingIntent pendingLaunch = PendingIntent.getActivity(this, 0, intentOpen, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.slogan))
                .setSmallIcon(isPlaying() ? R.drawable.ic_play_arrow_white_24dp : R.drawable.ic_pause_white_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setShowWhen(false)
                .addAction(isPlaying() ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp,
                        "PLAY/PAUSE", pendingPlayPause)
                .addAction(R.drawable.ic_close_white_24dp, "ARRETER", pendingIntentClose)
                .setDeleteIntent(pendingIntentClose)
                .setContentIntent(pendingLaunch)
                .build();

        startForeground(2254, notification);
    }

    public void hideNotification() {
        stopForeground(true);
    }


    public void stopService() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            stopSelf();
        }
    }

    public void setRunOnStreamPrepared(final Runnable runOnStreamPrepared) {
        this.runOnStreamPrepared = runOnStreamPrepared;
    }
}
