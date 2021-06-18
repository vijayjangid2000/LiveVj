package com.vijay.medialive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class FrgWatch extends Fragment {

    final int REQUEST_CODE = 100;
    PlayerView playerView;
    private SimpleExoPlayer player = null;
    private boolean playWhenReady = false;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    View layout;
    UserDetails userDetails;
    ImageButton btnMute, btnFullScreen;
    Button btnPlay;

    private void initViews() {

        userDetails = UserDetails.getInstance(getContext());

        playerView = layout.findViewById(R.id.exoplayer);

        btnPlay = layout.findViewById(R.id.btn_startUrl);
        btnPlay.setOnClickListener(view -> {
                if (player != null) {
                    if (!player.isPlaying()) {
                        player.play();
                        snack("Playing Now!");
                        btnPlay.setText("Release");
                    } else {
                        releasePlayer(); // running play release
                        snack("Player released");
                        btnPlay.setText("Load Video");
                        playerView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    snack("Starting liveStream...");
                    String temp = userDetails.getStr_streamUrlForClient();
                    if (temp == null) snack("Please set Streaming URL in Settings");
                    else if (temp.length() < 2) snack("Please set Streaming URL in Settings");
                    else convertLinkToHls();

                }

        });

        btnMute = layout.findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(view -> {
            if (player != null) {
                if (player.getVolume() != 0) {
                    player.setVolume(0);
                    btnMute.setImageDrawable(drawable(R.drawable.ic_mute_24));
                } else {
                    player.setVolume(player.getDeviceVolume());
                    btnMute.setImageDrawable(drawable(R.drawable.ic_unmute_24));
                }
            } else {
                snack("Play a video first!");
            }
        });

        btnFullScreen = layout.findViewById(R.id.btn_fullScreen);
        btnFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (player != null) {
                    // do full screen
                } else {
                    snack("Play a video first!");
                }

            }
        });


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable
                                     Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_watch,
                container, false);
        initViews();
        return layout;
    }

    void snack(String message) {
        Snackbar snackbar = Snackbar
                .make(layout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    void convertLinkToHls() {

        String youtubeLiveLink = UserDetails.getInstance
                (getContext()).getStr_streamUrlForClient();

        // we call POST from url and extract the manifestUrl

        final String[] hlsLink = {""};

        Runnable runnableCode = () -> {
            try {
                if (youtubeLiveLink.contains("m3u8")) {
                    hlsLink[0] = youtubeLiveLink;
                    return;
                }
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
                hlsLink[0] = extractHlsUrl(content.toString());

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
                    getActivity().runOnUiThread(() -> {
                        userDetails.setStr_streamUrlForClient(hlsLink[0]);
                        userDetails.applyUpdate(getContext());
                        initializePlayer();
                    });
                    timer.cancel();

                    /* Here initialize the player,
                     * make sure you do it in runOnUiThread() */
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
        player = new SimpleExoPlayer.Builder(getContext())
                .build();
        playerView.setPlayer(player);

        // Create a data source factory.
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory();
        // Create a HLS media source pointing to a playlist uri.
        HlsMediaSource hlsMediaSource =
                new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem
                                .fromUri(userDetails.getStr_streamUrlForClient()));

        player.setMediaSource(hlsMediaSource);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) btnPlay.setText("Release");
                else btnPlay.setText("Play Video");
            }
        });


        playerView.setVisibility(View.VISIBLE);
        player.prepare();

    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            convertLinkToHls();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            convertLinkToHls();
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
        if (Util.SDK_INT >= 24) releasePlayer();
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

    private Drawable drawable(int id) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), id, null);
        return drawable;
    }
}
