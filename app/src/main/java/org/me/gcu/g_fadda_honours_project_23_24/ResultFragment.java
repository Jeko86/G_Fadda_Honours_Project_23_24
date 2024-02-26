package org.me.gcu.g_fadda_honours_project_23_24;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;


public class ResultFragment extends Fragment {

    private ImageView imageView;
    private TextView classified_result;
    private TextView confidence_result;
    private final int imageSize = 224;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);
        imageView = view.findViewById(R.id.imageView);
        classified_result = view.findViewById(R.id.classified_result);
        confidence_result = view.findViewById(R.id.confidence_result);
        // Check if arguments contain the image path and load it
        if (getArguments() != null && getArguments().containsKey("imagePath")) {
            String imagePath = getArguments().getString("imagePath");
            loadImageAndClassify(imagePath);
        }

        return view;
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

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
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

            // Mapping from original class names to user-friendly names
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


            classified_result.setText(diseaseName.get(classes[maxPos]));

            /*String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", diseaseName.get(classes[i]), confidences[i]*100);
            }
            confidence_result.setText(s);*/

            // After: Displaying only the confidence of the class with the highest confidence
            String highestConfidencePercentage = String.format("%.1f%%", maxConfidence * 100);
            confidence_result.setText(highestConfidencePercentage);


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error loading image or model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


}