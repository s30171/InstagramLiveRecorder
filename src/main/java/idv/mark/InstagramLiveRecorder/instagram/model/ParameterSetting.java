package idv.mark.InstagramLiveRecorder.instagram.model;

import lombok.Data;

@Data
public class ParameterSetting {
    // 強制中止訊號
    private boolean forceRecording = false;
    // 是否需要歷史紀錄
    private boolean needDigitHistory = false;
    // 請求間隔時間
    private int MAX_DIGIT_INTERVAL_MILLISECONDS = 300;
    // 輸出路徑
    private String outputPath = "output";
    // 輸出檔名
    private String outputFileName = "default.mp4";
    // ffmpeg 路徑
    private String ffmpegPath = "";
    // ffmpeg 輸出參數
    private String ffmpegOutputParameter = "-c:v copy -c:a aac";
}
