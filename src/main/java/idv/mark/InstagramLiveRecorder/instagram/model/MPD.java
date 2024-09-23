package idv.mark.InstagramLiveRecorder.instagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MPD {

    @JacksonXmlProperty(isAttribute = true)
    public String type;

    @JacksonXmlProperty(isAttribute = true, localName = "availabilityStartTime")
    public String availabilityStartTime;

    @JacksonXmlProperty(isAttribute = true, localName = "availabilityEndTime")
    public String availabilityEndTime;

    @JacksonXmlProperty(isAttribute = true, localName = "timeShiftBufferDepth")
    public String timeShiftBufferDepth;

    @JacksonXmlProperty(isAttribute = true, localName = "suggestedPresentationDelay")
    public String suggestedPresentationDelay;

    @JacksonXmlProperty(isAttribute = true, localName = "minBufferTime")
    public String minBufferTime;

    @JacksonXmlProperty(isAttribute = true, localName = "publishTime")
    public String publishTime;

    @JacksonXmlProperty(isAttribute = true, localName = "minimumUpdatePeriod")
    public String minimumUpdatePeriod;

    @JacksonXmlProperty(isAttribute = true, localName = "validationErrors")
    public String validationErrors;

    @JacksonXmlProperty(isAttribute = true, localName = "currentServerTimeMs")
    public long currentServerTimeMs;

    @JacksonXmlProperty(isAttribute = true, localName = "firstAvTimeMs")
    public long firstAvTimeMs;

    @JacksonXmlProperty(isAttribute = true, localName = "lastVideoFrameTs")
    public long lastVideoFrameTs;

    @JacksonXmlProperty(isAttribute = true, localName = "loapStreamId")
    public String loapStreamId;

    @JacksonXmlProperty(isAttribute = true, localName = "publishFrameTime")
    public long publishFrameTime;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Period> Period;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Period {
        @JacksonXmlProperty(isAttribute = true)
        public String id;

        @JacksonXmlProperty(isAttribute = true)
        public String start;

        @JacksonXmlElementWrapper(useWrapping = false)
        public List<AdaptationSet> AdaptationSet;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdaptationSet {
        @JacksonXmlProperty(isAttribute = true)
        public boolean segmentAlignment;

        @JacksonXmlProperty(isAttribute = true)
        public int maxWidth;

        @JacksonXmlProperty(isAttribute = true)
        public int maxHeight;

        @JacksonXmlProperty(isAttribute = true)
        public int maxFrameRate;

        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Representation> Representation;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Representation {
        @JacksonXmlProperty(isAttribute = true)
        public String id;

        @JacksonXmlProperty(isAttribute = true)
        public String mimeType;

        @JacksonXmlProperty(isAttribute = true)
        public String codecs;

        @JacksonXmlProperty(isAttribute = true)
        public int width;

        @JacksonXmlProperty(isAttribute = true)
        public int height;

        @JacksonXmlProperty(isAttribute = true)
        public int frameRate;

        @JacksonXmlProperty(isAttribute = true)
        public int bandwidth;

        @JacksonXmlProperty(isAttribute = true, localName = "FBMaxBandwidth")
        public int FBMaxBandwidth;

        @JacksonXmlProperty(isAttribute = true, localName = "FBPlaybackResolutionMos")
        public String FBPlaybackResolutionMos;

        @JacksonXmlProperty(isAttribute = true, localName = "FBQualityClass")
        public String FBQualityClass;

        @JacksonXmlProperty(isAttribute = true, localName = "FBQualityLabel")
        public String FBQualityLabel;

        public SegmentTemplate SegmentTemplate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentTemplate {
        @JacksonXmlProperty(isAttribute = true)
        public int presentationTimeOffset;

        @JacksonXmlProperty(isAttribute = true)
        public int timescale;

        @JacksonXmlProperty(isAttribute = true)
        public String initialization;

        @JacksonXmlProperty(isAttribute = true)
        public String media;

        public SegmentTimeline SegmentTimeline;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentTimeline {
        @JacksonXmlProperty(isAttribute = true, localName = "FBPredictedMedia")
        public String FBPredictedMedia;

        @JacksonXmlProperty(isAttribute = true, localName = "FBPredictedMediaEndNumber")
        public int FBPredictedMediaEndNumber;

        @JacksonXmlElementWrapper(useWrapping = false)
        public List<S> S;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S {
        @JacksonXmlProperty(isAttribute = true)
        public long t;
        @JacksonXmlProperty(isAttribute = true)
        public long d;
    }
}
