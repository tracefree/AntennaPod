package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Item (episode) within a feed.
 *
 * @author daniel
 */
public class FeedItem implements Serializable {

    /** tag that indicates this item is in the queue */
    public static final String TAG_QUEUE = "Queue";
    /** tag that indicates this item is in favorites */
    public static final String TAG_FAVORITE = "Favorite";

    private long id;
    /**
     * The id/guid that can be found in the rss/atom feed. Might not be set.
     */
    private String itemIdentifier;
    private String title;
    /**
     * The description of a feeditem.
     */
    private String description;

    private String link;
    private Date pubDate;
    private FeedMedia media;

    private transient Feed feed;
    private long feedId;
    private String podcastIndexChapterUrl;
    private String socialInteractUrl;
    private String podcastIndexTranscriptUrl;
    private String podcastIndexTranscriptType;
    private Transcript transcript;

    private int state;
    public static final int NEW = -1;
    public static final int UNPLAYED = 0;
    public static final int PLAYED = 1;

    private String paymentLink;

    /**
     * Is true if the database contains any chapters that belong to this item. This attribute is only
     * written once by DBReader on initialization.
     * The FeedItem might still have a non-null chapters value. In this case, the list of chapters
     * has not been saved in the database yet.
     * */
    private final boolean hasChapters;

    /**
     * The list of chapters of this item. This might be null even if there are chapters of this item
     * in the database. The 'hasChapters' attribute should be used to check if this item has any chapters.
     * */
    private transient List<Chapter> chapters;
    private String imageUrl;

    private boolean autoDownloadEnabled = true;

    /**
     * Any tags assigned to this item
     */
    private final Set<String> tags = new HashSet<>();

    public FeedItem() {
        this.state = UNPLAYED;
        this.hasChapters = false;
    }

