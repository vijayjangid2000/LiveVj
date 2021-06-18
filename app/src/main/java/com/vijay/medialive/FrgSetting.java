package com.vijay.medialive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class FrgSetting extends Fragment {

    TextInputLayout etlStreamURL, etlRtmpAddress, etlRtmpUsername, etlPassword, etlStreamKey;
    TextInputEditText etStreamUrl, etRtmpAddress, etRtmpUsername, etPassword, etStreamKey;
    View layout;
    Button btnSave;
    UserDetails userDetails;

    String rtmpHost, rtmpPassword, streamUrlForClient, rtmpKey, rtmpUsername;

    private void initViews() {
        etlStreamURL = layout.findViewById(R.id.etl_m3u8);
        etStreamUrl = layout.findViewById(R.id.et_m3u8);

        etlPassword = layout.findViewById(R.id.etl_rtmpPass);
        etPassword = layout.findViewById(R.id.et_rtmpPass);

        etRtmpAddress = layout.findViewById(R.id.et_rtmp);
        etlRtmpAddress = layout.findViewById(R.id.etl_rtmp);

        etlRtmpUsername = layout.findViewById(R.id.etl_UserName);
        etRtmpUsername = layout.findViewById(R.id.et_UserName);

        etStreamKey = layout.findViewById(R.id.et_StreamKey);
        etlStreamKey = layout.findViewById(R.id.etl_StreamKey);

        rtmpHost = userDetails.getStr_rtmpHostUrl();
        rtmpPassword = userDetails.getStr_rtmpPassword();
        streamUrlForClient = userDetails.getStr_streamUrlForClient();
        rtmpKey = userDetails.getStr_streamKey();
        rtmpUsername = userDetails.getStr_userName();

        etPassword.setText(rtmpPassword);
        etStreamUrl.setText(streamUrlForClient);
        etRtmpAddress.setText(rtmpHost);
        etRtmpUsername.setText(rtmpUsername);
        etStreamKey.setText(rtmpKey);

        btnSave = layout.findViewById(R.id.btn_saveDetail);
        btnSave.setOnClickListener(view -> saveDetails());
    }

    private void saveDetails() {

        if (getContext() == null) {
            showSnackBar("Null Context of Fragment");
            return;
        }

        rtmpHost = etRtmpAddress.getText().toString();
        rtmpKey = etStreamKey.getText().toString();
        rtmpUsername = etRtmpUsername.getText().toString();
        rtmpPassword = etPassword.getText().toString();
        streamUrlForClient = etStreamUrl.getText().toString();

        int l = 2;
        boolean invalid = rtmpHost.length() < l ||
                rtmpKey.length() < l || rtmpUsername.length() < l
                || rtmpPassword.length() < l || streamUrlForClient.length() < l;

        if (invalid) showSnackBar("Saved, Some fields are empty");
        else showSnackBar("Okay, Saved!");

        userDetails.setStr_streamUrlForClient(streamUrlForClient);
        userDetails.setStr_streamKey(rtmpKey);
        userDetails.setStr_rtmpHostUrl(rtmpHost);
        userDetails.setStr_userName(rtmpUsername);
        userDetails.setStr_rtmpPassword(rtmpPassword);

        userDetails.applyUpdate(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable
                                     Bundle savedInstanceState) {

        layout = inflater.inflate(R.layout.fragment_setting, container, false);
        userDetails = UserDetails.getInstance(getContext());
        initViews();

        return layout;
    }

    void showSnackBar(String message) {
        Snackbar snackbar = Snackbar
                .make(layout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

}
