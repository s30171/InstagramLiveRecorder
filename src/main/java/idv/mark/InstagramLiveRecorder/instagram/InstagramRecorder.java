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
        String username = null;
        String csrfToken = null;
        String sessionId = null;

        // 解析參數
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    log.info("\nUsage: java -jar InstagramLiveRecorder.jar -i <outputFilePath> -u <username> -c <csrfToken> -s <sessionId> [-interval <requestInterval>] [-history]");
                    System.exit(0);
                    break;
                case "-ffmpegPath":
                    parameterSetting.setFfmpegPath(args[++i]);
                    break;
                case "-ffmpegParam":
                    parameterSetting.setFfmpegOutputParameter(args[++i]);
                    break;
                case "-i":
                    String outputFilePath = args[++i];
                    parameterSetting.setOutputPath(getFilePath(outputFilePath));
                    String fileName = getFileName(outputFilePath);
                    parameterSetting.setOutputFileName(getFileName(outputFilePath));
                    parameterSetting.setOutputFileExtension(getFileExtension(fileName));
                    break;
                case "-u":
                    username = args[++i]; // Instagram 用戶名 ex: triplescosmos
                    break;
                case "-c":
                    csrfToken = args[++i];
                    break;
                case "-s":
                    sessionId = args[++i];
                    break;
                case "-interval":
                    if (i + 1 < args.length) {
                        try {
                            int requestInterval = Integer.parseInt(args[++i]);
                            if (requestInterval < 1 || requestInterval > 5000) {
                                log.error("interval must be a 1-5000 number");
                                System.exit(1);
                            }
                            parameterSetting.setMAX_DIGIT_INTERVAL_MILLISECONDS(requestInterval);
                        } catch (NumberFormatException e) {
                            log.error("interval must be a 1-5000 number");
                            System.exit(1);
                        }
                    }
                    break;
                case "-history":
                    parameterSetting.setNeedDigitHistory(true);
                    break;
                default:
                    log.error("unknown parameter: " + args[i]);
            }
        }

        String logMsg = "\n";
        boolean exit = false;
        // 檢查是否有必須的參數 (username, csrfToken, sessionId)
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

        // 初始化 Instagram 錄製器
        MPDProcessor mpdProcessor = null;
        try {
            GetUserStreamInfo getUserStreamInfo = new GetUserStreamInfo(username, csrfToken, sessionId);
            String dashPlaybackUrlByWebInfoApi = getUserStreamInfo.getDashPlaybackUrlByWebInfoApi();
            mpdProcessor = new MPDProcessor(dashPlaybackUrlByWebInfoApi, parameterSetting);
        } catch (Exception e) {
            log.error("GetUserStreamInfo or getDashPlaybackUrlByWebInfoApi error", e);
            System.exit(1);
        }

        // 設置中斷處理
        MPDProcessor finalMpdProcessor = mpdProcessor;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("catch exit, write file...");
            try {
                finalMpdProcessor.stop();
            } catch (Exception e) {
                log.error("Error while stopping MPDProcessor", e);
            } finally {
                log.info("all stopped, Thread: {}", Thread.currentThread().getId());
                System.exit(0);
                log.info("System.exit triggered! Thread: {}", Thread.currentThread().getId());
            }
        }));

        // 開始處理錄製過程
        log.info("start processing... Thread: {}", Thread.currentThread().getId());
        mpdProcessor.process();
        log.info("all done! Thread: {}", Thread.currentThread().getId());

        System.exit(0); // 正常退出
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

    // 解析輸出路徑 (副檔名)
    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
}
