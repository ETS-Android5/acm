package org.literacybridge.acm.core;

import java.util.List;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Playlist;

public class DataRequestResult implements IDataRequestResult {
    private final Map<String, Integer> facetCounts;
    private final Map<String, Integer> languageFacetCounts;
    private final List<String> audioItems;
    private final Iterable<Playlist> tags;

    public DataRequestResult(Map<String, Integer> facetCounts,
            Map<String, Integer> languageFacetCounts, List<String> audioItems,
            Iterable<Playlist> tags) {
        this.facetCounts = facetCounts;
        this.languageFacetCounts = languageFacetCounts;
        this.audioItems = audioItems;
        this.tags = tags;
    }

    /* (non-Javadoc)
     * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getFacetCount(main.java.org.literacybridge.acm.categories.Taxonomy.Category)
     */
    public int getFacetCount(Category category) {
        if (category == null) {
            return 0;
        }
        Integer count = facetCounts.get(category.getUuid());
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    /* (non-Javadoc)
     * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getAudioItems()
     */
    public List<String> getAudioItems() {
        return audioItems;
    }

    @Override
    public int getLanguageFacetCount(String languageCode) {
        if (languageCode == null) {
            return 0;
        }
        Integer count = languageFacetCounts.get(languageCode);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    @Override
    public Iterable<Playlist> getTags() {
        return tags;
    }
}
