package org.me.gcu.g_fadda_honours_project_23_24;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ClassificationResultVM extends ViewModel {

    private MutableLiveData<String> classifiedResult = new MutableLiveData<>();
    private MutableLiveData<String> confidenceResult = new MutableLiveData<>();
    private MutableLiveData<String> imagePath = new MutableLiveData<>();

    private MutableLiveData<Integer> comparisonImageId = new MutableLiveData<>(); // Added line for reference image ID***


    // Getters for LiveData
    public LiveData<String> getClassifiedResult() {

        return classifiedResult;
    }

    public LiveData<String> getConfidenceResult() {

        return confidenceResult;
    }

    public LiveData<String> getImagePath() {

        return imagePath;
    }
    //****************************
    public LiveData<Integer>getComparisonImageId(){
        return comparisonImageId;
    }

    // Setters for values
    public void setClassifiedResult(String result) {

        classifiedResult.setValue(result);
    }

    public void setConfidenceResult(String confidence) {

        confidenceResult.setValue(confidence);
    }

    public void setImagePath(String path) {

        imagePath.setValue(path);
    }
    // ****************************
    public void setComparisonImageId(Integer imageId) {
        comparisonImageId.setValue(imageId);
    }
}
