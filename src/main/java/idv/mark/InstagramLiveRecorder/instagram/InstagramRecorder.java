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
        ParameterSetting parameterSetting = new ParameterSetting();
        // 預設值
        String outputFilePath = parameterSetting.getOutputPath();
        String username = null;
        String csrfToken = null;
        String sessionId = null;
        Integer requestInterval = parameterSetting.getMAX_DIGIT_INTERVAL_MILLISECONDS();

        // 解析命令行參數
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    log.info("\nUsage: java -jar InstagramLiveRecorder.jar -i <outputFilePath> -u <username> -c <csrfToken> -s <sessionId> [-interval <requestInterval>] [-history]");
                    break;
                case "-ffmpegPath":
                    parameterSetting.setFfmpegPath(args[++i]); // ffmpeg 路徑
                    break;
                case "-ffmpegParam":
                    parameterSetting.setFfmpegOutputParameter(args[++i]); // ffmpeg 輸出參數
                    break;
                case "-i":
                    outputFilePath = args[++i]; // 輸出路徑
                    parameterSetting.setOutputPath(getFilePath(outputFilePath));
                    parameterSetting.setOutputFileName(getFileName(outputFilePath));
                    break;
                case "-u":
                    username = args[++i]; // Instagram 用戶名
                    break;
                case "-c":
                    csrfToken = args[++i]; // csrfToken
                    break;
                case "-s":
                    sessionId = args[++i]; // sessionId
                    break;
                case "-interval":
                    if (i + 1 < args.length) { // 確保還有下一個參數
                        try {
                            requestInterval = Integer.parseInt(args[++i]); // 這裡直接解析並增加 i
                            if (requestInterval < 1 || requestInterval > 5000) {
                                log.error("interval must be a 1-5000 number");
                                System.exit(1);
                            }
                        } catch (NumberFormatException e) {
                            log.error("interval must be a 1-5000 number");
                            System.exit(1);
                        }
                    }
                    break;
                case "-history":
                    parameterSetting.setNeedDigitHistory(true); // 是否需要歷史紀錄
                    break;
                default:
                    log.error("unknown parameter: " + args[i]);
            }
        }

        String logMsg = "\n";
        boolean exit = false;
        // 檢查是否有必須的參數
        if (username == null) {
            exit = true;
            logMsg += "need username：-u <username> \n";
        }
        if (csrfToken == null) {
            exit = true;
            logMsg += "need csrf_token：-c <csrf_token> \n";
        }
        if (sessionId == null) {
            exit = true;
            logMsg += "need session_id：-s <session_id>";
        }
        if (exit) {
            log.error(logMsg);
            System.exit(1);
        }

        // 設置輸出路徑
        File outputDir = new File(parameterSetting.getOutputPath());
        if (!outputDir.exists()) outputDir.mkdirs(); // 創建資料夾

        // 初始化 Instagram 錄製邏輯
        GetUserStreamInfo getUserStreamInfo = new GetUserStreamInfo(username, csrfToken, sessionId);
        String dashPlaybackUrlByWebInfoApi = getUserStreamInfo.getDashPlaybackUrlByWebInfoApi();
        MPDProcessor mpdProcessor = new MPDProcessor(dashPlaybackUrlByWebInfoApi, parameterSetting);

        // 設置中斷處理
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("catch exit, write file...");
            mpdProcessor.stop();
            log.info("recording stopped");
        }));

        // 開始處理錄製過程
        mpdProcessor.process();

        log.info("all done!");
    }

    // 解析輸出路徑 (檔名)
    private static String getFileName(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        return split[split.length - 1];
    }

    // 解析輸出路徑 (檔案資料夾)
    private static String getFilePath(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        if (split.length > 1) {
            return outputFilePath.substring(0, outputFilePath.length() - split[split.length - 1].length() - 1);
        } else {
            return "";
        }
    }
}
