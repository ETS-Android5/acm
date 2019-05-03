package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingsMatcher;
import org.literacybridge.acm.gui.assistants.common.AbstractFilesPage;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is the context for a content import. By the time the Assistant has finished, this will
 * contain all of the data for importing the audio tracks for one language in one deployment.
 */
class ContentImportContext implements AbstractFilesPage.FileImportContext, AbstractMatchPage.MatchContext {
    // Configured items

    /**
     * Value to use for the fuzzy matching threshold.
     */
    Integer fuzzyThreshold;

    /**
     * email addresses to be notified about the import.
     */
    Collection<String> notifyList;

    /**
     * Program Spec for the project.
     */
    ProgramSpec programSpec;

    // From the Welcome page

    /**
     * The deployment for which audio is to be imported.
     */
    int deploymentNo = -1;
    /**
     * The language code for which audio is to be imported.
     */
    String languagecode;

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    final Set<File> importableRoots = new LinkedHashSet<>();
    final Set<File> importableFiles = new LinkedHashSet<>();

    // Accessors to satisfy FileImportContext.
    @Override
    public Set<File> getImportableRoots() {
        return importableRoots;
    }
    @Override
    public Set<File> getImportableFiles() {
        return importableFiles;
    }

    // From the Match page

    /**
     * The matcher object. Lets us know how titles were matched against files.
     */
    AudioMatcher matcher = new AudioMatcher();
    @Override
    public AudioMatcher getMatcher() {
        return matcher;
    }

    /**
     * Created playlists, in the ACM.
     */
    Set<String> createdPlaylists = new LinkedHashSet<>();


}
