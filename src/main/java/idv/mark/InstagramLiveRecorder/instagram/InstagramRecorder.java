package idv.mark.InstagramLiveRecorder.instagram;

import idv.mark.InstagramLiveRecorder.instagram.model.ParameterSetting;
import idv.mark.InstagramLiveRecorder.instagram.service.GetUserStreamInfo;
import idv.mark.InstagramLiveRecorder.instagram.service.MPDRecorder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Instagram 錄製器程式進入點 (有透過maven mainClass設定)
 */
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
                    parameterSetting.setOutputPath(FileUtil.getFilePath(outputFilePath));
                    String fileName = FileUtil.getFileName(outputFilePath);
                    parameterSetting.setOutputFileName(FileUtil.getFileName(outputFilePath));
                    parameterSetting.setOutputFileExtension(FileUtil.getFileExtension(fileName));
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

        // 初始化 Instagram 錄製器
        MPDRecorder mpdRecorder = null;
        try {
            GetUserStreamInfo getUserStreamInfo = new GetUserStreamInfo(username, csrfToken, sessionId);
            String dashPlaybackUrlByWebInfoApi = getUserStreamInfo.getDashPlaybackUrlByWebInfoApi();
            mpdRecorder = new MPDRecorder(dashPlaybackUrlByWebInfoApi, parameterSetting);
        } catch (Exception e) {
            log.error("GetUserStreamInfo or getDashPlaybackUrlByWebInfoApi error", e);
            System.exit(1);
        }

        // 設置中斷處理
        MPDRecorder finalMpdRecorder = mpdRecorder;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("stopped triggered, write file...");
            try {
                finalMpdRecorder.stop();
            } catch (Exception e) {
                log.error("Error while stopping MPDProcessor", e);
            } finally {
                log.info("Shutdown complete!");
            }
        }));

        // 開始處理錄製過程
        log.info("start processing...");
        mpdRecorder.process();
        log.info("all done!");
    }
}
