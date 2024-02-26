package org.me.gcu.g_fadda_honours_project_23_24;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class GalleryFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        openGallery();

        return view;
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("imagePath", selectedImageUri.toString()); // Key 'imagePath' used to pass the image

                    ResultFragment resultFragment = new ResultFragment();
                    resultFragment.setArguments(bundle);


                    getFragmentManager().beginTransaction()
                            .replace(R.id.frame_layout, resultFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    }
}