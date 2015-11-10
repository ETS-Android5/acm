package org.literacybridge.acm.gui.util.language;

import java.util.Locale;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;

public class LanguageUtil {

    // current default language for items shown in the UI
    private static Locale defaultLanguage = Locale.ENGLISH;

    // fallback language if default language is not available for a certain item
    private static Locale fallbackLanguage = Locale.ENGLISH;

    // UI language
    private static Locale uiLanguage = Locale.ENGLISH;

    // The language that the user has chosen to show the name of a !LocalizedAudioItems!
    // DO NOT USE FOR UI CONTROLS
    public static Locale getUserChoosenLanguage() {
        return defaultLanguage;
    }

    public static void setUserChoosenLanguage(Locale newDefaultLanguage) {
        defaultLanguage = newDefaultLanguage;
    }

    public static Locale GetFallbackLanguage() {
        return fallbackLanguage;
    }

    public static void setFallbackLanguage(Locale newFallbackLanguage) {
        fallbackLanguage = newFallbackLanguage;
    }

    public static String getBestMatchingLocalizedLabel(Category category, Locale wish) {
        return category.getCategoryName();
    }

    public static Locale getUILanguage() {
        return uiLanguage;
    }

    public static void setUILanguage(Locale newUILocale) {
        uiLanguage = newUILocale;
    }

    public static String getLocalizedLanguageName(Locale locale) {
        String label = ACMConfiguration.getCurrentDB().getLanguageLabel(locale);
        if (label == null) {
            label = locale.getDisplayLanguage(getUILanguage());
        }
        return label;
    }

}
