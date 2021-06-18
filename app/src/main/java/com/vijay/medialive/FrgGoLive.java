package com.vijay.medialive;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import org.jetbrains.annotations.NotNull;

public class FrgGoLive extends Fragment {

    View layout;
    final int REQUEST_CODE = 100;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable
                                     Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_go_live, container, false);

        Button button = layout.findViewById(R.id.btn_goLive);
        button.setOnClickListener(view -> {
            if (doAllPermissionGranted()) {
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                startActivity(intent);
            }
        });

        return layout;
    }


    boolean doAllPermissionGranted() {

        String storage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String camera = Manifest.permission.CAMERA;
        String microPhone = Manifest.permission.RECORD_AUDIO;

        int granted = PackageManager.PERMISSION_GRANTED;

        boolean isStorageGranted = ActivityCompat.
                checkSelfPermission(getContext(), storage) == granted;
        boolean isCameraGranted = ActivityCompat.
                checkSelfPermission(getContext(), storage) == granted;
        boolean isRecordGranted = ActivityCompat.
                checkSelfPermission(getContext(), storage) == granted;

        if (ContextCompat.checkSelfPermission(getContext(), storage) != granted)
            requestPermissions(new String[]{storage, camera, microPhone}, REQUEST_CODE);

        boolean temp = (isStorageGranted && isCameraGranted && isRecordGranted);
        if (temp) snack("Please grant storage permission");

        return temp;
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull @NotNull String[] permissions,
             @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (doAllPermissionGranted()) {
                snack("Granted Successfully! , Now you can use all features");
            } else {
                snack("Please grant Error: permission");
            }
        }
    }


    void snack(String message) {
        Snackbar snackbar = Snackbar
                .make(layout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

}