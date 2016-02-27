package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Playlist extends Persistable {
    // TODO: make uuid final once deprecated method setUuid() is removed
    private String uuid;
    private String name;
    private final List<String> audioItems;

    public Playlist(String uuid) {
        this.uuid = uuid;
        audioItems = Lists.newArrayList();
    }

    @Deprecated
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAudioItem(String uuid) {
        audioItems.add(uuid);
    }

    public void removeAudioItem(String uuid) {
        audioItems.remove(uuid);
    }

    public int getAudioItemPosition(String uuid) {
        return audioItems.indexOf(uuid);
    }

    public Iterable<String> getAudioItemList() {
        return audioItems;
    }

    public int getNumAudioItems() {
        return audioItems.size();
    }

    @Override
    public void commit(Transaction t) throws IOException {
        t.getIndex().updatePlaylistName(this, t);
    }

    @Override
    public void rollback(Transaction t) throws IOException {
        t.getIndex().refresh(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SortedMap<Integer, String> items = Maps.newTreeMap();
        private String uuid;
        private String name;
        private Playlist playlistPrototype;

        private Builder() {}

        public Builder withUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPlaylistPrototype(Playlist playlist) {
            this.playlistPrototype = playlist;
            return this;
        }

        public Builder addAudioItem(String uuid, int position) {
            items.put(position, uuid);
            return this;
        }

        public Playlist build() {
            final Playlist playlist = (playlistPrototype == null) ? new Playlist(uuid) : playlistPrototype;
            playlist.audioItems.clear();
            playlist.setName(name);
            for (String audioItem : items.values()) {
                playlist.addAudioItem(audioItem);
            }
            return playlist;
        }
    }
}
