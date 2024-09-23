package idv.mark.InstagramLiveRecorder.instagram;

import idv.mark.InstagramLiveRecorder.instagram.model.ParameterSetting;
import idv.mark.InstagramLiveRecorder.instagram.service.GetUserStreamInfo;
import idv.mark.InstagramLiveRecorder.instagram.service.MPDProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Data
@Slf4j
public class InstagramRecorder {

    public static void main(String[] args) {
        GetUserStreamInfo getUserStreamInfo = new GetUserStreamInfo(
                "triplescosmos",
                "{your_csrf_token}",
                "{your_session_id}"
        );
        String dashPlaybackUrlByWebInfoApi = getUserStreamInfo.getDashPlaybackUrlByWebInfoApi();
        ParameterSetting parameterSetting = new ParameterSetting();
        parameterSetting.setNeedDigitHistory(true);
        String outputPath = parameterSetting.getOutputPath();
        // 創建資料夾
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        MPDProcessor mpdProcessor = new MPDProcessor(dashPlaybackUrlByWebInfoApi, parameterSetting);
        mpdProcessor.process();
    }
}
