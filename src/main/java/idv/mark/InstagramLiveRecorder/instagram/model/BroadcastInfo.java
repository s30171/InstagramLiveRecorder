package idv.mark.InstagramLiveRecorder.instagram.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class BroadcastInfo {

    private String id;

    private String message;

    @SerializedName("published_time")
    private long publishedTime;

    @SerializedName("broadcast_prompt")
    private String broadcastPrompt;

    @SerializedName("broadcast_message")
    private String broadcastMessage;

    private Dimensions dimensions;

    @SerializedName("hide_from_feed_unit")
    private boolean hideFromFeedUnit;

    @SerializedName("is_live_comment_mention_enabled")
    private boolean isLiveCommentMentionEnabled;

    @SerializedName("is_live_comment_replies_enabled")
    private boolean isLiveCommentRepliesEnabled;

    @SerializedName("media_id")
    private String mediaId;

    @SerializedName("response_timestamp")
    private long responseTimestamp;

    @SerializedName("str_id")
    private String strId;

    @SerializedName("strong_id__")
    private String strongId;

    @SerializedName("broadcast_owner")
    private BroadcastOwner broadcastOwner;

    @SerializedName("dimensions_typed")
    private Dimensions dimensionsTyped;

    @SerializedName("cover_frame_url")
    private String coverFrameUrl;

    @SerializedName("video_duration")
    private double videoDuration;

    @SerializedName("internal_only")
    private boolean internalOnly;

    @SerializedName("is_viewer_comment_allowed")
    private boolean isViewerCommentAllowed;

    @SerializedName("broadcast_experiments")
    private BroadcastExperiments broadcastExperiments;

    @SerializedName("live_post_id")
    private String livePostId;

    @SerializedName("is_player_live_trace_enabled")
    private int isPlayerLiveTraceEnabled;

    private int visibility;

    @SerializedName("media_overlay_info")
    private MediaOverlayInfo mediaOverlayInfo;

    private List<Object> cobroadcasters;

    @SerializedName("organic_tracking_token")
    private String organicTrackingToken;

    @SerializedName("dash_playback_url")
    private String dashPlaybackUrl;

    @SerializedName("dash_abr_playback_url")
    private String dashAbrPlaybackUrl;

    @SerializedName("viewer_count")
    private double viewerCount;

    @SerializedName("broadcast_status")
    private String broadcastStatus;

    private String status;

    @Data
    public static class Dimensions {
        private int height;
        private int width;
    }

    @Data
    public static class BroadcastOwner {
        private String pk;

        @SerializedName("pk_id")
        private String pkId;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("is_private")
        private boolean isPrivate;

        @SerializedName("strong_id__")
        private String strongId;

        private String id;
        private String username;

        @SerializedName("is_verified")
        private boolean isVerified;

        @SerializedName("live_broadcast_id")
        private String liveBroadcastId;

        @SerializedName("live_broadcast_visibility")
        private int liveBroadcastVisibility;

        @SerializedName("profile_pic_id")
        private String profilePicId;

        @SerializedName("profile_pic_url")
        private String profilePicUrl;

        @SerializedName("live_subscription_status")
        private String liveSubscriptionStatus;

        @SerializedName("interop_messaging_user_fbid")
        private String interopMessagingUserFbid;

        @SerializedName("friendship_status")
        private FriendshipStatus friendshipStatus;
    }

    @Data
    public static class FriendshipStatus {
        private boolean blocking;
        private boolean followedBy;
        private boolean following;
        private boolean incomingRequest;

        @SerializedName("is_bestie")
        private boolean isBestie;

        @SerializedName("is_eligible_to_subscribe")
        private boolean isEligibleToSubscribe;

        @SerializedName("is_feed_favorite")
        private boolean isFeedFavorite;

        @SerializedName("is_private")
        private boolean isPrivate;

        @SerializedName("is_restricted")
        private boolean isRestricted;

        private boolean muting;
        private boolean outgoingRequest;
        private boolean subscribed;
    }

    @Data
    public static class BroadcastExperiments {
        @SerializedName("ig_live_audio_video_toggle")
        private IgLiveAudioVideoToggle igLiveAudioVideoToggle;

        @SerializedName("ig_live_use_rsys_rtc_infra")
        private IgLiveUseRsysRtcInfra igLiveUseRsysRtcInfra;

        @SerializedName("ig_live_upvoteable_qa")
        private IgLiveUpvoteableQa igLiveUpvoteableQa;

        @SerializedName("ig_live_viewer_to_viewer_waves")
        private IgLiveViewerToViewerWaves igLiveViewerToViewerWaves;

        @SerializedName("ig_live_emoji_reactions")
        private IgLiveEmojiReactions igLiveEmojiReactions;

        @SerializedName("ig_live_friend_chat")
        private IgLiveFriendChat igLiveFriendChat;

        @SerializedName("ig_live_halo_call_controls")
        private IgLiveHaloCallControls igLiveHaloCallControls;

        @SerializedName("ig_live_badges_ufi")
        private IgLiveBadgesUfi igLiveBadgesUfi;

        @SerializedName("ig_live_share_system_comment")
        private IgLiveShareSystemComment igLiveShareSystemComment;

        @SerializedName("ig_live_comment_interactions")
        private IgLiveCommentInteractions igLiveCommentInteractions;

        @SerializedName("ig_live_comment_subscription")
        private IgLiveCommentSubscription igLiveCommentSubscription;

        @SerializedName("ig_live_viewer_redesign_broadcaster_v1")
        private IgLiveViewerRedesignBroadcasterV1 igLiveViewerRedesignBroadcasterV1;

        @SerializedName("ig_allow_4p_live_with")
        private IgAllow4pLiveWith igAllow4pLiveWith;

        @SerializedName("ig_live_invite_only")
        private IgLiveInviteOnly igLiveInviteOnly;

        @SerializedName("ig_live_bff_upsell")
        private IgLiveBffUpsell igLiveBffUpsell;

        @SerializedName("ig_live_android_games")
        private IgLiveAndroidGames igLiveAndroidGames;
    }

    @Data
    public static class IgLiveAudioVideoToggle {
        @SerializedName("video_toggle_enabled")
        private boolean videoToggleEnabled;

        @SerializedName("audio_toggle_enabled")
        private boolean audioToggleEnabled;
    }

    @Data
    public static class IgLiveUseRsysRtcInfra {
        private boolean enabled;
    }

    @Data
    public static class IgLiveUpvoteableQa {
        private boolean enabled;
    }

    @Data
    public static class IgLiveViewerToViewerWaves {
        private boolean enabled;
    }

    @Data
    public static class IgLiveEmojiReactions {
        @SerializedName("is_host_enabled")
        private boolean isHostEnabled;

        @SerializedName("use_emoji_set_2")
        private boolean useEmojiSet2;
    }

    @Data
    public static class IgLiveFriendChat {
        @SerializedName("is_enabled_for_broadcast")
        private boolean isEnabledForBroadcast;
    }

    @Data
    public static class IgLiveHaloCallControls {
        @SerializedName("tap_to_show_pill_enabled")
        private boolean tapToShowPillEnabled;

        @SerializedName("tap_state_bottom_call_controls_enabled")
        private boolean tapStateBottomCallControlsEnabled;

        @SerializedName("tap_state_animation_enabled")
        private boolean tapStateAnimationEnabled;
    }

    @Data
    public static class IgLiveBadgesUfi {
        @SerializedName("badges_always_on_enabled")
        private boolean badgesAlwaysOnEnabled;
    }

    @Data
    public static class IgLiveShareSystemComment {
        @SerializedName("join_request_system_comment_delay_5_else_0")
        private boolean joinRequestSystemCommentDelay5Else0;

        @SerializedName("share_system_comment_delay_10_else_5")
        private boolean shareSystemCommentDelay10Else5;

        @SerializedName("show_join_request_system_comment")
        private boolean showJoinRequestSystemComment;

        @SerializedName("show_share_system_comment")
        private boolean showShareSystemComment;
    }

    @Data
    public static class IgLiveCommentInteractions {
        @SerializedName("android_is_required_mvvm_enabled")
        private boolean androidIsRequiredMvvmEnabled;

        @SerializedName("is_host_comment_reply_redesign_enabled")
        private boolean isHostCommentReplyRedesignEnabled;

        @SerializedName("is_host_comment_liking_enabled")
        private boolean isHostCommentLikingEnabled;

        @SerializedName("is_broadcast_level_expand_enabled")
        private boolean isBroadcastLevelExpandEnabled;
    }

    @Data
    public static class IgLiveCommentSubscription {
        @SerializedName("is_broadcast_enabled")
        private boolean isBroadcastEnabled;

        @SerializedName("dont_change_comments_height")
        private boolean dontChangeCommentsHeight;
    }

    @Data
    public static class IgLiveViewerRedesignBroadcasterV1 {
        @SerializedName("aspect_ratio_change_enabled")
        private boolean aspectRatioChangeEnabled;

        @SerializedName("is_aspect_ratio_16_9")
        private boolean isAspectRatio169;

        @SerializedName("comment_redesign_combined_test_enabled")
        private boolean commentRedesignCombinedTestEnabled;

        @SerializedName("viewer_redesign_combined_test_enabled")
        private boolean viewerRedesignCombinedTestEnabled;

        @SerializedName("viewer_redesign_v2_combined_test_enabled")
        private boolean viewerRedesignV2CombinedTestEnabled;
    }

    @Data
    public static class IgAllow4pLiveWith {
        private boolean allow;
    }

    @Data
    public static class IgLiveInviteOnly {
        @SerializedName("is_invite_only_branding_enabled")
        private boolean isInviteOnlyBrandingEnabled;
    }

    @Data
    public static class IgLiveBffUpsell {
        @SerializedName("show_join_live_sheet")
        private boolean showJoinLiveSheet;
    }

    @Data
    public static class IgLiveAndroidGames {
        @SerializedName("viewer_poll_enabled")
        private boolean viewerPollEnabled;
    }

    @Data
    public static class MediaOverlayInfo {
        // Fields for MediaOverlayInfo (if any) can go here
    }
}
