package com.bunkerpalace.cordova;

import android.content.Intent;
import android.widget.ImageButton;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.view.MotionEvent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Application;
import android.content.res.Resources;
import android.content.Context;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle;
import com.google.android.youtube.player.YouTubePlayer.Provider;


public class YouTubeActivity extends YouTubeBaseActivity implements OnInitializedListener,
        PlayerStateChangeListener, View.OnClickListener {

    private static final int RECOVERY_REQUEST = 1;
    private YouTubePlayerView youTubeView;
    private String videoId;
    private String apiKey;

    private YouTubePlayer mPlayer;

    private View mPlayButtonLayout;
    private TextView mPlayTimeTextView;

    private Handler mHandler = null;
    private SeekBar mSeekBar;

    private static Context mContext;
    
    ImageButton playBtn;
    ImageButton pauseBtn;
    
    boolean gone = false;
    boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Intent intent = getIntent();
        videoId = intent.getStringExtra("videoId");
        apiKey = intent.getStringExtra("YouTubeApiId");
        // youTubeView = new YouTubePlayerView(this);
        // youTubeView.initialize(apiKey, this);
        //setContentView(youTubeView);
        Application app = getApplication();
        String package_name = app.getPackageName();
        Resources resources = app.getResources();
        int pllayout = resources.getIdentifier("activity_custom_player", "layout", package_name);
        setContentView(pllayout);

        YouTubePlayerView youTubePlayerView = (YouTubePlayerView) findViewById(resources.getIdentifier("youtube_player_view", "id", package_name));
        youTubePlayerView.initialize(apiKey, this);

        mPlayButtonLayout = findViewById(resources.getIdentifier("video_control", "id", package_name));
        // findViewById(resources.getIdentifier("play_video", "id", package_name)).setOnClickListener(this);
        // findViewById(resources.getIdentifier("pause_video", "id", package_name)).setOnClickListener(this);

        playBtn = (ImageButton) findViewById(resources.getIdentifier("play_video", "id", package_name));
        pauseBtn = (ImageButton) findViewById(resources.getIdentifier("pause_video", "id", package_name));
        playBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        playBtn.setVisibility(ImageButton.VISIBLE);
        pauseBtn.setVisibility(ImageButton.GONE);

        mPlayTimeTextView = (TextView) findViewById(resources.getIdentifier("play_time", "id", package_name));
        mSeekBar = (SeekBar) findViewById(resources.getIdentifier("video_seekbar", "id", package_name));
        mSeekBar.setOnSeekBarChangeListener(mVideoSeekBarChangeListener);

        mHandler = new Handler();

        // LinearLayout ln = (LinearLayout)findViewById(resources.getIdentifier("youtube_player_wrapper", "id", package_name));

        // ln.setOnTouchListener(new View.OnTouchListener() {
        //     @Override
        //     public boolean onTouch(View view, MotionEvent motionEvent) {
        //         if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
        //             if(!gone){
        //                 mPlayButtonLayout.setVisibility(View.GONE);
        //                 gone = true;
        //             }else{
        //                 mPlayButtonLayout.setVisibility(View.VISIBLE);
        //                 gone = false;

        //             }


        //         }
        //         return true;
        //     }
        // });

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        // if (!wasRestored) {
        //     player.loadVideo(videoId);
        //     player.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS);
        //     Toast.makeText(this, "Player Started", Toast.LENGTH_LONG).show();
        //     //player.setPlayerStateChangeListener(this);
        // }
        //player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
        if (null == player) return;
        mPlayer = player;

        displayCurrentTime();

        // Start buffering
        if (!wasRestored) {
            player.cueVideo(videoId);
        }

        player.setPlayerStyle(PlayerStyle.CHROMELESS);
        mPlayButtonLayout.setVisibility(View.VISIBLE);
        //playBtn.performClick();
        // Add listeners to YouTubePlayer instance
        player.setPlayerStateChangeListener(mPlayerStateChangeListener);
        player.setPlaybackEventListener(mPlaybackEventListener);
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_REQUEST).show();
        } else {
            String error = String.format("Error initializing YouTube player", errorReason.toString());
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onVideoEnded() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onError(
            com.google.android.youtube.player.YouTubePlayer.ErrorReason arg0) {
        updateLog("onError(): " + arg0.toString());
        finish();
    }

    @Override
    public void onAdStarted() {}

    @Override
    public void onLoaded(String arg0) {}

    @Override
    public void onLoading() {}

    @Override
    public void onVideoStarted() {}

    private void updateLog(String text){
        //Log.d("YouTubeActivity", text);
    };

    PlaybackEventListener mPlaybackEventListener = new PlaybackEventListener() {
        @Override
        public void onBuffering(boolean arg0) {
        }

        @Override
        public void onPaused() {
            mHandler.removeCallbacks(runnable);
        }

        @Override
        public void onPlaying() {
            mHandler.postDelayed(runnable, 100);
            displayCurrentTime();
            //mSeekBar.setProgress(mPlayer.getCurrentTimeMillis()/100);
        }

        @Override
        public void onSeekTo(int arg0) {
            mHandler.postDelayed(runnable, 100);
        }

        @Override
        public void onStopped() {
            mHandler.removeCallbacks(runnable);
        }
    };

    PlayerStateChangeListener mPlayerStateChangeListener = new PlayerStateChangeListener() {
        @Override
        public void onAdStarted() {
        }

        @Override
        public void onError(ErrorReason arg0) {
        }

        @Override
        public void onLoaded(String arg0) {
        }

        @Override
        public void onLoading() {
        }

        @Override
        public void onVideoEnded() {
        }

        @Override
        public void onVideoStarted() {
            displayCurrentTime();
        }
    };

    SeekBar.OnSeekBarChangeListener mVideoSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){
                long lengthPlayed = (mPlayer.getDurationMillis() * progress) / 100;
                mPlayer.seekToMillis((int) lengthPlayed);
                if(lengthPlayed >= (mPlayer.getDurationMillis() - 2000)){
                    mPlayer.seekToMillis((int) 0);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    public void onClick(View v) {
        Application app = getApplication();
        String package_name = app.getPackageName();
        Resources resources = app.getResources();
        
        if(isPlaying){
            mPlayer.pause();
            playBtn.setVisibility(ImageButton.VISIBLE);
            pauseBtn.setVisibility(ImageButton.GONE);
            isPlaying = false;
        } else {
            mPlayer.play();
            playBtn.setVisibility(ImageButton.GONE);
            pauseBtn.setVisibility(ImageButton.VISIBLE);
            isPlaying = true;
        }
//        switch (v.getId()) {
//            case resources.getIdentifier("play_video", "id", package_name):
//                if (null != mPlayer && !mPlayer.isPlaying())
//                    mPlayer.play();
//                break;
//            case resources.getIdentifier("pause_video", "id", package_name):
//                if (null != mPlayer && mPlayer.isPlaying())
//                    mPlayer.pause();
//                break;
//        }
    }

    private void displayCurrentTime() {
        if (null == mPlayer) return;
        String formattedTime = formatTime(mPlayer.getDurationMillis() - mPlayer.getCurrentTimeMillis());
        mPlayTimeTextView.setText(formattedTime);
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        return (hours == 0 ? "--:" : hours + ":") + String.format("%02d:%02d", minutes % 60, seconds % 60);
    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            displayCurrentTime();
            int progress = mPlayer.getCurrentTimeMillis() * 100 / mPlayer.getDurationMillis();
            long lengthPlayed = (mPlayer.getDurationMillis() * progress) / 100;
            // mPlayer.seekToMillis((int) lengthPlayed);
            if(lengthPlayed >= (mPlayer.getDurationMillis() - 2000)){
                mPlayer.pause();
                mPlayer.seekToMillis((int) 0);
            }
            mSeekBar.setProgress(progress);
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
