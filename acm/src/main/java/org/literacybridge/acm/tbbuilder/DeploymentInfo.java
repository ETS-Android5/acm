package org.literacybridge.acm.tbbuilder;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains all the information needed to create a deployment.
 *
 * To use:
 * - Create a DeploymentInfo, set options for the deployment (UF hidden, has tutorial)
 * - Create Packages for the deployment, with options (has intro)
 * - Add the playlists to the packages, in their order of appearance. For each playlist,
 *     add the announcement and invitation, and note if it is "user feedback".
 * - Add the content to the playlists, in their order of appearance
 */
public class DeploymentInfo {
    private final String programid;
    private final int deploymentNumber;
    private boolean ufHidden;
    private boolean hasTutorial;

    private final List<PackageInfo> packages = new ArrayList<>();

    public DeploymentInfo(String programid, int deploymentNumber) {
        this.programid = programid;
        this.deploymentNumber = deploymentNumber;
    }

    public boolean isUfHidden() { return ufHidden; }
    public DeploymentInfo ufHidden() { return setUfHidden(true); }
    public DeploymentInfo setUfHidden(boolean ufHidden) {
        this.ufHidden = ufHidden;
        return this;
    }

    public boolean hasTutorial() { return hasTutorial; }
    public DeploymentInfo setTutorial(boolean hasTutorial) {
        this.hasTutorial = hasTutorial;
        return this;
    }

    public PackageInfo addPackage(String languageCode, String variant) {
        PackageInfo packageInfo = new PackageInfo(languageCode, variant);
        packages.add(packageInfo);
        return packageInfo;
    }

    /**
     * Removes empty packages. An empty package is one which has no playlists, or only
     * empty playlists. An empty playlist is non-feedback one with no messages.
     */
    public DeploymentInfo prune() {
        List<PackageInfo> pruned = packages.stream()
            .map(PackageInfo::prune)
            .filter(PackageInfo::isNotEmpty)
            .collect(Collectors.toList());
        packages.clear();
        packages.addAll(pruned);
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s-%d", programid, deploymentNumber);
    }

    public Iterable<? extends PackageInfo> getPackages() {
        return packages;
    }

    public class PackageInfo {
        final String languageCode;
        final String variant;
        final String name;
        final String shortName;
        private AudioItem introMessage;
        private boolean hasUserFeedbackPlaylist;
        private boolean hasTutorial;

        final List<PlaylistInfo> playlists = new ArrayList<>();

        private PackageInfo(String languageCode, String variant) {
            this.languageCode = languageCode;
            this.variant = variant;
            this.name = makeName(false);
            this.shortName = makeName(true);
        }

        public PackageInfo withIntro(AudioItem introMessage) {
            this.introMessage = introMessage;
            return this;
        }
        public boolean hasIntro() { return introMessage != null; }

        public PlaylistInfo addPlaylist(PlaylistInfo.Builder builder) {
            PlaylistInfo playlistInfo = builder.build(this);
            playlists.add(playlistInfo);
            if (builder.isUserFeedback) {
                this.setUserFeedbackPlaylist();
            }
            return playlistInfo;
        }

        public boolean isHasUserFeedbackPlaylist() {
            return hasUserFeedbackPlaylist;
        }
        public PackageInfo setUserFeedbackPlaylist() {
            this.hasUserFeedbackPlaylist = true;
            return this;
        }
        public boolean isHasTutorial() {
            return hasTutorial;
        }
        public PackageInfo setTutorial() {
            this.hasTutorial = true;
            return this;
        }

        public int size() {
            return playlists.size() + (hasTutorial?1:0) + (hasUserFeedbackPlaylist?1:0);
        }

        public PackageInfo prune() {
            List<PlaylistInfo> pruned = playlists.stream()
                .map(PlaylistInfo::prune)
                .filter(PlaylistInfo::isNotEmpty)
                .collect(Collectors.toList());
            playlists.clear();
            playlists.addAll(pruned);
            return this;
        }

