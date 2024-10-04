package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.ThreadUtil;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 主要錄製MPD file的類別, 會從MPD XML檔案中取得m4v, m4a
 */
@Data
@Slf4j
public class MPDRecorder {
    private String dashABRPlaybackUrl;
    private String dashHost;
    private ParameterSetting parameterSetting;
    private InstagramHttpUtil instagramHttpUtil = new InstagramHttpUtil();
    private List<MPD> mpdList = new ArrayList<>();
    private boolean recording = true;
    private boolean init = false;
    private boolean stopTriggered = false;
    private final Map<MPD.S, SegmentStatus> segmentsDownloadMap = new ConcurrentHashMap<>();
    private SegmentManager segmentManager;
    private FileMerger fileMerger;
    private SegmentDigger segmentDigger;

    public MPDRecorder(String dashABRPlaybackUrl, ParameterSetting parameterSetting) {
        this.dashABRPlaybackUrl = dashABRPlaybackUrl;
        this.dashHost = dashABRPlaybackUrl.split("dash-abr")[0];
        this.parameterSetting = parameterSetting;
        this.segmentManager = new SegmentManager(dashHost, segmentsDownloadMap, () -> recording, parameterSetting::isForceRecording, instagramHttpUtil);
        this.segmentDigger = new SegmentDigger(() -> recording, () -> segmentManager.getVideoMediaUrl(), () -> segmentManager.getAudioMediaUrl(), dashHost, parameterSetting, instagramHttpUtil);
        this.fileMerger = new FileMerger(parameterSetting);
    }

    public void process() {
        if (StringUtils.isAllBlank(this.dashABRPlaybackUrl)) {
            throw new RuntimeException("dashPlaybackUrl is blank");
        }
        // 確保任務完成, 並且等待所有的任務完成
        List<CompletableFuture<Void>> allMissions = new ArrayList<>();
        log.info("Starting detect mission");
        segmentManager.detect().thenRun(() -> log.info("Detect mission completed"));
        log.info("Starting record mission");
        allMissions.add(record().thenRun(() -> log.info("Record mission completed")));
        if (parameterSetting.isNeedDigitHistory()) {
            // 挖掘過去直播回放 (從參數設定中取得)
            log.info("Starting digit history mission");
            allMissions.add(segmentDigger.digitHistory(segmentsDownloadMap).thenRun(() -> log.info("Digit history mission completed")));
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
        fileMerger.writeFile(segmentManager.getSegmentsDownloadMap().values(), segmentManager.getM4vVideoDataMap(), segmentManager.getM4aAudioDataMap());
    }

    // 錄影主方法 (分別從dash MPD XML 檔案取得m4v, m4a)
    private CompletableFuture<Void> record() {
        return CompletableFuture.runAsync(() -> {
            log.info("Start recording");
            while (recording && !parameterSetting.isForceRecording()) {
                ThreadUtil.sleep(2000);
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
        segmentManager.downloadSegment(true, s, videoRepresentation.getSegmentTemplate().getMedia(), audioRepresentation.getSegmentTemplate().getMedia());
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
            ThreadUtil.sleep(3000);
        }
        // 停止detect
        parameterSetting.setForceRecording(true);
    }
}
