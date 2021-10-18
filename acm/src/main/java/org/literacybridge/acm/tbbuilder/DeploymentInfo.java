package org.literacybridge.acm.tbbuilder;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains all the information needed to create a deployment.
 */
public class DeploymentInfo {
    private final String programid;
    private final int deploymentNumber;
    private boolean ufHidden;
    private boolean hasTutorial;

    private final Map<String, PackageInfo> packages = new HashMap<>();

    public DeploymentInfo(String programid, int deploymentNumber) {
        this.programid = programid;
        this.deploymentNumber = deploymentNumber;
    }

    public boolean isUfHidden() { return ufHidden; }
    public DeploymentInfo ufHidden() { return setUfHidden(true); }
    public DeploymentInfo setUfHidden(boolean ufHidden) {
        this.ufHidden = ufHidden;
    }

    public boolean hasTutorial() { return hasTutorial; }
    public DeploymentInfo setTutorial(boolean hasTutorial) {
        this.hasTutorial = hasTutorial;
        return this;
    }

    public PackageInfo addPackage(String languageCode, String variant) {
        PackageInfo packageInfo = new PackageInfo(languageCode, variant);
        packages.put(packageInfo.name, packageInfo);
        return packageInfo;
    }

    public class PackageInfo {
        final String languageCode;
        final String variant;
        final String name;
        final String shortName;
        private AudioItem introMessage;

        final List<PlaylistInfo> playlists = new ArrayList<>();

        public PackageInfo(String languageCode, String variant) {
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
            PlaylistInfo playlistInfo = builder.build();
            playlists.add(playlistInfo);
            return playlistInfo;
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
        public class Builder {
            private String title;
            private PromptInfo announcement;
            private PromptInfo invitation;
            private boolean isUserFeedback;
            
            public Builder withTitle(String title) {
                this.title=title;
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
            public Builder isUserFeedback() {
                this.isUserFeedback = true;
                return this;
            }

            public PlaylistInfo build() {
                return new PlaylistInfo(this);
            }
        }
        
        private final Builder builder;
        private final List<AudioItem> content = new ArrayList<>();

        public PlaylistInfo(Builder builder) {
            this.builder = builder;
        }

        public void addContent(AudioItem audioItem) {
            content.add(audioItem);
        }
    }

    public static class PromptInfo {
        public final AudioItem audioItem;
        public final File audioFile;

        public PromptInfo(AudioItem audioItem) {
            this.audioItem = audioItem;
            this.audioFile = null;
        }

        public PromptInfo(File audioFile) {
            this.audioFile = audioFile;
            this.audioItem = null;
        }
    }
}
