package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.CmdUtil;
import idv.mark.InstagramLiveRecorder.instagram.FileUtil;
import idv.mark.InstagramLiveRecorder.instagram._enum.AudioRepresentationId;
import idv.mark.InstagramLiveRecorder.instagram._enum.VideoRepresentationId;
import idv.mark.InstagramLiveRecorder.instagram.model.MPD;
import idv.mark.InstagramLiveRecorder.instagram.model.ParameterSetting;
import idv.mark.InstagramLiveRecorder.instagram.model.SegmentStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Data
@Slf4j
public class MPDProcessor {
    private String dashABRPlaybackUrl;
    private String dashHost;
    private String videoMediaUrl;
    private String audioMediaUrl;
    private ParameterSetting parameterSetting;
    private InstagramHttpUtil instagramHttpUtil = new InstagramHttpUtil();
    private List<MPD> mpdList = new ArrayList<>();
    private Map<Long, byte[]> m4vVideoDataMap = new HashMap<>();
    private Map<Long, byte[]> m4aAudioDataMap = new HashMap<>();
    private Map<MPD.S, SegmentStatus> segmentsDownloadMap = new ConcurrentHashMap<>();
    private boolean recording = true;
    private boolean init = false;
    private boolean stopTriggered = false;

    public MPDProcessor(String dashABRPlaybackUrl, ParameterSetting parameterSetting) {
        this.dashABRPlaybackUrl = dashABRPlaybackUrl;
        this.dashHost = dashABRPlaybackUrl.split("dash-abr")[0];
        this.parameterSetting = parameterSetting;
    }

    public void process() {
        if (StringUtils.isAllBlank(this.dashABRPlaybackUrl)) {
            throw new RuntimeException("dashPlaybackUrl is blank");
        }
        // 確保任務完成, 並且等待所有的任務完成
        List<CompletableFuture<Void>> allMissions = new ArrayList<>();
        log.info("Starting detect mission");
        detect().thenRun(() -> log.info("Detect mission completed"));
        log.info("Starting record mission");
        allMissions.add(record().thenRun(() -> log.info("Record mission completed")));
        if (parameterSetting.isNeedDigitHistory()) {
            // 挖掘過去直播回放 (從參數設定中取得)
            log.info("Starting digit history mission");
            allMissions.add(digitHistory().thenRun(() -> log.info("Digit history mission completed")));
        }
        CompletableFuture.allOf(allMissions.toArray(new CompletableFuture[0])).join();
    }

    // 停止錄影 (可能由user自己觸發)
    public void stop() {
        log.info("Stop triggered, Thread : {}", Thread.currentThread().getId());
        if (stopTriggered) {
            Thread.currentThread().interrupt();
        }
        stopTriggered = true;
        recording = false;
        parameterSetting.setForceRecording(true);
        waitUntilAllSegmentsDownloaded();
        writeFile();
    }

    // 等待所有的segment下載完成
    private void waitUntilAllSegmentsDownloaded() {
        if (MapUtils.isEmpty(segmentsDownloadMap)) {
            throw new RuntimeException("No segment need downloaded");
        }
        int times = 10;
        while (times > 0 && !parameterSetting.isForceRecording()) {
            List<SegmentStatus> notDownloaded = segmentsDownloadMap.values().stream().filter(segmentStatus -> !segmentStatus.isDownloaded()).toList();
            if (CollectionUtils.isEmpty(notDownloaded)) {
                log.info("All segments downloaded");
                break;
            }
            String notDownloadSegments = notDownloaded.stream().map(segmentStatus -> String.valueOf(segmentStatus.getSegmentId())).collect(Collectors.joining(","));
            log.info("Wait for segment downloading: {}", notDownloadSegments);
            times -= 1;
            sleep(3000);
        }
        // 停止detect
        parameterSetting.setForceRecording(true);
    }

