package com.vijay.medialive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Zplayer extends AppCompatActivity {


    final int REQUEST_CODE = 100;

    PlayerView playerView;
    private SimpleExoPlayer player = null;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    EditText et_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);

        et_url = findViewById(R.id.et_url);
        playerView = findViewById(R.id.exoplayer);

        Button startUrl = findViewById(R.id.btn_startUrl);
        startUrl.setOnClickListener(view -> {
            if (doAllPermissionGranted()) {
                toast("Starting Now....");
                convertLinkToHls(et_url.getText().toString());
            }
        });

        Button btn_goLive = findViewById(R.id.btn_goLive);
        btn_goLive.setOnClickListener(view -> {
            Intent intent = new Intent(Zplayer.this, LiveActivity.class);
            startActivity(intent);
        });

        Button btn_mute = findViewById(R.id.btn_mute);
        btn_mute.setOnClickListener(view -> {
            if(player != null) {
                if(player.getVolume() != 0) {
                    player.setVolume(0);
                    btn_mute.setText("Unmute");
                }
                else {
                    player.setVolume(player.getDeviceVolume());
                    btn_mute.setText("Mute");
                }
            }
        });
    }

    void convertLinkToHls(String youtubeLiveLink) {

        final String[] hlsLink = {""};

        Runnable runnableCode = () -> {
            try {
                URL url = new URL(youtubeLiveLink);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();


                if(youtubeLiveLink.contains("m3u8")){
                    hlsLink[0] = youtubeLiveLink;
                }else{
                    hlsLink[0] = extractHlsUrl(content.toString());
                }
                con.disconnect();
            } catch (Exception e) {
                hlsLink[0] = youtubeLiveLink;
                e.printStackTrace();
                Log.d("TAG", "convertLinkToHls: " +
                        ": Reason is that the link is not a live url," +
                        "copy the link by open video then right click, copy video url from youtube.");
            }
            //extractHlsUrl(response);
        };

        Thread requestThread = new Thread(runnableCode);
        requestThread.start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!requestThread.isAlive()) {
                    // hlsLink[0]
                    runOnUiThread(() -> {
                        et_url.setText(hlsLink[0]);
                        initializePlayer();
                    });
                    timer.cancel();

                    /* Here initialize the player,
                     * make you do it in runOnUiTHread() */
                }
            }
        }, 1000, 1000);
    }

    private static String extractHlsUrl(String response) {

        String keyName = "hlsManifestUrl";

        if (response.contains(keyName)) {
            int index = response.indexOf(keyName);
            index = index + 17;

            int lastIndex = index;
            while (lastIndex < response.length()) {
                if ((response.charAt(lastIndex) == '8') &&
                        (response.charAt(lastIndex - 1) == 'u') &&
                        (response.charAt(lastIndex - 2) == '3') &&
                        (response.charAt(lastIndex - 3) == 'm')) {
                    break;
                }

                lastIndex++;
            }
            return response.substring(index, lastIndex + 1);
        }

        return null;
    }

    private void initializePlayer() {
        releasePlayer();
        player = new SimpleExoPlayer.Builder(this)
                .build();
        playerView.setPlayer(player);

        String tempUrl = et_url.getText().toString();

        // Create a data source factory.
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory();
        // Create a HLS media source pointing to a playlist uri.
        HlsMediaSource hlsMediaSource =
                new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(tempUrl));

        player.setMediaSource(hlsMediaSource);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare();
        player.play();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }

    boolean doAllPermissionGranted() {

        String storage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String camera = Manifest.permission.CAMERA;
        String microPhone = Manifest.permission.RECORD_AUDIO;

        int granted = PackageManager.PERMISSION_GRANTED;

        boolean isStorageGranted = ActivityCompat.checkSelfPermission(this, storage) == granted;

        if (ContextCompat.checkSelfPermission(this, storage) != granted)
            requestPermissions(new String[]{storage, camera, microPhone}, REQUEST_CODE);

        if (!isStorageGranted) toast("Please grant storage permission");

        return isStorageGranted;
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_CODE) {

            if (resultCode == RESULT_OK) {
                toast("Granted Successfully! , Now you can create poster");
            } else {
                toast("Please grant Error: permission");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}