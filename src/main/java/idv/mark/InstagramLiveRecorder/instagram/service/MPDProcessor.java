package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.CmdUtil;
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
    private List<MPD> mpdList = new ArrayList<>();
    private final Map<MPD.S, SegmentStatus> segmentsDownloadMap = new ConcurrentHashMap<>();
    private Map<Long, byte[]> m4vVideoDataMap = new HashMap<>();
    private Map<Long, byte[]> m4aAudioDataMap = new HashMap<>();
    private boolean recording = true;
    private ParameterSetting parameterSetting;
    private String videoMediaUrl;
    private String audioMediaUrl;
    private InstagramHttpUtil instagramHttpUtil = new InstagramHttpUtil();
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
        printLogInfo("Starting detect mission");
        detect().thenRun(() -> printLogInfo("Detect mission completed"));
        printLogInfo("Starting record mission");
        allMissions.add(record().thenRun(() -> printLogInfo("Record mission completed")));
        if (parameterSetting.isNeedDigitHistory()) {
            // 挖掘過去直播回放 (從參數設定中取得)
            printLogInfo("Starting digit history mission");
            allMissions.add(digitHistory().thenRun(() -> printLogInfo("Digit history mission completed")));
        }
        CompletableFuture.allOf(allMissions.toArray(new CompletableFuture[0])).join();
        if (CollectionUtils.isEmpty(m4vVideoDataMap.values())) {
            return;
        }
        // 等待所有的segment下載完成
        waitUntilAllSegmentsDownloaded();
        // 寫檔
        writeFile();
    }

    public void stop() {
        if (stopTriggered) {
            Thread.currentThread().interrupt();
        }
        this.stopTriggered = true;
        this.recording = false;
        this.parameterSetting.setForceRecording(true);
    }

    private void waitUntilAllSegmentsDownloaded() {
        if (MapUtils.isEmpty(segmentsDownloadMap)) {
            throw new RuntimeException("No segment need downloaded");
        }
        int times = 10;
        while (times > 0 || !parameterSetting.isForceRecording()) {
            List<SegmentStatus> notDownloaded = segmentsDownloadMap.values().stream().filter(segmentStatus -> !segmentStatus.isDownloaded()).toList();
            if (CollectionUtils.isEmpty(notDownloaded)) {
                printLogInfo("All segments downloaded");
                break;
            }
            String notDownloadSegments = notDownloaded.stream().map(segmentStatus -> String.valueOf(segmentStatus.getSegmentId())).collect(Collectors.joining(","));
            printLogInfo("Wait for segment downloading: {}", notDownloadSegments);
            times -= 1;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 停止detect
        parameterSetting.setForceRecording(true);
    }

    private void writeFile() {
        String outputPath = parameterSetting.getOutputPath();
        String fileName = parameterSetting.getOutputFileName().replace(".mp4", "");
        List<SegmentStatus> segmentStatusList = new ArrayList<>(segmentsDownloadMap.values());
        segmentStatusList.sort((o1, o2) -> (int) (o1.getSegmentId() - o2.getSegmentId()));
        // 寫 video 和 audio
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
        // 音檔影片檔合併一起
        String fullFileName = String.format("%s/%s.mp4", outputPath, fileName);
        String ffmpegOutputParameter = parameterSetting.getFfmpegOutputParameter();
        String[] ffmpegParameterArray = ffmpegOutputParameter.split(" ");
        String ffmpegPath = StringUtils.isAllBlank(parameterSetting.getFfmpegPath()) ? "ffmpeg" : String.format("%s/ffmpeg", parameterSetting.getFfmpegPath());
        List<String> cmdList = new ArrayList<>() {{
            add(ffmpegPath);
            add("-i");
            add(String.format("%s/temp.m4v", outputPath));
            add("-i");
            add(String.format("%s/temp.m4a", outputPath));
        }};
        cmdList.addAll(Arrays.asList(ffmpegParameterArray));
        cmdList.add(fullFileName);
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
        printLogInfo("output file: {}", fullFileName);
    }

    // 在存放於現有的MPD duration中，挖掘出過去的直播
    // 會依照觸發多至少的順序發送Http HEAD request, 404表示無, 200表示有
    // 有的話, 會將該segment加入到segmentsDownloadMap, 並且由偵測器下載該segment
    // 若全部都沒有, 則從小到大, 依序挖掘 (過往發送過的請求不加入挖掘順序)
    private CompletableFuture<Void> digitHistory() {
        return CompletableFuture.runAsync(() -> {
            while (!parameterSetting.isForceRecording()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 樣本數沒有達到30個, 先繼續等到達到30個 (或是停止錄影)
                if (segmentsDownloadMap.keySet().size() > 30 || !recording) {
                    break;
                }
            }
            printTextAnimation();
            List<MPD.S> segmentList = new ArrayList<>(segmentsDownloadMap.keySet());
            segmentList.sort((o1, o2) -> (int) (o1.getT() - o2.getT()));
            Map<Long, List<MPD.S>> segmentMap = segmentList.stream().collect(Collectors.groupingBy(MPD.S::getD));
            List<Long> digitDurationFirstList = segmentMap.keySet().stream().sorted().collect(Collectors.toList());
            List<Long> allDuration = LongStream.rangeClosed(1800, 2500).boxed().collect(Collectors.toList());
            printLogInfo("Start digit history, min={}, max={}", 1800, 2500);
            digitDurationFirstList.remove(0);
            digit(digitDurationFirstList, allDuration);
        });
    }

    private void digit(List<Long> digitDurationFirstList, List<Long> allDuration) {
        long digitSegmentId;
        List<MPD.S> segmentList = new ArrayList<>(segmentsDownloadMap.keySet());
        segmentList.sort((o1, o2) -> (int) (o1.getT() - o2.getT()));
        MPD.S startDigitSegment = segmentList.get(1);
        List<Long> copyDigitFirstList = new ArrayList<>(digitDurationFirstList);
        List<Long> copyAllDuration = new ArrayList<>(allDuration);
        // 4001 通常是直播第一個segment的id, 加入嘗試挖掘
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
                printLogInfo("digit history end");
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
                printLogInfo("digit segment {} success", digitSegmentId);
                MPD.S digitSuccessSegment = new MPD.S(digitSegmentId, minusDuration);
                segmentsDownloadMap.put(digitSuccessSegment, new SegmentStatus(digitSegmentId, this.videoMediaUrl, this.audioMediaUrl, false, false));
                if (digitSuccessSegment.getT() > 4300) {
                    digit(digitDurationFirstList, allDuration);
                } else {
                    printLogInfo("digit done, last segmentId = {}", digitSuccessSegment.getT());
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

    private void printLogInfo(String text, Object... objects) {
        System.out.print("");
        log.info(text, objects);
    }

    private void printTextAnimation() {
        // 創建一個執行緒來顯示Running動畫
        Thread loadingThread = new Thread(() -> {
            String[] animation = { "Running   ", "Running.  ", "Running.. ", "Running..." };
            int i = 0;
            while (!parameterSetting.isForceRecording()) {
                System.out.print("\r" + animation[i % animation.length]); // 顯示動畫
                i++;
                try {
                    Thread.sleep(500); // 設定動畫間隔時間
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // 開始loading動畫執行緒
        loadingThread.start();
    }

    private HttpResponse<byte[]> tryDigitBySegmentId(long digitSegmentId) {
        try {
            // 間隔時間
            Thread.sleep(parameterSetting.getMAX_DIGIT_INTERVAL_MILLISECONDS());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String digitURL = this.videoMediaUrl.replace("$Time$", String.valueOf(digitSegmentId)).replace("../", dashHost);
        return instagramHttpUtil.digitHasSource(digitURL);
    }

    private CompletableFuture<Void> record() {
        return CompletableFuture.runAsync(() -> {
            printLogInfo("Start recording");
            while (recording && !parameterSetting.isForceRecording()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
            printLogInfo("End recording");
        });
    }

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

    public CompletableFuture<Void> detect() {
       return CompletableFuture.runAsync(() -> {
            while (!parameterSetting.isForceRecording()) {
                try {
                    Thread.sleep(2000);
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
                        Thread.sleep(200);
                        downloadSegment(false, segment, status.getVideoMediaUrl(), status.getAudioMediaUrl());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void writeToFile(byte[] data, String filePath, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(filePath + File.separator + fileName)) {
            new File(filePath).mkdirs();
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                    if (result != null) {
                        printLogInfo(result);
                    }
                });
    }
}