    // 執行緒暫停
    private void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 將影片和音訊寫入檔案
    private void writeFile() {
        String ffmpegOutputParameter = parameterSetting.getFfmpegOutputParameter();
        String[] ffmpegParameterArray = ffmpegOutputParameter.split(" ");
        List<String> cmdList = new ArrayList<>();
        String outputPath = parameterSetting.getOutputPath();
        String fileName = parameterSetting.getOutputFileName().replace(String.format(".%s", parameterSetting.getOutputFileExtension()), "");
        String fullFileName = String.format("%s/%s.mp4", outputPath, fileName);
        String anotherPath = askWantToOverwrite(fullFileName);
        boolean overwrite = "y".equals(anotherPath);
        if (!"y".equals(anotherPath)) {
            fullFileName = anotherPath;
        }
        outputPath = FileUtil.getFilePath(fullFileName);
        File file = new File(outputPath);
        if (!file.exists())  file.mkdirs();

        String ffmpegPath = StringUtils.isAllBlank(parameterSetting.getFfmpegPath()) ? "ffmpeg" : String.format("%s/ffmpeg", parameterSetting.getFfmpegPath());
        cmdList.add(ffmpegPath);
        cmdList.add("-i");
        cmdList.add(String.format("%s/temp.m4v", outputPath));
        cmdList.add("-i");
        cmdList.add(String.format("%s/temp.m4a", outputPath));
        cmdList.addAll(Arrays.asList(ffmpegParameterArray));
        if (overwrite) cmdList.add("-y");
        cmdList.add(fullFileName);

        List<SegmentStatus> segmentStatusList = new ArrayList<>(segmentsDownloadMap.values());
        segmentStatusList.sort((o1, o2) -> (int) (o1.getSegmentId() - o2.getSegmentId()));
        try (ByteArrayOutputStream videoOutputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream audioOutputStream = new ByteArrayOutputStream()) {
            for (SegmentStatus segmentStatus : segmentStatusList) {
                long segmentId = segmentStatus.getSegmentId();
                byte[] videoBytes = m4vVideoDataMap.get(segmentId);
                byte[] audioBytes = m4aAudioDataMap.get(segmentId);
                if ((videoBytes == null ||videoBytes.length == 0) || (audioBytes == null || audioBytes.length == 0)) {
                    log.error("segmentId={} download fail, skip segment", segmentId);
                    continue;
                }
                videoOutputStream.write(m4vVideoDataMap.get(segmentId));
                audioOutputStream.write(m4aAudioDataMap.get(segmentId));
            }
            writeToFile(videoOutputStream.toByteArray(), outputPath, "temp.m4v");
            writeToFile(audioOutputStream.toByteArray(), outputPath, "temp.m4a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CmdUtil.exec(cmdList.toArray(new String[0]));

        // 刪除暫存檔
        File tempOutputM4v = new File(String.format("%s/temp.m4v", outputPath));
        File tempOutputM4a = new File(String.format("%s/temp.m4a", outputPath));
        if (tempOutputM4v.exists()) {
            tempOutputM4v.delete();
        }
        if (tempOutputM4a.exists()) {
            tempOutputM4a.delete();
        }
        log.info("output file: {}", fullFileName);
    }

    // 讓使用者選擇是否覆蓋或提供新檔案路徑
    public static String askWantToOverwrite(String fullFileName) {
        File file = new File(fullFileName);
        if (!file.exists()) {
            return fullFileName;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.printf("file %s exist。choose:\n- type 'y' to overrite file\n- or set path for file (ex: test/record.mp4)", fullFileName);
        String userInput = scanner.nextLine().trim();
        if ("y".equals(userInput)) {
            return "y";
        } else {
            return askWantToOverwrite(userInput);
        }
    }


    // 在存放於現有的MPD duration中，觸發挖掘出過去的直播
    // 會依照觸發多至少的順序發送Http HEAD request, 404表示無, 200表示有
    // 有的話, 會將該segment加入到segmentsDownloadMap, 並且由偵測器下載該segment
    // 若全部都沒有, 則從小到大, 依序挖掘 (過往發送過的請求不加入挖掘順序)
    private CompletableFuture<Void> digitHistory() {
        return CompletableFuture.runAsync(() -> {
            while (!parameterSetting.isForceRecording()) {
                sleep(3000);
                // 樣本數沒有達到30個, 先繼續等到達到30個 (或是停止錄影)
                if (segmentsDownloadMap.keySet().size() > 30 || !recording) {
                    break;
                }
            }
            List<MPD.S> segmentList = new ArrayList<>(segmentsDownloadMap.keySet());
            segmentList.sort((o1, o2) -> (int) (o1.getT() - o2.getT()));
            Map<Long, List<MPD.S>> segmentMap = segmentList.stream().collect(Collectors.groupingBy(MPD.S::getD));
            List<Long> digitDurationFirstList = segmentMap.keySet().stream().sorted().collect(Collectors.toList());
            List<Long> allDuration = LongStream.rangeClosed(1800, 2500).boxed().collect(Collectors.toList());
            log.info("Start digit history, min={}, max={}", 1800, 2500);
            digitDurationFirstList.remove(0);
            digit(digitDurationFirstList, allDuration);
        });
    }

    // 挖掘過去的直播 (遞迴方法)
    private void digit(List<Long> digitDurationFirstList, List<Long> allDuration) {
        long digitSegmentId;
        List<MPD.S> segmentList = new ArrayList<>(segmentsDownloadMap.keySet());
        segmentList.sort((o1, o2) -> (int) (o1.getT() - o2.getT()));
        MPD.S startDigitSegment = segmentList.get(1);
        List<Long> copyDigitFirstList = new ArrayList<>(digitDurationFirstList);
        List<Long> copyAllDuration = new ArrayList<>(allDuration);
        if (startDigitSegment.getT() <= 5000) {
            // 5000以下的segment不挖掘
            log.info("digit done, last segmentId = {}", startDigitSegment.getT());
            return;
        }
        // 4001 可能是直播第一個segment的id, 加入嘗試挖掘
        long t = startDigitSegment.getT();
        if (4001 < t && t < 7000) {
            copyAllDuration.add(0, t - 4001L);
        }
        while (!parameterSetting.isForceRecording()) {
            long minusDuration;
            if (!CollectionUtils.isEmpty(copyDigitFirstList)) {
                Long copyDigitFirstSegmentId = copyDigitFirstList.get(0);
                minusDuration = copyDigitFirstList.remove(0);
                allDuration.remove(copyDigitFirstSegmentId);
            } else if (!CollectionUtils.isEmpty(copyAllDuration)) {
                minusDuration = copyAllDuration.remove(0);
            } else {
                log.info("digit history end");
                break;
            }
            digitSegmentId = startDigitSegment.getT() - minusDuration;
            HttpResponse<byte[]> httpResponse = tryDigitBySegmentId(digitSegmentId);
            if (httpResponse == null) {
                int retryTimes = 5;
                while (retryTimes >= 0) {
                    retryTimes--;
                    httpResponse = tryDigitBySegmentId(digitSegmentId);
                    if (httpResponse == null) {
                        log.error("digitHasSource error, httpResponse is null, retryTimes={}, segmentId={}", retryTimes, digitSegmentId);
                    } else {
                        break;
                    }
                }
                if (retryTimes <= 0 && httpResponse == null) {
                    // 重試五次失敗的話跳過
                    log.error("retry failed, segmentId={}", digitSegmentId);
                    continue;
                }
            }
            int statusCode = httpResponse.statusCode();
            if (statusCode == 200) {
                // 挖掘成功
                log.info("digit segment {} success", digitSegmentId);
                MPD.S digitSuccessSegment = new MPD.S(digitSegmentId, minusDuration);
                segmentsDownloadMap.put(digitSuccessSegment, new SegmentStatus(digitSegmentId, this.videoMediaUrl, this.audioMediaUrl, false, false));
                if (digitSuccessSegment.getT() > 4300) {
                    digit(digitDurationFirstList, allDuration);
                } else {
                    log.info("digit done, last segmentId = {}", digitSuccessSegment.getT());
                }
                break;
            } else if (statusCode == 404 || statusCode == 410) {
                if (!recording) {
                    // 錄影當下不要印出log
                    log.debug("digit segment {} not found", digitSegmentId);
                }
                // 404表示沒有, 410表示過去有但現在沒有
            } else {
                log.error("digitHasSource error, httpResponse status code: {}", statusCode);
                break;
            }
        }
    }

    // 嘗試挖掘segment
    private HttpResponse<byte[]> tryDigitBySegmentId(long digitSegmentId) {
        sleep(parameterSetting.getMAX_DIGIT_INTERVAL_MILLISECONDS());
        String digitURL = this.videoMediaUrl.replace("$Time$", String.valueOf(digitSegmentId)).replace("../", dashHost);
        return instagramHttpUtil.digitHasSource(digitURL);
    }

    // 錄影主方法 (分別從dash MPD XML 檔案取得m4v, m4a)
    private CompletableFuture<Void> record() {
        return CompletableFuture.runAsync(() -> {
            log.info("Start recording");
            while (recording && !parameterSetting.isForceRecording()) {
                sleep(2000);
                // get MPD file
                MPD mpd = instagramHttpUtil.getMPDXMLByDashPlayBackUrl(this.dashABRPlaybackUrl);
                MPD.Period period = mpd.getPeriod().get(0);

                // 篩選出 videoRepresentation 和 audioRepresentation
                MPD.Representation videoRepresentation = null;
                MPD.Representation audioRepresentation = null;
                List<MPD.AdaptationSet> adaptationSetList = period.getAdaptationSet();
                for (MPD.AdaptationSet adaptationSet : adaptationSetList) {
                    List<MPD.Representation> representationList = adaptationSet.getRepresentation();
                    for (MPD.Representation representation : representationList) {
                        String id = representation.getId();
                        if (VideoRepresentationId.SOURCE.getId().equals(id)) {
                            videoRepresentation = representation;
                        } else if (AudioRepresentationId.SOURCE.getId().equals(id)) {
                            audioRepresentation = representation;
                        }
                    }
                }

                // 下載 init 的編碼資訊
                addInit(videoRepresentation, audioRepresentation);

                // 處理segments (加入到 segments & 爬回上一個)
                MPD.SegmentTemplate segmentTemplate = videoRepresentation.getSegmentTemplate();
                MPD.SegmentTimeline segmentTimeline = segmentTemplate.getSegmentTimeline();
                List<MPD.S> sList = segmentTimeline.getS();
                // 加到 segmentsDownloadedMap
                for (MPD.S s : sList) {
                    if (segmentsDownloadMap.containsKey(s)) {
                        continue;
                    }
                    segmentsDownloadMap.put(s, new SegmentStatus(s.getT(),
                            videoRepresentation.getSegmentTemplate().getMedia(),
                            audioRepresentation.getSegmentTemplate().getMedia(),
                            false,
                            false));
                }
                if ("static".equals(mpd.getType())) {
                    recording = false;
                    break;
                }
                if (mpdList.contains(mpd)) {
                    continue;
                }
                mpdList.add(mpd);
            }
            log.info("End recording");
        });
    }

    // 加入dash init 的編碼資訊
    private void addInit(MPD.Representation videoRepresentation, MPD.Representation audioRepresentation) {
        if (init) {
            return;
        }
        MPD.S s = new MPD.S(0L, 0L);
        synchronized (segmentsDownloadMap) {
            SegmentStatus segmentStatus = segmentsDownloadMap.get(s);
            if (Objects.isNull(segmentStatus)) {
                this.init = true;
                segmentsDownloadMap.put(s, new SegmentStatus(s.getT(),
                        videoRepresentation.getSegmentTemplate().getMedia(),
                        audioRepresentation.getSegmentTemplate().getMedia(),
                        false,
                        true));
            }
        }
        downloadSegment(true, s, videoRepresentation.getSegmentTemplate().getMedia(), audioRepresentation.getSegmentTemplate().getMedia());
    }

    // 啟動偵測器 (主要負責下載加入Map的segment)
    public CompletableFuture<Void> detect() {
       return CompletableFuture.runAsync(() -> {
            while (!parameterSetting.isForceRecording()) {
                sleep(2000);
                if (segmentsDownloadMap.isEmpty()) {
                    continue;
                }
                for (Map.Entry<MPD.S, SegmentStatus> entry : segmentsDownloadMap.entrySet()) {
                    MPD.S segment = entry.getKey();
                    SegmentStatus status = entry.getValue();
                    if (status.isDownloaderRunning()) {
                        continue;
                    }
                    if (status.isDownloaded()) {
                        continue;
                    }
                    sleep(200);
                    downloadSegment(false, segment, status.getVideoMediaUrl(), status.getAudioMediaUrl());
                }
            }
        });
    }

    // 將byte[]寫入檔案
    private void writeToFile(byte[] data, String filePath, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(filePath + File.separator + fileName)) {
            new File(filePath).mkdirs();
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 下載segment主要方法
    private void downloadSegment(boolean init, MPD.S s, String videoMediaUrl, String audioMediaUrl) {
        this.videoMediaUrl = videoMediaUrl;
        this.audioMediaUrl = audioMediaUrl;
        String segmentId;
        if (init) {
            segmentId = "init";
        } else {
            segmentId = String.valueOf(s.getT());
        }
        SegmentStatus segmentStatus = segmentsDownloadMap.get(s);
        // 取得 video 下載網址
        String videoDownloadURL = videoMediaUrl.replace("$Time$", segmentId).replace("../", dashHost);
        // 取得 audio 下載網址
        String audioDownloadURL = audioMediaUrl.replace("$Time$", segmentId).replace("../", dashHost);
        // 下載影片和音訊，並在兩者都完成後處理
        instagramHttpUtil.downloadSegment(videoDownloadURL)
                .thenCombine(instagramHttpUtil.downloadSegment(audioDownloadURL), (videoResponse, audioResponse) -> {
                    if (videoResponse.statusCode() != 200 || audioResponse.statusCode() != 200) {
                        log.error("Download error: video={}, audio={}", videoResponse.statusCode(), audioResponse.statusCode());
                        log.error("video={}, audio={}", videoDownloadURL, audioDownloadURL);
                        return null;
                    }
                    // 將結果加入到 List
                    m4vVideoDataMap.put(s.getT(), videoResponse.body());
                    m4aAudioDataMap.put(s.getT(), audioResponse.body());
                    segmentStatus.setDownloaded(true);
                    segmentStatus.setDownloaderRunning(false);
                    return String.format("segment %s downloaded", segmentId);
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Download failed: {}", throwable.getMessage());
                        return;
                    }
                    if (result != null && recording) {
                        log.info(result);
                    }
                });
    }
}
