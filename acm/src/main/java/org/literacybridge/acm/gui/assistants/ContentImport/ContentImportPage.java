package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;

import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base class for Content Import Assistant pages. Contains code shared amongst the pages.
 *
 * @param <Context>
 */
abstract class ContentImportPage<Context> extends AssistantPage<Context> {
    static Color bgColor = Color.white; // table.getBackground();
    static Color bgSelectionColor = new JTable().getSelectionBackground();
    static Color bgAlternateColor = new Color(235, 245, 252);

    // Speaker with sound coming out of it.
    static ImageIcon soundImage = new ImageIcon(UIConstants.getResource("sound-1.png"));
    // Speaker with no sound coming out.
    static ImageIcon noSoundImage = new ImageIcon(UIConstants.getResource("sound-3.png"));

    Context context;
    private MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    ContentImportPage(Assistant.PageHelper<Context> listener) {
        super(listener);
        context = getContext();
    }

    /**
     * Given a message title (ie, from the Program Spec), see if we already have such an
     * audio item in the desired language.
     *
     * @param title        The title to search for.
     * @param languagecode The language in which we want the audio item.
     * @return the AudioItem if it exists, otherwise null.
     */
    AudioItem findAudioItemForTitle(String title, String languagecode) {
        List<Category> categoryList = new ArrayList<>();
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(languagecode).getLocale());

        SearchResult searchResult = store.search(title, categoryList, localeList);
        // Filter because search will return near matches.
        @SuppressWarnings("UnnecessaryLocalVariable")
        AudioItem item = searchResult.getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .filter(it -> it.getTitle().equals(title))
            .findAny()
            .orElse(null);
        return item;
    }

}
