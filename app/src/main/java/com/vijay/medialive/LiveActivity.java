package com.vijay.medialive;

import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.pedro.rtplibrary.view.OpenGlView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera1Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera1}
 */
public class LiveActivity extends AppCompatActivity
        implements Button.OnClickListener, ConnectCheckerRtmp, SurfaceHolder.Callback,
        View.OnTouchListener {

    private Integer[] orientations = new Integer[]{0, 90, 180, 270};

    private RtmpCamera1 rtmpCamera1;
    private ImageButton btnGoLive, btnRecord, btnSwitchCamera, btnSettingRtmp;
    private String currentDateAndTime = "";
    private File folder;
    //options menu

    static int videoBitrate, fps, audioBitrate, sampleRate, indexInListResolution;
    static Boolean isEchoCancel, isNoiseSuppress, isStereo, isTcp;
    static List<String> listResolutions;

    private static String lastVideoBitrate;
    private TextView tvBitrate;
    private int totalDataUsed = 0;
    UserDetails userDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_live_streaming);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        userDetails = UserDetails.getInstance(this);

        folder = PathUtils.getRecordPath(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        OpenGlView openGlView = findViewById(R.id.surfaceView);
        openGlView.getHolder().addCallback(this);
        openGlView.setOnTouchListener(this);
        rtmpCamera1 = new RtmpCamera1(openGlView, this);
        prepareSettings();
        tvBitrate = findViewById(R.id.tv_bitrate);

        btnGoLive = findViewById(R.id.b_start_stop);
        btnGoLive.setOnClickListener(this);

        btnRecord = findViewById(R.id.b_record);
        btnRecord.setOnClickListener(this);

        btnSwitchCamera = findViewById(R.id.switch_camera);
        btnSwitchCamera.setOnClickListener(this);

        btnSettingRtmp = findViewById(R.id.rtmpSetting);
        btnSettingRtmp.setOnClickListener(this);
    }

    private void prepareSettings() {
        ArrayAdapter<Integer> orientationAdapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        orientationAdapter.addAll(orientations);

        listResolutions = new ArrayList<>();
        for (Camera.Size size : rtmpCamera1.getResolutionsBack())
            listResolutions.add(size.width + "X" + size.height);

        // default  values
        indexInListResolution = 0;
        videoBitrate = 2500;
        fps = 30;
        audioBitrate = 128;
        sampleRate = 44100;
        isEchoCancel = false;
        isNoiseSuppress = false;
        isStereo = true;
        isTcp = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.microphone:
                if (!rtmpCamera1.isAudioMuted()) {
                    item.setIcon(getResources().getDrawable(R.drawable.icon_microphone_off));
                    rtmpCamera1.disableAudio();
                } else {
                    item.setIcon(getResources().getDrawable(R.drawable.icon_microphone));
                    rtmpCamera1.enableAudio();
                }
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.b_start_stop:
                Log.d("TAG_R", "b_start_stop: ");
                if (!rtmpCamera1.isStreaming()) {
                    btnGoLive.setImageDrawable(getDrawable(R.drawable.exo_icon_stop));
                    String user = userDetails.getStr_userName();
                    String password = userDetails.getStr_rtmpPassword();
                    if (!user.isEmpty() && !password.isEmpty()) {
                        rtmpCamera1.setAuthorization(user, password);
                    }
                    if (rtmpCamera1.isRecording() || prepareEncoders()) {
                        rtmpCamera1.startStream(
                                userDetails.getStr_rtmpHostUrl() + "/" + userDetails.getStr_streamKey());
                    } else {
                        //If you see this all time when you start stream,
                        //it is because your encoder device dont support the configuration
                        //in video encoder maybe color format.
                        //If you have more encoder go to VideoEncoder or AudioEncoder class,
                        //change encoder and try
                        toast("Error preparing stream, This device cant do it"
                        );
                        btnGoLive.setImageDrawable(getDrawable(R.drawable.ic_baseline_live_tv_24));
                    }
                } else {
                    btnGoLive.setImageDrawable(getDrawable(R.drawable.ic_baseline_live_tv_24));
                    rtmpCamera1.stopStream();
                }
                break;

            case R.id.b_record:
                Log.d("TAG_R", "b_start_stop: ");
                totalDataUsed = 0;
                if (!rtmpCamera1.isRecording()) {
                    try {
                        if (!folder.exists()) {
                            folder.mkdir();
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                        currentDateAndTime = sdf.format(new Date());
                        if (!rtmpCamera1.isStreaming()) {
                            if (prepareEncoders()) {
                                rtmpCamera1.startRecord(
                                        folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                                btnRecord.setImageDrawable(getDrawable(R.drawable.ic_stop_24));
                                toast("Recording... ");
                            } else {
                                toast("Error preparing stream, This device cant do it");
                            }
                        } else {
                            rtmpCamera1.startRecord(
                                    folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                            btnRecord.setImageDrawable(getDrawable(R.drawable.ic_stop_24));
                            toast("Recording... ");
                        }
                    } catch (IOException e) {
                        rtmpCamera1.stopRecord();
                        btnRecord.setImageDrawable(getDrawable(R.drawable.ic_record_video));
                        toast(e.getMessage());
                    }
                } else {
                    rtmpCamera1.stopRecord();
                    btnRecord.setImageDrawable(getDrawable(R.drawable.ic_record_video));
                    toast(
                            "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath()
                    );
                    currentDateAndTime = "";
                }
                break;

            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (final CameraOpenException e) {
                    toast(e.getMessage());
                }
                break;

            case R.id.rtmpSetting:
                openBottomSheet();
                break;

            default:
                break;

        }
    }

    private boolean prepareEncoders() {
        Camera.Size resolution = rtmpCamera1.getResolutionsBack().get(indexInListResolution);
        int width = resolution.width, height = resolution.height;
        return rtmpCamera1.prepareVideo(width, height, fps, videoBitrate * 1024,
                CameraHelper.getCameraOrientation(this)) && rtmpCamera1.prepareAudio(
                audioBitrate * 1024, sampleRate, isStereo, isEchoCancel, isNoiseSuppress);
    }

    @Override
    public void onConnectionStartedRtmp(String rtmpUrl) {
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(() -> toast("Connection success"));
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(() -> {
            toast("Connection failed. " + reason);
            rtmpCamera1.stopStream();
            btnGoLive.setImageDrawable(getDrawable(R.drawable.ic_baseline_live_tv_24));
            if (rtmpCamera1.isRecording()) {
                rtmpCamera1.stopRecord();
                btnRecord.setImageDrawable(getDrawable(R.drawable.ic_record_video));
                snack("file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath());
                currentDateAndTime = "";
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(final long bitrate) {
        totalDataUsed += bitrate;
        String temp = (totalDataUsed / 8 / 1024 / 1024) + " MB";
        runOnUiThread(() -> tvBitrate.setText((bitrate / 8 / 1024) + " KB/sec\n" + temp));
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(() -> {
            toast("Disconnected");
            if (rtmpCamera1.isRecording()) {
                rtmpCamera1.stopRecord();
                btnRecord.setImageDrawable(getDrawable(R.drawable.ic_record_video));
                snack("file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath());
                currentDateAndTime = "";
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(() -> toast("Auth error"));
    }

    @Override
    public void onAuthSuccessRtmp() {
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
        // optionally:
        //rtmpCamera1.startPreview(CameraHelper.Facing.BACK);
        //or
        //rtmpCamera1.startPreview(CameraHelper.Facing.FRONT);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (rtmpCamera1.isRecording()) {
            rtmpCamera1.stopRecord();
            btnRecord.setImageDrawable(getDrawable(R.drawable.ic_record_video));
            snack("file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath());
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
            btnGoLive.setImageDrawable(getDrawable(R.drawable.ic_baseline_live_tv_24));
        }
        rtmpCamera1.stopPreview();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE) {
                rtmpCamera1.setZoom(motionEvent);
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            rtmpCamera1.tapToFocus(view, motionEvent);
        }
        return true;
    }

    void snack(String message) {
        Snackbar snackbar = Snackbar.make(tvBitrate, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Drawable drawable(int id) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), id, null);
        return drawable;
    }

    void openBottomSheet() {
        SheetClass sheetClass = new SheetClass();
        sheetClass.show(getSupportFragmentManager(), "exampleBottomSheet");
        if (lastVideoBitrate != null &&
                !lastVideoBitrate.equals(videoBitrate) && rtmpCamera1.isStreaming()) {
            int bitrate = videoBitrate * 1024;
            rtmpCamera1.setVideoBitrateOnFly(bitrate);
            toast("New bitrate: " + bitrate);
        }
    }

    public static class SheetClass extends BottomSheetDialogFragment {

        private View view;
        private Spinner spnResolution;
        private EditText etFps, etSampleRate, etBitrateVideo, etBitrateAudio;
        private CheckBox cbEchoCanceller, cbNoiseSuppressor;
        private RadioButton rbMono;
        private Button btnSave;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            view = inflater.inflate(R.layout.bottom_sheet, container, false);
            initViews();
            return view;
        }

        void initViews() {
            spnResolution = view.findViewById(R.id.spnResolution);
            btnSave = view.findViewById(R.id.saveStreamDetails);
            etFps = view.findViewById(R.id.et_fpsVideo);
            etSampleRate = view.findViewById(R.id.et_sampleRateAudio);
            etBitrateAudio = view.findViewById(R.id.et_bitrateAudio);
            etBitrateVideo = view.findViewById(R.id.et_bitrateVideo);
            cbEchoCanceller = view.findViewById(R.id.cb_EchoCancel);
            cbNoiseSuppressor = view.findViewById(R.id.cb_NoiseSuppress);
            rbMono = view.findViewById(R.id.rbMonoChannel);

            etFps.setText(fps + "");
            etSampleRate.setText(sampleRate + "");
            etBitrateAudio.setText(audioBitrate + "");
            etBitrateVideo.setText(videoBitrate + "");

            ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>
                    (getContext(), R.layout.support_simple_spinner_dropdown_item);
            resolutionAdapter.addAll(listResolutions);
            spnResolution.setAdapter(resolutionAdapter);

            lastVideoBitrate = String.valueOf(videoBitrate);

            btnSave.setOnClickListener(view -> {

            });
        }

        @Override
        public void dismiss() {
            super.dismiss();

        }

        void applyData() {
            indexInListResolution = spnResolution.getSelectedItemPosition();
            videoBitrate = Integer.parseInt(etBitrateVideo.getText().toString());
            fps = Integer.parseInt(etFps.getText().toString());
            audioBitrate = Integer.parseInt(etBitrateAudio.getText().toString());
            sampleRate = Integer.parseInt(etSampleRate.getText().toString());
            isEchoCancel = cbEchoCanceller.isChecked();
            isNoiseSuppress = cbNoiseSuppressor.isChecked();
            isStereo = rbMono.isChecked();

            spnResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    indexInListResolution = i;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    indexInListResolution = 0;
                }
            });

            if (audioBitrate < 64 || audioBitrate > 512) audioBitrate = 128;
            if (videoBitrate < 320 || videoBitrate > 30000) videoBitrate = 2500;
            if (fps < 20 || fps > 120) fps = 30;
            if (sampleRate < 20000 || sampleRate > 200000) sampleRate = 44100;

            Toast.makeText(getContext(), "Saved Successfully", Toast.LENGTH_SHORT).show();
        }
    }
}
