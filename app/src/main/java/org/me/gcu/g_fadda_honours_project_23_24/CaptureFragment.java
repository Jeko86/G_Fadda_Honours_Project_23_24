package org.me.gcu.g_fadda_honours_project_23_24;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
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
import android.media.MediaActionSound; // implement a sound effect when take a photo


public class CaptureFragment extends Fragment {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCameraX();
                    } else {
                        Toast.makeText(getContext(), "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        displaySelectedImage(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture, container, false);
        previewView = view.findViewById(R.id.previewView);
        Button captureButton = view.findViewById(R.id.bt_CapturePhoto);
        Button galleryButton = view.findViewById(R.id.bt_Gallery);

        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        checkCameraPermission();

        captureButton.setOnClickListener(v -> capturePhoto());
        galleryButton.setOnClickListener(v -> openGallery());

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
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void capturePhoto() {
        triggerFlashEffect();

        File photoFile = new File(getOutputDirectory(), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                getActivity().runOnUiThread(() -> displayCapturedImage(photoFile));
            }
            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    private void triggerFlashEffect() {
        final View flashOverlay = getView().findViewById(R.id.flashOverlay);

        getActivity().runOnUiThread(() -> {
            flashOverlay.setVisibility(View.VISIBLE);
            flashOverlay.setBackgroundColor(Color.WHITE);
            flashOverlay.animate()
                    .setDuration(150)
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            flashOverlay.setVisibility(View.GONE);
                            flashOverlay.setAlpha(1.0f);
                            flashOverlay.setBackgroundColor(Color.TRANSPARENT);
                        }
                    });
        });
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private File getOutputDirectory() {
        File mediaDir = getActivity().getExternalMediaDirs()[0];
        if (mediaDir != null && mediaDir.exists()) {
            File output = new File(mediaDir, getResources().getString(R.string.app_name));
            if (!output.exists()) {
                output.mkdirs();
            }
            return output;
        }
        return getActivity().getFilesDir();
    }

    private void displaySelectedImage(Uri uri) {
        navigateToResultFragment(uri.toString());
    }

    private void displayCapturedImage(File photoFile) {
        navigateToResultFragment(photoFile.getAbsolutePath());
    }

    private void navigateToResultFragment(String imagePath) {
        Bundle bundle = new Bundle();
        bundle.putString("imagePath", imagePath);
        ResultFragment resultFragment = new ResultFragment();
        resultFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, resultFragment)
                .addToBackStack(null)
                .commit();
    }
}