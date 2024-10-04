package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.ThreadUtil;
import idv.mark.InstagramLiveRecorder.instagram.model.MPD;
import idv.mark.InstagramLiveRecorder.instagram.model.SegmentStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 段落管理器 (負責下載segment)
 */
@Slf4j
@Data
@RequiredArgsConstructor
public class SegmentManager {
    private final String dashHost;
    private final Map<MPD.S, SegmentStatus> segmentsDownloadMap;
    private final Supplier<Boolean> recording;
    private final Supplier<Boolean> isForceRecording;
    private final InstagramHttpUtil instagramHttpUtil;
    private String videoMediaUrl;
    private String audioMediaUrl;
    private Map<Long, byte[]> m4vVideoDataMap = new HashMap<>();
    private Map<Long, byte[]> m4aAudioDataMap = new HashMap<>();

    // 啟動偵測器 (主要負責下載加入Map的segment)
    public CompletableFuture<Void> detect() {
        return CompletableFuture.runAsync(() -> {
            while (!isForceRecording.get()) {
                ThreadUtil.sleep(2000);
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
                    ThreadUtil.sleep(200);
                    downloadSegment(false, segment, status.getVideoMediaUrl(), status.getAudioMediaUrl());
                }
            }
        });
    }

    // 下載segment主要方法
    public void downloadSegment(boolean init, MPD.S s, String videoMediaUrl, String audioMediaUrl) {
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
                    if (result != null && recording.get()) {
                        log.info(result);
                    }
                });
    }
}