        public boolean isNotEmpty() {
            return playlists.size() > 0 || hasTutorial || hasUserFeedbackPlaylist;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getLanguageCode() {
            return languageCode;
        }

        public String getVariant() {
            return variant;
        }

        public String getShortName() {
            return shortName;
        }

        public Iterable<? extends PlaylistInfo> getPlaylists() {
            return playlists;
        }

        /**
         * Helper to build package names. For TBv1 we require a package name shorter than 20 characters,
         * to fit into a fixed-length field on the device.
         *
         * @param shortName if true, munge the name until it is < 20 characters.
         * @return the name.
         */
        private String makeName(boolean shortName) {
            String variantStr = StringUtils.isNotBlank(variant) ? '-' + variant.toLowerCase() : "";
            String packageName = programid + '-' + deploymentNumber + '-' + languageCode + variantStr;

            // If a short name is needed, and this is too long, shorten it.
            if (shortName && packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                // Eliminate hyphens.
                variantStr = StringUtils.isNotBlank(variant) ? variant.toLowerCase() : "";
                packageName = programid + deploymentNumber + languageCode + variantStr;

                // If thats still too long, eliminate vowels in project name.
                String shortid;
                if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                    assert programid != null;
                    shortid = programid.replaceAll("[aeiouAEIOU]", "");
                    packageName = shortid + deploymentNumber + languageCode + variantStr;
                }
                // If still too long, truncate project name.
                if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                    int keep = programid.length() - (Constants.MAX_PACKAGE_NAME_LENGTH - packageName.length());
                    if (keep > 0) {
                        shortid = programid.substring(0, keep);
                        packageName = shortid + deploymentNumber + languageCode + variantStr;
                    } else {
                        // This means either a very long variant or a very long language. Should never happen.
                        // Use the hashcode of the string, and hope?
                        // Put the vowels back
                        shortid = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
                        packageName = shortid + deploymentNumber + languageCode + variantStr;
                        packageName = Integer.toHexString(packageName.hashCode());
                    }
                }
            }
            return packageName;
        }
    }

    public static class PlaylistInfo {
        public static class Builder {
            private String title;
            private PromptInfo announcement;
            private PromptInfo invitation;
            private String categoryId;
            private boolean isUserFeedback;
            private boolean isLocked;

            public Builder withTitle(String title) {
                this.title=title;
                return this;
            }
            public Builder withPrompts(PlaylistPrompts prompts) {
                this.announcement = prompts.getShortItem()!=null ?
                                    new PromptInfo(prompts.getShortItem()) :
                                    new PromptInfo(prompts.getShortFile());
                this.invitation = prompts.getLongItem()!=null ?
                                    new PromptInfo(prompts.getLongItem()) :
                                    new PromptInfo(prompts.getLongFile());
                this.categoryId = prompts.getCategoryId();
                return this;
            }
            public Builder withAnnouncement(AudioItem announcement) {
                this.announcement = new PromptInfo(announcement);
                return this;
            }
            public Builder withAnnouncement(File announcement) {
                this.announcement = new PromptInfo(announcement);
                return this;
            }
            public Builder withInvitation(AudioItem invitation) {
                this.invitation = new PromptInfo(invitation);
                return this;
            }
            public Builder withInvitation(File invitation) {
                this.invitation = new PromptInfo(invitation);
                return this;
            }
            public Builder isUserFeedback(boolean isUserFeedback) {
                this.isUserFeedback = isUserFeedback;
                return this;
            }
            public Builder isLocked(boolean isLocked) {
                this.isLocked = isLocked;
                return this;
            }

            private PlaylistInfo build(PackageInfo packageInfo) {
                return new PlaylistInfo(packageInfo,this);
            }
        }

        private final PackageInfo packageInfo;
        private final String title;
        private final String categoryId;
        private final PromptInfo announcement;
        private final PromptInfo invitation;
        private final boolean isUserFeedback;
        private final boolean isLocked;
        private final List<String> content = new ArrayList<>();

        private PlaylistInfo(PackageInfo packageInfo, Builder builder) {
            this.packageInfo = packageInfo;
            this.title = builder.title;
            this.categoryId = builder.categoryId;
            this.announcement = builder.announcement;
            this.invitation = builder.invitation;
            this.isUserFeedback = builder.isUserFeedback;
            this.isLocked = builder.isLocked;
        }

        public void addContent(AudioItem audioItem) {
            content.add(audioItem.getId());
        }

        /**
         * The content member either has audio items or it doesn't.
         * @return this
         */
        public PlaylistInfo prune() {
            return this;
        }

        public boolean isNotEmpty() {
            return content.size() > 0 || isUserFeedback;
        }
        
        public String getTitle() {
            return title;
        }

        public PlaylistPrompts getPlaylistPrompts() {
            return new PlaylistPrompts(title, packageInfo.languageCode, categoryId,
                announcement.audioFile, announcement.audioItem, invitation.audioFile, invitation.audioItem);
        }

        public Iterable<? extends String> getAudioItemIds() {
            return content;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static class PromptInfo {
        // Must be one and only one of these.
        public final AudioItem audioItem;
        public final File audioFile;

        public PromptInfo(AudioItem audioItem) {
            this.audioItem = audioItem;
            this.audioFile = null;
        }

        public PromptInfo(File audioFile) {
            this.audioItem = null;
            this.audioFile = audioFile;
        }
    }
}
