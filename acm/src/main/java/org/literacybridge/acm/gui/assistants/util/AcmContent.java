package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Class to hold information about the structure of content in playlists, in a deployment,
 * as deduced from the playlists in the ACM.
 */
public class AcmContent {
    static class EnumerationAdapter<from, to> implements Enumeration<to> {
        Enumeration<from> sourceEnumeration;
        public EnumerationAdapter(Enumeration<from> source) {
            sourceEnumeration = source;
        }

        @Override
        public boolean hasMoreElements() {
            return sourceEnumeration.hasMoreElements();
        }

        @Override
        @SuppressWarnings("unchecked")
        public to nextElement() {
            return (to)sourceEnumeration.nextElement();
        }
    }

    public static class AcmRootNode extends DefaultMutableTreeNode {

        @SuppressWarnings("unchecked")
        public Enumeration<LanguageNode> getChildren() {
            return new EnumerationAdapter<TreeNode, LanguageNode>(super.children());
        }
        public LanguageNode find(String languagecode) {
            Enumeration<LanguageNode> children = getChildren();
            while (children.hasMoreElements()) {
                LanguageNode languageNode = children.nextElement();
                if (languageNode.getLanguageCode().equals(languagecode))
                    return languageNode;
            }
            return null;
        }

        public List<LanguageNode> getLanguageNodes() {
            // Fortunately this is pretty low volume.
            List<LanguageNode> result = new ArrayList<>();
            if (super.children != null) {
                for (Object tn : super.children) {
                    result.add((LanguageNode)tn);
                }
            }
            return result;
        }

        public LanguageNode getLanguageNode(String languagecode) {
            for (LanguageNode child : getLanguageNodes()) {
                if (languagecode.equalsIgnoreCase(child.getLanguageCode()))
                    return child;
            }
            return null;
        }
    }

    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        final String languagename;
        public LanguageNode(String languagecode) {
            super(languagecode);
            this.languagename = ACMConfiguration
                .getInstance().getCurrentDB().getLanguageLabel(new Locale(languagecode));
        }
        public String getLanguageCode() {
            return (String)getUserObject();
        }
        public String toString() {
            return String.format("%s (%s)", languagename, getLanguageCode());
        }

        @SuppressWarnings("unchecked")
        public Enumeration<PlaylistNode> getChildren() {
            return new EnumerationAdapter<TreeNode, PlaylistNode>(super.children());
        }
        public PlaylistNode find(String title) {
            Enumeration<PlaylistNode> children = getChildren();
            while (children.hasMoreElements()) {
                PlaylistNode playlistNode = children.nextElement();
                if (playlistNode.getTitle().equals(title))
                    return playlistNode;
            }
            return null;
        }

        public List<PlaylistNode> getPlaylistNodes() {
            List<PlaylistNode> result = new ArrayList<>();
            if (super.children != null) {
                for (Object tn : super.children) {
                    result.add((PlaylistNode)tn);
                }
            }
            return result;
        }
    }

    /**
     * Node class for a Playlist. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        public PlaylistNode(Playlist playlist) {
            super(playlist);
        }
        public Playlist getPlaylist() {
            return (Playlist)getUserObject();
        }
        public String toString() {
            return AssistantPage.undecoratedPlaylistName(getPlaylist().getName());
        }
        public String getTitle() {
            return AssistantPage.undecoratedPlaylistName(getPlaylist().getName());
        }
        public String getDecoratedTitle() {
            return getPlaylist().getName();
        }

        @SuppressWarnings("unchecked")
        public Enumeration<AudioItemNode> getChildren() {
            return new EnumerationAdapter<TreeNode, AudioItemNode>(super.children());
        }

        public List<AudioItemNode> getAudioItemNodes() {
            List<AudioItemNode> result = new ArrayList<>();
            if (super.children != null) {
                for (Object tn : super.children) {
                    result.add((AudioItemNode)tn);
                }
            }
            return result;
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlist. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class AudioItemNode extends DefaultMutableTreeNode {
        public AudioItemNode(AudioItemNode other) {this(other.getAudioItem());}
        public AudioItemNode(AudioItem item) {
            super(item);
        }
        public AudioItem getAudioItem() {
            return (AudioItem)getUserObject();
        }
        public String toString() {
            return getAudioItem().getTitle();
        }
    }
}
