package org.me.gcu.g_fadda_honours_project_23_24;


import android.net.Uri;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;


public class CaptureFragment extends Fragment {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ImageCapture imageCapture;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startCameraX();
            } else {
                Toast.makeText(getContext(), "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        previewView = view.findViewById(R.id.previewView);
        Button captureButton = view.findViewById(R.id.bt_CapturePhoto); // Corrected button ID

        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        checkCameraPermission();

        captureButton.setOnClickListener(view1 -> capturePhoto()); // Added listener for capture button

        return view;


    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraX();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCameraX() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();

                // Initialize ImageCapture
                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Bind use cases to camera
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void capturePhoto() {
        File photoFile = new File(getOutputDirectory(), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                getActivity().runOnUiThread(() -> {
                    ImageView imageView = getView().findViewById(R.id.imageView);
                    imageView.setImageURI(Uri.fromFile(photoFile));

                    imageView.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("imagePath", photoFile.getAbsolutePath());
                        ResultFragment resultFragment = new ResultFragment();
                        resultFragment.setArguments(bundle);

                        // Assuming you have a method like this in your activity to handle fragment replacement
                        ((MainActivity) getActivity()).replaceFragment(resultFragment);
                    });
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    private File getOutputDirectory() {
        File mediaDir = getActivity().getExternalMediaDirs()[0];
        if (mediaDir != null) {
            File output = new File(mediaDir, getResources().getString(R.string.app_name));
            if (!output.exists()) {
                output.mkdirs();
            }
            return output;
        }
        return getActivity().getFilesDir();
    }

}