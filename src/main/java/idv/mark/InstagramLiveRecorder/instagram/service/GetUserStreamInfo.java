package idv.mark.InstagramLiveRecorder.instagram.service;

import com.google.gson.Gson;
import idv.mark.InstagramLiveRecorder.instagram.model.BroadcastInfo;
import idv.mark.InstagramLiveRecorder.instagram.model.UserProfileInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

@Data
@Slf4j
public class GetUserStreamInfo {

    private String broadcastUserId;
    private String broadcastUserAccountName;
    private String csrfToken;
    private String sessionId;
    public static final String GET_WEB_INFO_URL = "https://i.instagram.com/api/v1/live/web_info/?target_user_id=%s";
    public static final String GET_USER_PROFILE_INFO_URL = "https://i.instagram.com/api/v1/users/web_profile_info/?username=%s";
    private InstagramHttpUtil instagramHttpUtil = new InstagramHttpUtil();

    public GetUserStreamInfo(String broadcastUserAccountName, String csrfToken, String sessionId) {
        this.broadcastUserAccountName = broadcastUserAccountName;
        this.csrfToken = csrfToken;
        this.sessionId = sessionId;
    }

    public String getDashPlaybackUrlByWebInfoApi() {
        if (StringUtils.isAllBlank(this.broadcastUserId) && StringUtils.isNotBlank(this.broadcastUserAccountName)) {
            UserProfileInfo userProfileInfo = getUserProfileInfo(this.broadcastUserAccountName);
            this.broadcastUserId = userProfileInfo.getData().getUser().getId();
        }
        if (StringUtils.isAllBlank(broadcastUserId)) {
            throw new RuntimeException("broadcastUserId is blank");
        }
        String url = String.format(GET_WEB_INFO_URL, broadcastUserId);
        String body = instagramHttpUtil.getHttpRequestWithSessionHeader(url);
        BroadcastInfo broadcastInfo = new Gson().fromJson(body, BroadcastInfo.class);
        if ("fail".equals(broadcastInfo.getStatus())) {
            throw new RuntimeException("broadcastInfo status is fail, message: " + broadcastInfo.getMessage());
        }
        return broadcastInfo.getDashAbrPlaybackUrl();
    }

    public UserProfileInfo getUserProfileInfo(String broadcastUserAccountName) {
        Map<String, String> withSessionApiHeaders = instagramHttpUtil.getWITH_SESSION_API_HEADERS();
        if (!withSessionApiHeaders.containsKey("x-csrftoken")) {
            if (StringUtils.isAllBlank(this.csrfToken)) {
                throw new RuntimeException("csrfToken is blank");
            }
            withSessionApiHeaders.put("x-csrftoken", this.csrfToken);
        }
        if (!withSessionApiHeaders.containsKey("cookie")) {
            if (StringUtils.isAllBlank(this.sessionId)) {
                throw new RuntimeException("sessionId is blank");
            }
            withSessionApiHeaders.put("cookie", "sessionid=" + this.sessionId);
        }
        if (StringUtils.isAllBlank(broadcastUserAccountName)) {
            throw new RuntimeException("broadcastUserAccountName is blank");
        }
        String url = String.format(GET_USER_PROFILE_INFO_URL, broadcastUserAccountName);
        String body = instagramHttpUtil.getHttpRequestWithSessionHeader(url);
        UserProfileInfo userProfileInfo = new Gson().fromJson(body, UserProfileInfo.class);
        log.info(Objects.isNull(userProfileInfo) ? "userProfileInfo is null" : String.format("userProfileInfo id: %s", userProfileInfo.getData().getUser().getId()));
        return userProfileInfo;
    }

}
