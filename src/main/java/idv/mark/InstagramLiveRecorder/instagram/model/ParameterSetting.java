package idv.mark.InstagramLiveRecorder.instagram.model;

import lombok.Data;

@Data
public class ParameterSetting {
    private boolean needDigitHistory = false;
    private boolean forceRecording = false;
    private int MAX_DIGIT_INTERVAL_MILLISECONDS = 300;
    private String outputPath = "output";

}
