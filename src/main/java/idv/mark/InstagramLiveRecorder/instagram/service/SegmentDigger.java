package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.ThreadUtil;
import idv.mark.InstagramLiveRecorder.instagram.model.MPD;
import idv.mark.InstagramLiveRecorder.instagram.model.ParameterSetting;
import idv.mark.InstagramLiveRecorder.instagram.model.SegmentStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 挖掘器 (負責挖掘過去的直播)
 */
@Data
@Slf4j
@RequiredArgsConstructor
public class SegmentDigger {

    private final Supplier<Boolean> recording;
    private final Supplier<String> videoMediaUrl;
    private final Supplier<String> audioMediaUrl;
    private final String dashHost;
    private final ParameterSetting parameterSetting;
    private final InstagramHttpUtil instagramHttpUtil;

    // 在存放於現有的MPD duration中，觸發挖掘出過去的直播
    // 會依照觸發多至少的順序發送Http HEAD request, 404表示無, 200表示有
    // 有的話, 會將該segment加入到segmentsDownloadMap, 並且由偵測器下載該segment
    // 若全部都沒有, 則從小到大, 依序挖掘 (過往發送過的請求不加入挖掘順序)
    public CompletableFuture<Void> digitHistory(Map<MPD.S, SegmentStatus> segmentsDownloadMap) {
        return CompletableFuture.runAsync(() -> {
            while (!parameterSetting.isForceRecording()) {
                ThreadUtil.sleep(3000);
                // 樣本數沒有達到30個, 先繼續等到達到30個 (或是停止錄影)
                if (segmentsDownloadMap.keySet().size() > 30 || !recording.get()) {
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
            digit(segmentsDownloadMap, digitDurationFirstList, allDuration);
        });
    }

    // 挖掘過去的直播 (遞迴方法)
    private void digit(Map<MPD.S, SegmentStatus> segmentsDownloadMap, List<Long> digitDurationFirstList, List<Long> allDuration) {
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
                segmentsDownloadMap.put(digitSuccessSegment, new SegmentStatus(digitSegmentId, videoMediaUrl.get(), audioMediaUrl.get(), false, false));
                if (digitSuccessSegment.getT() > 4300) {
                    digit(segmentsDownloadMap, digitDurationFirstList, allDuration);
                } else {
                    log.info("digit done, last segmentId = {}", digitSuccessSegment.getT());
                }
                break;
            } else if (statusCode == 404 || statusCode == 410) {
                if (!recording.get()) {
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
        ThreadUtil.sleep(parameterSetting.getMAX_DIGIT_INTERVAL_MILLISECONDS());
        String digitURL = videoMediaUrl.get().replace("$Time$", String.valueOf(digitSegmentId)).replace("../", dashHost);
        return instagramHttpUtil.digitHasSource(digitURL);
    }
}