    /**
     * This constructor is used by DBReader.
     * */
    public FeedItem(long id, String title, String link, Date pubDate, String paymentLink, long feedId,
                    boolean hasChapters, String imageUrl, int state,
                    String itemIdentifier, boolean autoDownloadEnabled, String podcastIndexChapterUrl,
                    String transcriptType, String transcriptUrl, String socialInteractUrl) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.paymentLink = paymentLink;
        this.feedId = feedId;
        this.hasChapters = hasChapters;
        this.imageUrl = imageUrl;
        this.state = state;
        this.itemIdentifier = itemIdentifier;
        this.autoDownloadEnabled = autoDownloadEnabled;
        this.podcastIndexChapterUrl = podcastIndexChapterUrl;
        this.socialInteractUrl = socialInteractUrl;
        if (transcriptUrl != null) {
            this.podcastIndexTranscriptUrl = transcriptUrl;
            this.podcastIndexTranscriptType = transcriptType;
        }
    }

    /**
     * This constructor should be used for creating test objects.
     */
    public FeedItem(long id, String title, String itemIdentifier, String link, Date pubDate, int state, Feed feed) {
        this.id = id;
        this.title = title;
        this.itemIdentifier = itemIdentifier;
        this.link = link;
        this.pubDate = (pubDate != null) ? (Date) pubDate.clone() : null;
        this.state = state;
        this.feed = feed;
        this.hasChapters = false;
    }

    /**
     * This constructor should be used for creating test objects involving chapter marks.
     */
    public FeedItem(long id, String title, String itemIdentifier, String link, Date pubDate,
                    int state, Feed feed, boolean hasChapters) {
        this.id = id;
        this.title = title;
        this.itemIdentifier = itemIdentifier;
        this.link = link;
        this.pubDate = (pubDate != null) ? (Date) pubDate.clone() : null;
        this.state = state;
        this.feed = feed;
        this.hasChapters = hasChapters;
    }

    public void updateFromOther(FeedItem other) {
        if (other.imageUrl != null) {
            this.imageUrl = other.imageUrl;
        }
        if (other.title != null) {
            title = other.title;
        }
        if (other.getDescription() != null) {
            description = other.getDescription();
        }
        if (other.link != null) {
            link = other.link;
        }
        if (other.pubDate != null && !other.pubDate.equals(pubDate)) {
            pubDate = other.pubDate;
        }
        if (other.media != null) {
            if (media == null) {
                setMedia(other.media);
                // reset to new if feed item did link to a file before
                setNew();
            } else if (media.compareWithOther(other.media)) {
                media.updateFromOther(other.media);
            }
        }
        if (other.paymentLink != null) {
            paymentLink = other.paymentLink;
        }
        if (other.chapters != null) {
            if (!hasChapters) {
                chapters = other.chapters;
            }
        }
        if (other.podcastIndexChapterUrl != null) {
            podcastIndexChapterUrl = other.podcastIndexChapterUrl;
        }
        if (other.socialInteractUrl != null) {
            socialInteractUrl = other.socialInteractUrl;
        }
        if (other.getTranscriptUrl() != null) {
            podcastIndexTranscriptUrl = other.podcastIndexTranscriptUrl;
        }
        if (other.getTranscriptType() != null) {
            podcastIndexTranscriptType = other.podcastIndexTranscriptType;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        if (this.media != null) {
            media.setItemId(id);
        }
    }

    /**
     * Returns the value that uniquely identifies this FeedItem. If the
     * itemIdentifier attribute is not null, it will be returned. Else it will
     * try to return the title. If the title is not given, it will use the link
     * of the entry.
     */
    public String getIdentifyingValue() {
        if (itemIdentifier != null && !itemIdentifier.isEmpty()) {
            return itemIdentifier;
        } else if (title != null && !title.isEmpty()) {
            return title;
        } else if (hasMedia() && media.getDownloadUrl() != null) {
            return media.getDownloadUrl();
        } else {
            return link;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    /**
     * Get the link for the feed item for the purpose of Share.
     * It falls backs to the feed's link if the item has no link.
     */
    public String getLinkWithFallback() {
        if (StringUtils.isNotBlank(link)) {
            return link;
        } else if (StringUtils.isNotBlank(getFeed().getLink())) {
            return getFeed().getLink();
        }
        return null;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getPubDate() {
        if (pubDate != null) {
            return (Date) pubDate.clone();
        } else {
            return null;
        }
    }

    public void setPubDate(Date pubDate) {
        if (pubDate != null) {
            this.pubDate = (Date) pubDate.clone();
        } else {
            this.pubDate = null;
        }
    }

    @Nullable
    public FeedMedia getMedia() {
        return media;
    }

    /**
     * Sets the media object of this FeedItem. If the given
     * FeedMedia object is not null, it's 'item'-attribute value
     * will also be set to this item.
     */
    public void setMedia(FeedMedia media) {
        this.media = media;
        if (media != null && media.getItem() != this) {
            media.setItem(this);
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public boolean isNew() {
        return state == NEW;
    }

    public int getPlayState() {
        return state;
    }

    public void setNew() {
        state = NEW;
    }

    public boolean isPlayed() {
        return state == PLAYED;
    }

    public void setPlayed(boolean played) {
        if (played) {
            state = PLAYED;
        } else {
            state = UNPLAYED;
        }
    }

    public boolean isInProgress() {
        return (media != null && media.isInProgress());
    }

    /**
     * Updates this item's description property if the given argument is longer than the already stored description
     * @param newDescription The new item description, content:encoded, itunes:description, etc.
     */
    public void setDescriptionIfLonger(String newDescription) {
        if (newDescription == null) {
            return;
        }
        if (this.description == null) {
            this.description = newDescription;
        } else if (this.description.length() < newDescription.length()) {
            this.description = newDescription;
        }
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public String getItemIdentifier() {
        return itemIdentifier;
    }

    public void setItemIdentifier(String itemIdentifier) {
        this.itemIdentifier = itemIdentifier;
    }

    public boolean hasMedia() {
        return media != null;
    }

    public String getImageLocation() {
        if (imageUrl != null) {
            return imageUrl;
        } else if (media != null && media.hasEmbeddedPicture()) {
            return FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER + media.getLocalFileUrl();
        } else if (feed != null) {
            return feed.getImageUrl();
        } else {
            return null;
        }
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    /**
     * Returns the image of this item, as specified in the feed.
     * To load the image that can be displayed to the user, use {@link #getImageLocation},
     * which also considers embedded pictures or the feed picture if no other picture is present.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean hasChapters() {
        return hasChapters;
    }

    public void disableAutoDownload() {
        this.autoDownloadEnabled = false;
    }

    public boolean isAutoDownloadEnabled() {
        return this.autoDownloadEnabled;
    }

    public boolean isDownloaded() {
        return media != null && media.isDownloaded();
    }

    /**
     * @return true if the item has this tag
     */
    public boolean isTagged(String tag) {
        return tags.contains(tag);
    }

    /**
     * @param tag adds this tag to the item. NOTE: does NOT persist to the database
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * @param tag the to remove
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public String getPodcastIndexChapterUrl() {
        return podcastIndexChapterUrl;
    }

    public void setPodcastIndexChapterUrl(String url) {
        podcastIndexChapterUrl = url;
    }

    public void setSocialInteractUrl(String url) {
        socialInteractUrl = url;
    }

    public String getSocialInteractUrl() {
        return socialInteractUrl;
    }

    public void setTranscriptUrl(String type, String url) {
        updateTranscriptPreferredFormat(type, url);
    }

    public String getTranscriptUrl() {
        return podcastIndexTranscriptUrl;
    }

    public String getTranscriptType() {
        return podcastIndexTranscriptType;
    }

    public void updateTranscriptPreferredFormat(String typeStr, String url) {
        if (StringUtils.isEmpty(typeStr) || StringUtils.isEmpty(url)) {
            return;
        }
        TranscriptType type = TranscriptType.fromMime(typeStr);
        TranscriptType previousType = TranscriptType.fromMime(podcastIndexTranscriptType);
        if (type.priority > previousType.priority) {
            podcastIndexTranscriptUrl = url;
            podcastIndexTranscriptType = type.canonicalMime;
        }
    }

    public Transcript getTranscript() {
        return transcript;
    }

    public void setTranscript(Transcript t) {
        transcript = t;
    }

    public boolean hasTranscript() {
        return (podcastIndexTranscriptUrl != null);
    }

    @NonNull
    @Override
    public String toString() {
        return "FeedItem [id=" + id + ", title=" + title + ", feedId=" + feedId + ", pubDate=" + pubDate + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FeedItem feedItem = (FeedItem) o;
        return id == feedItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
