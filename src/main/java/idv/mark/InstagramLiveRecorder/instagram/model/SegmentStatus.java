package idv.mark.InstagramLiveRecorder.instagram.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SegmentStatus {
    private long segmentId;
    private String videoMediaUrl;
    private String audioMediaUrl;
    private boolean isDownloaded;
    private boolean isDownloaderRunning;
}
