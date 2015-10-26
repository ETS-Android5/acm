package org.literacybridge.acm.store;

import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.db.Taxonomy.Category;

public abstract class MetadataStore {
    public abstract AudioItem newAudioItem(String uid);
    public abstract AudioItem getAudioItem(String uid);
    public abstract Iterable<AudioItem> getAudioItems();
    public abstract Iterable<AudioItem> search(String searchFilter, List<Category> categories, List<Locale> locales);
    public abstract Iterable<AudioItem> search(String searchFilter, Playlist selectedTag);

    public abstract Playlist newPlaylist(String uid);
    public abstract Playlist getPlaylist(String uid);
    public abstract Iterable<Playlist> getPlaylists();
}
