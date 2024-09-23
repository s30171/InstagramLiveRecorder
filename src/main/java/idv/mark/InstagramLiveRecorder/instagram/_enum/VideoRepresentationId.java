package idv.mark.InstagramLiveRecorder.instagram._enum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VideoRepresentationId {
    SOURCE("dash-lp-pst-v"),
    _720P_PLUS("dash-lp-hd2-v"),
    _480P("dash-lp-hd1-v"),
    _432P("dash-lp-hd-v"),
    _360P("dash-lp-md-v"),
    _216P("dash-lp-ld-v")
    ;

    private final String id;
}
