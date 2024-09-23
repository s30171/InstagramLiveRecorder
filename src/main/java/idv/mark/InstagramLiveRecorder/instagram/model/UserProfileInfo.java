package idv.mark.InstagramLiveRecorder.instagram.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileInfo {
    private UserData data;
    private String status;

    @Data
    public static class UserData {
        private User user;
    }

    @Data
    public static class User {
        @SerializedName("ai_agent_type")
        private String aiAgentType;
        private String biography;

        @SerializedName("bio_links")
        private List<String> bioLinks;

        @SerializedName("fb_profile_biolink")
        private String fbProfileBioLink;

        @SerializedName("biography_with_entities")
        private BiographyWithEntities biographyWithEntities;

        @SerializedName("blocked_by_viewer")
        private boolean blockedByViewer;

        @SerializedName("restricted_by_viewer")
        private boolean restrictedByViewer;

        @SerializedName("country_block")
        private boolean countryBlock;

        @SerializedName("eimu_id")
        private String eimuId;

        @SerializedName("external_url")
        private String externalUrl;

        @SerializedName("external_url_linkshimmed")
        private String externalUrlLinkshimmed;

        @SerializedName("edge_followed_by")
        private EdgeFollowedBy edgeFollowedBy;

        private String fbid;

        @SerializedName("followed_by_viewer")
        private boolean followedByViewer;

        @SerializedName("edge_follow")
        private EdgeFollow edgeFollow;

        @SerializedName("follows_viewer")
        private boolean followsViewer;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("group_metadata")
        private String groupMetadata;

        @SerializedName("has_ar_effects")
        private boolean hasArEffects;

        @SerializedName("has_clips")
        private boolean hasClips;

        @SerializedName("has_guides")
        private boolean hasGuides;

        @SerializedName("has_chaining")
        private boolean hasChaining;

        @SerializedName("has_channel")
        private boolean hasChannel;

        @SerializedName("has_blocked_viewer")
        private boolean hasBlockedViewer;

        @SerializedName("highlight_reel_count")
        private int highlightReelCount;

        @SerializedName("has_requested_viewer")
        private boolean hasRequestedViewer;

        @SerializedName("hide_like_and_view_counts")
        private boolean hideLikeAndViewCounts;

        private String id;

        @SerializedName("is_business_account")
        private boolean isBusinessAccount;

        @SerializedName("is_professional_account")
        private boolean isProfessionalAccount;

        @SerializedName("is_supervision_enabled")
        private boolean isSupervisionEnabled;

        @SerializedName("is_guardian_of_viewer")
        private boolean isGuardianOfViewer;

        @SerializedName("is_supervised_by_viewer")
        private boolean isSupervisedByViewer;

        @SerializedName("is_supervised_user")
        private boolean isSupervisedUser;

        @SerializedName("is_embeds_disabled")
        private boolean isEmbedsDisabled;

        @SerializedName("is_joined_recently")
        private boolean isJoinedRecently;

        @SerializedName("guardian_id")
        private String guardianId;

        @SerializedName("business_address_json")
        private String businessAddressJson;

        @SerializedName("business_contact_method")
        private String businessContactMethod;

        @SerializedName("business_email")
        private String businessEmail;

        @SerializedName("business_phone_number")
        private String businessPhoneNumber;

        @SerializedName("business_category_name")
        private String businessCategoryName;

        @SerializedName("overall_category_name")
        private String overallCategoryName;

        @SerializedName("category_enum")
        private String categoryEnum;

        @SerializedName("category_name")
        private String categoryName;

        @SerializedName("is_private")
        private boolean isPrivate;

        @SerializedName("is_verified")
        private boolean isVerified;

        @SerializedName("is_verified_by_mv4b")
        private boolean isVerifiedByMv4b;

        @SerializedName("is_regulated_c18")
        private boolean isRegulatedC18;

        @SerializedName("edge_mutual_followed_by")
        private EdgeMutualFollowedBy edgeMutualFollowedBy;

        @SerializedName("pinned_channels_list_count")
        private int pinnedChannelsListCount;

        @SerializedName("profile_pic_url")
        private String profilePicUrl;

        @SerializedName("profile_pic_url_hd")
        private String profilePicUrlHd;

        @SerializedName("requested_by_viewer")
        private boolean requestedByViewer;

        @SerializedName("should_show_category")
        private boolean shouldShowCategory;

        @SerializedName("should_show_public_contacts")
        private boolean shouldShowPublicContacts;

        @SerializedName("show_account_transparency_details")
        private boolean showAccountTransparencyDetails;

        @SerializedName("transparency_label")
        private String transparencyLabel;

        @SerializedName("transparency_product")
        private String transparencyProduct;

        private String username;

        @SerializedName("connected_fb_page")
        private String connectedFbPage;
        private List<String> pronouns;

        @SerializedName("edge_owner_to_timeline_media")
        private EdgeOwnerToTimelineMedia edgeOwnerToTimelineMedia;
    }

    @Data
    public static class BiographyWithEntities {
        @SerializedName("raw_text")
        private String rawText;

        private List<Object> entities;
    }

    @Data
    public static class EdgeFollowedBy {
        private int count;
    }

    @Data
    public static class EdgeFollow {
        private int count;
    }

    @Data
    public static class EdgeMutualFollowedBy {
        private int count;
        private List<Object> edges;
    }

    @Data
    public static class EdgeOwnerToTimelineMedia {
        private int count;
        @SerializedName("page_info")
        private PageInfo pageInfo;
        private List<Object> edges;
    }

    @Data
    public static class PageInfo {
        @SerializedName("has_next_page")
        private boolean hasNextPage;

        @SerializedName("end_cursor")
        private String endCursor;
    }
}
