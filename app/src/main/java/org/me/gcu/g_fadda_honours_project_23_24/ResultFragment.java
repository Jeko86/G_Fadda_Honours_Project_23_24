package org.me.gcu.g_fadda_honours_project_23_24;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.me.gcu.g_fadda_honours_project_23_24.ml.Model;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;


public class ResultFragment extends Fragment {

    private ImageView imageView, comparisonImageView;
    private TextView classified_result, confidence_result;
    private final int imageSize = 224;
    private ClassificationResultVM viewModel;

    // save tle latest classification
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ClassificationResultVM.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        imageView = view.findViewById(R.id.imageView);
        comparisonImageView = view.findViewById(R.id.comparisonImageView);
        classified_result = view.findViewById(R.id.classified_result);
        confidence_result = view.findViewById(R.id.confidence_result);

        // Check if arguments contain the image path and load it
        if (getArguments() != null && getArguments().containsKey("imagePath")) {
            String imagePath = getArguments().getString("imagePath");
            loadImageAndClassify(imagePath);
        }

        viewModel.getClassifiedResult().observe(getViewLifecycleOwner(), result -> {
            classified_result.setText(result);
            updateComparisonImage(result);
        });

        viewModel.getConfidenceResult().observe(getViewLifecycleOwner(), confidence -> {
            confidence_result.setText(confidence);
        });

        viewModel.getImagePath().observe(getViewLifecycleOwner(), newPath -> {
            Bitmap bitmap = BitmapFactory.decodeFile(newPath);
            imageView.setImageBitmap(bitmap);
        });

        viewModel.getComparisonImageId().observe(getViewLifecycleOwner(), imageId -> {
            if (imageId != null) {
                comparisonImageView.setImageResource(imageId);

            }
        });

        return view;
    }
    //**********************************
    private String saveImageToInternalStorage(Bitmap bitmap) {
        ContextWrapper cw = new ContextWrapper(requireActivity());
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        // Create a unique name for the file based on the current time to prevent overwrites
        String fileName = "classified_" + System.currentTimeMillis() + ".png";
        File filePath = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePath.getAbsolutePath();
    }

    private void loadImageAndClassify(String imagePath) {
        Bitmap imageBitmap = null;

        // Check if imagePath is an URI or a file path and load the image accordingly
        if (imagePath.startsWith("content://")) {
            // imagePath is an URI, load the image from the URI
            try {
                Uri imageUri = Uri.parse(imagePath);
                imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
            }
        } else {
            // imagePath is a file path, load the image as before
            imageBitmap = BitmapFactory.decodeFile(imagePath);
        }

        if (imageBitmap != null) {
            imageView.setImageBitmap(imageBitmap);
            // Scale the image for classification
            Bitmap scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, false);
            classifyImage(scaledImage);
        } else {
            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    //load and set model to classify images imported
    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(requireContext());

            // Scale the selected image for classification
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            scaledImage.getPixels(intValues, 0, scaledImage.getWidth(), 0, 0, scaledImage.getWidth(), scaledImage.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Apple___Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust",
                    "Apple___healthy", "Background_without_leaves", "Grape___Black_rot",
                    "Grape___Esca_(Black_Measles)", "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Grape___healthy"};

            //Mapping from original class names to user-friendly names
            HashMap<String, String> diseaseName = getStringStringHashMap();


            classified_result.setText(diseaseName.get(classes[maxPos]));

            // getting the classification result
            String resultLabel = diseaseName.get(classes[maxPos]);
            viewModel.setClassifiedResult(resultLabel);

            //Displaying only the confidence of the class with the highest confidence
            @SuppressLint("DefaultLocale") String highestConfidencePercentage = String.format("%.1f%%", maxConfidence * 100);
            viewModel.setConfidenceResult(highestConfidencePercentage);

            // Save the classified image in high resolution as before scaled to be use by TensorFlow Lite model
            String imagePath = saveImageToInternalStorage(image);
            viewModel.setImagePath(imagePath);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error loading image or model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateComparisonImage(String resultLabel) {
        // Assuming you have a method in your ViewModel to set the reference image ID
        if ("Apple Scab".equals(resultLabel)) {
            viewModel.setComparisonImageId(R.drawable.applescab); // Example reference image
        }
        else if ("Apple Black Rot".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.appleblackrot);
        }
        else if ("Cedar Apple Rust".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.applerust);
        }
        else if ("Healthy Apple leaf".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.applehealthy);
        }
        else if ("Grape Black Rot".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.grapeblackrot);
        }
        else if ("Grape Esca".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.grapeesca);
        }
        else if ("Grape Leaf Blight".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.grapeleafblight);
        }
        else if ("Healthy Grape leaf".equals(resultLabel)){
            viewModel.setComparisonImageId(R.drawable.grapehealthy);
        }
        else{
            viewModel.setComparisonImageId(R.drawable.noresults);
        }
    }



    @NonNull
    private static HashMap<String, String> getStringStringHashMap() {
        HashMap<String, String> diseaseName = new HashMap<>();
        diseaseName.put("Apple___Apple_scab", "Apple Scab");
        diseaseName.put("Apple___Black_rot", "Apple Black Rot");
        diseaseName.put("Apple___Cedar_apple_rust", "Cedar Apple Rust");
        diseaseName.put("Apple___healthy", "Healthy Apple leaf");
        diseaseName.put("Background_without_leaves", "No leave detected");
        diseaseName.put("Grape___Black_rot", "Grape Black Rot");
        diseaseName.put("Grape___Esca_(Black_Measles)", "Grape Esca");
        diseaseName.put("Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Grape Leaf Blight");
        diseaseName.put("Grape___healthy", "Healthy Grape leaf");
        return diseaseName;
    }
}