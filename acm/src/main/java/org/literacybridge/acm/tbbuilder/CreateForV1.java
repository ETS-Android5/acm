package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.CATEGORY_UNCATEGORIZED_FEEDBACK;
import static org.literacybridge.acm.Constants.CUSTOM_GREETING;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CATEGORIES_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CONTENT_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT;
import static org.literacybridge.acm.tbbuilder.TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME;

/*
 * Builds a TBv1 deployment.
 * 

* Given a program 'DEMO', a deployment 'DEMO-21-1', and a package 'DEMO-1-en-c'
*
* a "tree.txt" file with empty directories to be created in each image.
*   inbox
*   log
*   log-archive
*   statistics

DEMO/content/DEMO-21-1/images/DEMO-1-en-c/
*
* DEMO/content/DEMO-21-1/
├── basic
├── communities
├── images
│   ├── DEMO-1-en
│   │   ├── languages
│   │   │   └── en
│   │   │       ├── 0.a18
│   │   │       ├── 1.a18
│   │   │        . . .
│   │   │       ├── 9.a18
│   │   │       ├── cat
│   │   │       │   ├── 9-0.a18
│   │   │       │   ├── LB-2_kkg8lufhqr_jp.a18
│   │   │        . . .
│   │   │       │   └── iLB-2_uzz71upxwm_vn.a18
│   │   │       └── control.txt
│   │   ├── messages
│   │   │   ├── audio
│   │   │   │   ├── LB-2_uzz71upxwm_vg.a18
│   │   │        . . .
│   │   │   │   └── LB-2_uzz71upxwm_zd.a18
│   │   │   └── lists
│   │   │       └── 1
│   │   │           ├── 9-0.txt
│   │   │           ├── LB-2_kkg8lufhqr_jp.txt
│   │   │           ├── LB-2_uzz71upxwm_ve.txt
│   │   │           ├── LB-2_uzz71upxwm_vj.txt
│   │   │           ├── LB-2_uzz71upxwm_vn.txt
│   │   │           └── _activeLists.txt
│   │   └── system
│   │       ├── DEMO-1-en-c.pkg     <<-- zero-byte marker file
│   │       ├── c.grp               <<-- zero-byte marker file
│   │       ├── config.txt          <<-- fairly constant across programs
│   │       └── profiles.txt        <<-- "DEMO-1-EN-C,en,1,menu"
│   ├── DEMO-1-en-c
│   . . .
└── programspec
    ├── content.csv
    ├── content.json                <<-- remove
    ├── deployment.properties
    ├── deployment_spec.csv
    ├── deployments.csv             <<-- remove
    ├── etags.properties
    ├── pending_spec.xlsx           <<-- remove?
    ├── program_spec.xlsx           <<-- remove
    ├── recipients.csv
    └── recipients_map.csv

 *
 */

@SuppressWarnings({"ResultOfMethodCallIgnored"})
class CreateForV1 {

    private final TBBuilder tbBuilder;
    private final TBBuilder.BuilderContext builderContext;
    private final DeploymentInfo deploymentInfo;
    private final AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();

    private final File stagingDir;

    AudioItemRepository.AudioFormat audioFormat = AudioItemRepository.AudioFormat.A18;

    CreateForV1(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext, DeploymentInfo deploymentInfo) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
        this.deploymentInfo = deploymentInfo;

        stagingDir = builderContext.stagingDir; // new File(builderContext.stagingDir.getParent(), "v1"+builderContext.stagingDir.getName());
    }


    void go() throws Exception {
        createBaseDeployment();

        for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
            addImageForPackage(packageInfo);
        }

        exportMetadata();
        
    }

    /**
     * Creates the structure for a Deployment, into which packages can be added.
     *
     * @throws Exception if there is an IO error.
     */
    private void createBaseDeployment() throws Exception {
//        File stagedMetadataDir = new File(stagingDir, "metadata" + File.separator + deploymentInfo.getName());
        DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        ISO8601time.setTimeZone(TBLoaderConstants.UTC);
        String timeStr = ISO8601time.format(new Date());
        String revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REVISION_FORMAT, timeStr, deploymentInfo.getName());
        // use LB Home Dir to create folder, then zip to Dropbox and delete the
        // folder
        IOUtils.deleteRecursive(builderContext.stagedDeploymentDir);
        builderContext.stagedDeploymentDir.mkdirs();
//        IOUtils.deleteRecursive(stagedMetadataDir);
//        stagedMetadataDir.mkdirs();
        IOUtils.deleteRecursive(builderContext.stagedProgramspecDir);
        builderContext.stagedProgramspecDir.mkdirs();

//        builderContext.contentInPackageCSVWriter = new CSVWriterBuilder(
//                new FileWriter(new File(stagedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME))).build();
//        builderContext.categoriesInPackageCSVWriter = new CSVWriterBuilder(
//                new FileWriter(new File(stagedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME))).build();
//        builderContext.packagesInDeploymentCSVWriter = new CSVWriterBuilder(
//                new FileWriter(new File(stagedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME))).build();
//
//        // write column headers
//        builderContext.contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
//        builderContext.categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
//        builderContext.packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = tbBuilder.utils.latestFirmwareImage();
        File stagedBasicDir = new File(builderContext.stagedDeploymentDir, "basic");
        FileUtils.copyFileToDirectory(sourceFirmware, stagedBasicDir);

        if (builderContext.sourceProgramspecDir != null) {
            FileUtils.copyDirectory(builderContext.sourceProgramspecDir, builderContext.stagedProgramspecDir);
        }
        
        Utils.deleteRevFiles(stagingDir);
        // Leave a marker to indicate that there exists an unpublished deployment here.
        File newRev = new File(stagingDir, revFileName);
        newRev.createNewFile();
        // Put a marker inside the unpublished content, so that we will be able to tell which of
        // possibly several is the unpublished one.
        Utils.deleteRevFiles(builderContext.stagedDeploymentDir);
        newRev = new File(builderContext.stagedDeploymentDir, revFileName);
        newRev.createNewFile();

        builderContext.reportStatus("%nDone with deployment of basic/community content.%n");
    }

    private void exportMetadata() throws IOException {
        File stagedMetadataDir = new File(stagingDir, "metadata" + File.separator + deploymentInfo.getName());
        IOUtils.deleteRecursive(stagedMetadataDir);
        stagedMetadataDir.mkdirs();

        ICSVWriter contentInPackageCSVWriter = new CSVWriterBuilder(
            new FileWriter(new File(stagedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME))).build();
        ICSVWriter categoriesInPackageCSVWriter = new CSVWriterBuilder(
            new FileWriter(new File(stagedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME))).build();
        ICSVWriter packagesInDeploymentCSVWriter = new CSVWriterBuilder(
            new FileWriter(new File(stagedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME))).build();

        // write column headers
        contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
        categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
        packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        // Write data
        String[] csvColumns = new String[9];
        csvColumns[0] = deploymentInfo.getProgramId().toUpperCase();

        String[] categoriesColumns = new String[4];
        categoriesColumns[0] = deploymentInfo.getProgramId().toUpperCase();

        String[] contentColumns = new String[5];
        contentColumns[0] = deploymentInfo.getProgramId().toUpperCase();


        for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
            csvColumns[1] = deploymentInfo.getName().toUpperCase(); // default: ${proramid}-${year}-${depl#}
            categoriesColumns[1] = packageInfo.getShortName().toUpperCase();
            contentColumns[1] = packageInfo.getShortName().toUpperCase();

            // Packages in Deployment
            csvColumns[2] = packageInfo.getShortName().toUpperCase();
            csvColumns[3] = packageInfo.getShortName().toUpperCase();
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int date = cal.get(Calendar.DAY_OF_MONTH);
            csvColumns[4] = month + "/" + date + "/"
                + year; // approx start date
            csvColumns[5] = null; // end date unknown at this point
            csvColumns[6] = packageInfo.getLanguageCode();
            String groupsconcat = "default.grp," + packageInfo.getLanguageCode() + ".grp";
            csvColumns[7] = groupsconcat;
            csvColumns[8] = null; // distribution name not known until publishing
            // NOTE that we don't ever include the distribution name in the metadata.
            // It's grabbed by the shell scripts from the folder name,
            // and then a SQL UPDATE adds it in after uploading the CSV.
            packagesInDeploymentCSVWriter.writeNext(csvColumns);

            int plPosition = 0;
            for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                // Categories in Package
                categoriesColumns[2] = playlistInfo.getCategoryId();
                categoriesColumns[3] = Integer.toString(plPosition++);
                categoriesInPackageCSVWriter.writeNext(categoriesColumns);

                // Content in Package
                contentColumns[3] = playlistInfo.getCategoryId();
                int messagePosition = 0;
                for (String audioItemId : playlistInfo.getAudioItemIds()) {
                    contentColumns[2] = audioItemId;
                    contentColumns[4] = Integer.toString(messagePosition++);
                    contentInPackageCSVWriter.writeNext(contentColumns);
                }
            }
        }

        contentInPackageCSVWriter.close();
        categoriesInPackageCSVWriter.close();
        packagesInDeploymentCSVWriter.close();
        
    }
    
    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param packageInfo Information about the package: name, language, groups.
     * @throws Exception if there is an error creating or reading a file.
     */
    private void addImageForPackage(DeploymentInfo.PackageInfo packageInfo) throws Exception {
//        Set<String> exportedCategories = null;
//        boolean hasIntro = false;
        builderContext.reportStatus("%n%nExporting package %s%n", packageInfo.getShortName());

//        File sourcePackageDir = new File(builderContext.sourceTbLoadersDir, "packages"+File.separator + packageInfo.getShortName());
//        File sourceMessagesDir = new File(sourcePackageDir, "messages");
//        File sourceListsDir = new File(sourceMessagesDir, "lists/" + TBBuilder.firstMessageListName);
        File stagedImagesDir = new File(builderContext.stagedDeploymentDir, "images");
        File stagedImageDir = new File(stagedImagesDir, packageInfo.getShortName());

        IOUtils.deleteRecursive(stagedImageDir);
        stagedImageDir.mkdirs();
        
//        if (!sourceListsDir.exists() || !sourceListsDir.isDirectory()) {
//            throw(new TBBuilder.TBBuilderException(String.format("Directory not found: %s%n", sourceListsDir)));
//        } else //noinspection ConstantConditions
//            if (sourceListsDir.listFiles().length == 0) {
//            throw(new TBBuilder.TBBuilderException(String.format("No lists found in %s%n", sourceListsDir)));
//        }


        File stagedMessagesDir = new File(stagedImageDir, "messages");
//        FileUtils.copyDirectory(sourceMessagesDir, stagedMessagesDir);

        File stagedAudioDir = new File(stagedMessagesDir, "audio");
//        File stagedListsDir = new File(stagedMessagesDir, "lists/" + TBBuilder.firstMessageListName);
        File stagedLanguagesDir = new File(stagedImageDir, "languages");
        File stagedLanguageDir = new File(stagedLanguagesDir, packageInfo.getLanguageCode());
        File shadowFilesDir = new File(builderContext.stagedDeploymentDir, "shadowFiles");
        File shadowAudioFilesDir = new File(shadowFilesDir, "messages" + File.separator + "audio");
        File shadowLanguageDir = new File(shadowFilesDir, "languages"+File.separator + packageInfo.getLanguageCode());

        File sourceCommunitiesDir = new File(builderContext.sourceTbLoadersDir, "communities");
        File stagedCommunitiesDir = new File(builderContext.stagedDeploymentDir, "communities");

        for (File f : new File[]{stagedAudioDir, stagedLanguageDir, stagedLanguageDir, sourceCommunitiesDir, stagedCommunitiesDir}) {
            if (!f.exists() && !f.mkdirs()) {
                throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
            }
        }
        if (builderContext.deDuplicateAudio) {
            for (File f : new File[]{shadowAudioFilesDir, shadowLanguageDir}) {
                if (!f.exists() && !f.mkdirs()) {
                    throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
                }
            }
        }

        // Create the empty directory structure
        File sourceBasic = new File(builderContext.sourceTbOptionsDir, "basic");
        FileUtils.copyDirectory(sourceBasic, stagedImageDir);

        addPackageContentToImage(packageInfo, stagedImageDir);
        addPackagePromptsToImage(packageInfo, stagedImageDir);
        addPackageSystemFilesToImage(packageInfo, stagedImageDir);
        if (packageInfo.hasTutorial()) {
            addTutorialToImage(packageInfo, stagedImageDir);
        }

//        File[] listFiles = stagedListsDir.listFiles();
//        //noinspection ConstantConditions
//        for (File listFile : listFiles) {
//            // We found a "0-5.txt" file (note that there's no entry in the _activeLists.txt file)
//            // Export the ids listed in the file as languages/intro.txt (last one wins), and
//            // delete the file. Remember that the file existed; we'll use that to select a
//            // control.txt file that plays the intro.a18 at startup.
//            if (listFile.getName().equalsIgnoreCase("_activeLists.txt")) {
//                exportedCategories = exportCategoriesCSVInPackage(packageInfo.getShortName(), listFile);
//            } else if (listFile.getName().equals(TBBuilder.IntroMessageListFilename)) {
//                exportIntroMessage(listFile, stagedLanguageDir, audioFormat);
//                listFile.delete();
//                hasIntro = true;
//            } else {
//                exportContentAndCsvForPlaylist(packageInfo.getShortName(), listFile, shadowAudioFilesDir, stagedAudioDir, audioFormat);
//            }
//        }

//        if (exportedCategories == null) {
//            throw new IllegalStateException("Missing _activeLists.txt file");
//        }


//        // The config.txt file. It could have just as easily been in basic/system/config.txt.
//        File sourceConfigFile = new File(builderContext.sourceTbOptionsDir, "config_files"+File.separator+"config.txt");
//        File stagedSystemDir = new File(stagedImageDir, "system");
//        FileUtils.copyFileToDirectory(sourceConfigFile, stagedSystemDir);

        // Custom greetings
        exportGreetings(sourceCommunitiesDir, stagedCommunitiesDir, packageInfo);

//        // System and category prompts
//        File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir, "languages"+File.separator + packageInfo.language);
//        exportSystemPrompts(shadowLanguageDir, stagedLanguageDir, packageInfo.language, audioFormat);
//        // The prompt "9-0" is always needed to announce where user feedback is recorded. "i9-0" is only needed
//        // if user feedback is public.
//        Set<String> neededPrompts = new HashSet<>(exportedCategories);
//        neededPrompts.add(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
//        exportPlaylistPrompts(packageInfo, neededPrompts, shadowLanguageDir, stagedLanguageDir);

//        // If the deployment has the tutorial, copy necessary files.
//        if (exportedCategories.contains(Constants.CATEGORY_TUTORIAL)) {
//            exportTutorial(sourceLanguageDir, stagedLanguageDir);
//        }
//        // If there is no category "9-0" in the _activeLists.txt file, then the user feedback
//        // should not be playable. In that case, use the "_nofb" versions of control.txt.
//        // Those have a "UFH", User Feedback Hidden, in the control file, which prevents the
//        // Talking Book from *adding* a "9-0" to the _activeLists.txt file when user feedback
//        // is recorded. If the 9-0 is already there, then the users can already hear other
//        // users' feedback.
//        boolean hasNoUf = !exportedCategories.contains(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
//        String sourceControlFilename = String.format("system_menus"+File.separator+"control-%s_intro%s.txt",
//                hasIntro?"with":"no",
//                hasNoUf?"_no_fb":"");
//        File sourceControlFile = new File(builderContext.sourceTbOptionsDir, sourceControlFilename);
//        FileUtils.copyFile(sourceControlFile, new File(stagedLanguageDir, "control.txt"));

//        // create profiles.txt
//        String profileString = packageInfo.getShortName().toUpperCase() + "," + packageInfo.languageCode + ","
//                + TBBuilder.firstMessageListName + ",menu\n";
//        File profileFile = new File(stagedSystemDir, "profiles.txt");
//        BufferedWriter out = new BufferedWriter(new FileWriter(profileFile));
//        out.write(profileString);
//        out.close();

//        for (String group : packageInfo.groups) {
//            File f = new File(stagedSystemDir, group + TBLoaderConstants.GROUP_FILE_EXTENSION);
//            f.createNewFile();
//        }

//        File f = new File(stagedSystemDir, packageInfo.getShortName() + ".pkg");
//        f.createNewFile();

//        exportPackagesCSVInDeployment(packageInfo.getShortName(), packageInfo.language, packageInfo.groups);
        builderContext.reportStatus(
                String.format("Done with adding image for %s and %s.%n", packageInfo.getShortName(), packageInfo.getLanguageCode()));
    }

    /**
     * Adds the content for the given package to the given image files. This creates and populates
     * the messages/lists/1 directory and the
     * @param packageInfo The package to be added to the image.
     * @param imageDir The location of the iamge files, where the content is written.
     */
    private void addPackageContentToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir)
        throws
            IOException,
            BaseAudioConverter.ConversionException,
            AudioItemRepository.UnsupportedFormatException,
            TBBuilder.TBBuilderException {
        File messagesDir = new File(imageDir, "messages");
        File listsDir = new File(messagesDir, "lists" + File.separator + "1");
        if (!listsDir.exists() && !listsDir.mkdirs()) {
            throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", listsDir)));
        }
        File audioDir = new File(messagesDir, "audio");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());

        File shadowFilesDir = new File(builderContext.stagedDeploymentDir, "shadowFiles");
        File shadowAudioFilesDir = new File(shadowFilesDir, "messages" + File.separator + "audio");

        File activeLists = new File(listsDir, "_activeLists.txt");
        try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
            // Export the playlist content
            for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                String promptCat = playlistInfo.getCategoryId();
                // Add the category id to the master list (_activeLists)
                activeListsWriter.println("!" + promptCat);

                File listFile = new File(listsDir, promptCat + ".txt");
                try (PrintWriter listWriter = new PrintWriter(listFile)) {
                    addPlaylistContentToImage(playlistInfo, listWriter, audioDir, shadowAudioFilesDir);
                }
            }
        }
        // Export the intro, if there is one.
        if (packageInfo.hasIntro()) {
            File exportFile = new File(languageDir,"intro.a18");
            repository.exportAudioFileWithFormat(packageInfo.getIntro(), exportFile, audioFormat);
        }
    }

    /**
     * Adds the content for a playlist to the given image's files.
     * @param playlistInfo The playlist to be added to the image.
     * @param listWriter Write audio item ids here.
     * @param audioDir Audio files (or shadow markers) go here.
     * @param shadowDir If shodowing, the real files go here.
     * @throws IOException
     * @throws BaseAudioConverter.ConversionException
     * @throws AudioItemRepository.UnsupportedFormatException
     */
    private void addPlaylistContentToImage(DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo, PrintWriter listWriter,
        File audioDir, File shadowDir) throws
                          IOException,
                          BaseAudioConverter.ConversionException,
                          AudioItemRepository.UnsupportedFormatException {

        for (String audioItemId : playlistInfo.getAudioItemIds()) {
            // Export the audio item.
            AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore().getAudioItem(audioItemId);
            builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, audioDir));
            String filename = repository.getAudioFilename(audioItem, audioFormat);

            File exportFile = determineShadowFile(audioDir, filename, shadowDir);
            if (!exportFile.exists()) {
                repository.exportAudioFileWithFormat(audioItem, exportFile, audioFormat);
            }

            // Add the audio item id to the list file.
            listWriter.println(audioItemId);
        }
    }

    private void addPackagePromptsToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws IOException {
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());
        File shadowFilesDir = new File(builderContext.stagedDeploymentDir, "shadowFiles");
        File shadowLanguageDir = new File(shadowFilesDir, "languages"+File.separator + packageInfo.getLanguageCode());

        // Copy the system prompt files from TB_Options.
        for (String prompt : TBBuilder.REQUIRED_SYSTEM_MESSAGES) {
            String promptFilename = prompt + '.' + audioFormat.getFileExtension();

            File exportFile = determineShadowFile(languageDir, promptFilename, shadowLanguageDir);
            if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
            if (!exportFile.exists()) {
                try {
                    repository.exportSystemPromptFileWithFormat(prompt, exportFile, packageInfo.getLanguageCode(), audioFormat);
                } catch(Exception ex) {
                    // Keep going after failing to export a prompt.
                    builderContext.logException(ex);
                }
            }
        }
        // Copy the prompt files from TB_Options.
        File shadowCatDir = new File(shadowLanguageDir, "cat");
        File catDir = new File(languageDir, "cat");

        Set<String> categories = packageInfo.getPlaylists()
            .stream()
            .map(DeploymentInfo.PackageInfo.PlaylistInfo::getCategoryId)
            .collect(Collectors.toSet());
        if (!packageInfo.isUfHidden()) {
            categories.add(CATEGORY_UNCATEGORIZED_FEEDBACK);
        }
        for (String prompt : categories) {
            String promptFilename = prompt + '.' + audioFormat.getFileExtension();
            String prompt2Filename = 'i'+prompt;
            File exportFile = determineShadowFile(catDir, promptFilename, prompt2Filename, shadowCatDir);
            if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
            // If the short-prompt audio file is not in the shadow directory, copy *both* there now.
            if (!exportFile.exists()) {
                try {
                    // This function handles the difference between languages/en/cat/2-0.a18 and "Health.a18".
                    repository.exportCategoryPromptPairWithFormat(packageInfo.getShortName(),
                        prompt,
                        exportFile,
                        packageInfo.getLanguageCode(),
                        audioFormat);
                } catch (BaseAudioConverter.ConversionException | AudioItemRepository.UnsupportedFormatException ex) {
                    // Keep going after failing to export a prompt.
                    builderContext.logException(ex);
                }
            }
        }
    }

    private void addPackageSystemFilesToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws
                                                                                                     IOException {
        File systemDir = new File(imageDir, "system");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());

        // The package marker file.
        File packageMarkerFile = new File(systemDir, packageInfo.getShortName() + ".pkg");
        packageMarkerFile.createNewFile();

        // The config.txt file. It could have just as easily been in basic/system/config.txt.
        File sourceConfigFile = new File(builderContext.sourceTbOptionsDir, "config_files"+File.separator+"config.txt");
        FileUtils.copyFileToDirectory(sourceConfigFile, systemDir);

        // .grp marker file
        new File(systemDir, "default.grp").createNewFile();
        new File(systemDir, packageInfo.getLanguageCode() + ".grp").createNewFile();

        // profiles.txt
        String profileString = packageInfo.getShortName().toUpperCase() + "," + packageInfo.getLanguageCode() + ","
            + TBBuilder.firstMessageListName + ",menu\n";
        File profileFile = new File(systemDir, "profiles.txt");
        try (FileWriter fw = new FileWriter(profileFile);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(profileString);
        }

        // Appropriate control file in languages directory, based on "intro" and uf hidden.
        String sourceControlFilename = String.format("system_menus"+File.separator+"control-%s_intro%s.txt",
            packageInfo.hasIntro()?"with":"no",
            packageInfo.isUfHidden()?"_no_fb":"");
        File sourceControlFile = new File(builderContext.sourceTbOptionsDir, sourceControlFilename);
        FileUtils.copyFile(sourceControlFile, new File(languageDir, "control.txt"));
    }

    /**
     * Copy the files for the tutorial. Currently just the $0-1.txt file, but this could also include the
     * audio files themselves.
     * @param packageInfo The package that needs a tutorial.
     * @param imageDir Where the files are to be put.
     * @throws IOException if the copy fails.
     */
    private void addTutorialToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws IOException {
        File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir, "languages"+File.separator + packageInfo.getLanguageCode());
        File sourceTutorialTxt = new File(sourceLanguageDir, Constants.CATEGORY_TUTORIAL + ".txt");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());
        File tutorialTxt = new File(languageDir, sourceTutorialTxt.getName());
        IOUtils.copy(sourceTutorialTxt, tutorialTxt);

    }

    /**
     * Creates a File object, either in the real directory, or in the shadow directory, depending on the value of
     * builderContext.deDuplicateAudio.
     * @param realDirectory Where the file ultimately needs to wind up; may get a zero-byte marker inside the 
     *                      deployment, so that we can keep only a single copy of the file.
     * @param filename The name of the file that will be created.
     * @param shadowDirectory A "shadow" directory, shared across all packages. One file here may ultimately
     *                        be copied to many packages.
     * @return The actual file to be written.
     * @throws IOException if there is a problem creating the zero-byte shadow file.
     */
    private File determineShadowFile(File realDirectory, String filename, File shadowDirectory) throws IOException {
        return determineShadowFile(realDirectory, filename, null, shadowDirectory);
    }

    /**
     * Almost exactly like determineShadowFile(File, String, File).
     * Creates a File object, either in the real directory, or in the shadow directory, depending on the value of
     * builderContext.deDuplicateAudio.
     * @param realDirectory Where the file ultimately needs to wind up; may get a zero-byte marker inside the 
     *                      deployment, so that we can keep only a single copy of the file.
     * @param filename The name of the file that will be created.
     * @param filename2 The name of another file for which a shadow file will be created. Note that this assumes
     *                  that the caller knows how to create the second file in the same directory as the first file.
     *                  This is used for the pairs of playlist prompts.
     * @param shadowDirectory A "shadow" directory, shared across all packages. One file here may ultimately
     *                        be copied to many packages.
     * @return The actual file to be written.
     * @throws IOException if there is a problem creating the zero-byte shadow file.
     */
    private File determineShadowFile(File realDirectory, String filename, String filename2, File shadowDirectory) throws IOException {
        File exportFile;
        if (builderContext.deDuplicateAudio) {
            // Leave a 0-byte marker file to indicate an audio file that should be here.
            File markerFile = new File(realDirectory, filename);
            markerFile.createNewFile();
            markerFile = new File(realDirectory, filename2);
            markerFile.createNewFile();
            exportFile = new File(shadowDirectory, filename);
        } else {
            // Export file to actual location.
            exportFile = new File(realDirectory, filename);
        }
        return exportFile;
    }


    /**
     * For some package, create the line in the packagesindeployment.csv file. See {@link CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT}
     * for the columns.
     * @param contentPackage The name of the package. This is built from the program name, the deployment, the
     *                       language, and variant (if any). The only real requirement is that it be unique.
     * @param languageCode Language of the package.
     * @param groups "Groups" of the package. We only use the language as a group. "Variants" are denoted in
     *               the package name.
     */
//    private void exportPackagesCSVInDeployment(
//            String contentPackage,
//            String languageCode, String[] groups)
//    {
//        String groupsconcat = StringUtils.join(groups, ',');
//        String[] csvColumns = new String[9];
//        csvColumns[0] = deploymentInfo.getProgramId().toUpperCase();
//        csvColumns[1] = deploymentInfo.getName().toUpperCase();
//        csvColumns[2] = contentPackage.toUpperCase();
//        csvColumns[3] = contentPackage.toUpperCase();
//        Calendar cal = Calendar.getInstance();
//        int year = cal.get(Calendar.YEAR);
//        int month = cal.get(Calendar.MONTH) + 1;
//        int date = cal.get(Calendar.DAY_OF_MONTH);
//        csvColumns[4] = month + "/" + date + "/"
//                + year; // approx start date
//        csvColumns[5] = null; // end date unknown at this point
//        csvColumns[6] = languageCode;
//        csvColumns[7] = groupsconcat;
//        csvColumns[8] = null; // distribution name not known until publishing
//        // NOTE that we don't ever include the distribution name in the metadata.
//        // It's grabbed by the shell scripts from the folder name,
//        // and then a SQL UPDATE adds it in after uploading the CSV.
//        builderContext.packagesInDeploymentCSVWriter.writeNext(csvColumns);
//    }

    /**
     * Given the _activeLists.txt file for a package, extract the list of category names from the file,
     * and return that as a set of strings. Write each category name to the "categoriesinPackage" csv. See
     * {@link CSV_COLUMNS_CATEGORIES_IN_PACKAGE} for columns.
     * @param contentPackage Name of the package. For the .csv file.
     * @param activeLists File with a list of playlist categories (like "2-0.txt" or "LB-2_uzz71upxwm_zf.txt"
     * @return a set of strings of the categories.
     * @throws IOException if a file can't be read or written.
     */
//    private Set<String> exportCategoriesCSVInPackage(
//            String contentPackage,
//            File activeLists) throws IOException {
//        Set<String> categoriesInPackage = new LinkedHashSet<>();
//        String[] csvColumns = new String[4];
//        csvColumns[0] = deploymentInfo.getProgramId().toUpperCase();
//        csvColumns[1] = contentPackage.toUpperCase();
//
//        int order = 1;
//        try (BufferedReader reader = new BufferedReader(new FileReader(activeLists))) {
//            while (reader.ready()) {
//                String categoryID = reader.readLine().trim();
//                if (categoryID.startsWith("!")) {
//                    categoryID = categoryID.substring(1);
//                }
//                categoriesInPackage.add(categoryID);
//                if (categoryID.startsWith("$")) {
//                    categoryID = categoryID.substring(1);
//                }
//                csvColumns[2] = categoryID;
//                csvColumns[3] = Integer.toString(order);
//                builderContext.categoriesInPackageCSVWriter.writeNext(csvColumns);
//                order++;
//            }
//        }
//        return categoriesInPackage;
//    }

    /**
     * Export the greetings for the recipients of the package. May include recipients from other variants.
     *
     * @param sourceCommunitiesDir The communities directory in the TB-Loaders directory.
     * @param stagedCommunitiesDir The communities directory in the output staging directory.
     * @param packageInfo The package info.
     * @throws IOException If a greeting file can't be read or written.
     * @throws BaseAudioConverter.ConversionException if a greeting file can't be converted.
     */
    private void exportGreetings(File sourceCommunitiesDir,
        File stagedCommunitiesDir,
        DeploymentInfo.PackageInfo  packageInfo) throws
                                                                                    IOException,
                                                                                    BaseAudioConverter.ConversionException {
        // If we know the deployment, we can be more specific, possibly save some space.
        RecipientList recipients = builderContext.deploymentNo > 0
           ? builderContext.programSpec.getRecipientsForDeploymentAndLanguage(deploymentInfo.getDeploymentNumber(),
            packageInfo.getLanguageCode())
           : builderContext.programSpec.getRecipients();
        for (Recipient recipient : recipients) {
            // TODO: Get recipient specific audio format, in case there are ever programs with mixed v1/v2 TBs.
            exportGreetingForRecipient(recipient, sourceCommunitiesDir, stagedCommunitiesDir, audioFormat);
        }
    }

    /**
     * Exports the greeting file for one recipient.
     * @param recipient The recipient for which to export the greeting.
     * @param sourceCommunitiesDir Where greetings come from ( {program}/TB-Loaders/communities )
     * @param stagedCommunitiesDir Where greetings go to.
     * @param audioFormat Audio format for the greeting.
     * @throws IOException if a greeting can't be read or written.
     * @throws BaseAudioConverter.ConversionException If a greeting can't be converted.
     */
    private void exportGreetingForRecipient(Recipient recipient,
            File sourceCommunitiesDir,
            File stagedCommunitiesDir,
            AudioItemRepository.AudioFormat audioFormat) throws
                                                         IOException,
                                                         BaseAudioConverter.ConversionException {
        String communityDirName = builderContext.programSpec.getRecipientsMap().getOrDefault(recipient.recipientid, recipient.recipientid);
        File sourceCommunityDir = new File(sourceCommunitiesDir, communityDirName);
        File targetCommunityDir = new File(stagedCommunitiesDir, communityDirName);
        File sourceLanguageDir = new File(sourceCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetLanguageDir = new File(targetCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetSystemDir = new File(targetCommunityDir, "system");
        // Copy the greeting in the recipient's language, if there is one. Create the vestigal ".grp" file as well.
        if (sourceLanguageDir.exists() && sourceLanguageDir.isDirectory() && Objects.requireNonNull(sourceLanguageDir.listFiles()).length > 0) {
            targetLanguageDir.mkdirs();
            targetSystemDir.mkdirs();
            File targetFile = new File(targetLanguageDir, CUSTOM_GREETING);
            repository.exportGreetingWithFormat(sourceLanguageDir, targetFile, audioFormat);
            File groupFile = new File(targetSystemDir, recipient.languagecode + ".grp");
            groupFile.createNewFile();
        }
    }

    /**
     * Copy the files for the tutorial. Currently just the $0-1.txt file, but this could also include the
     * audio files themselves.
     * @param sourceLanguageDir Source of files.
     * @param stagedLanguageDir Destination of files.
     * @throws IOException If a file can't be copied.
     */
//    private void exportTutorial(File sourceLanguageDir, File stagedLanguageDir) throws IOException {
//        File sourceTutorialTxt = new File(sourceLanguageDir, Constants.CATEGORY_TUTORIAL + ".txt");
//        File stagedTutorialTxt = new File(stagedLanguageDir, sourceTutorialTxt.getName());
//        IOUtils.copy(sourceTutorialTxt, stagedTutorialTxt);
//    }

    /**
     * Export the system files. Copy the actual files to the "shadow" directory, so we keep only one copy
     * of each file, regardless how many images it appears in. Place a zero-byte marker file where the real
     * files should go. The TB-Loader will fix it up.
     * @param shadowLanguageDir Where the real files go.
     * @param stagedLanguageDir Where the zero-byte marker files go.
     * @param language Language for which prompts are needed.
     * @param audioFormat Audio format for which prompts are needed.
     * @throws IOException If a file can't be copied, found, etc.
     */
//    private void exportSystemPrompts(File shadowLanguageDir,
//            File stagedLanguageDir,
//            String language,
//            AudioItemRepository.AudioFormat audioFormat) throws
//                                                         IOException {
//        for (String prompt : TBBuilder.REQUIRED_SYSTEM_MESSAGES) {
//            String promptFilename = prompt + '.' + audioFormat.getFileExtension();
//
//            File exportFile;
//            if (builderContext.deDuplicateAudio) {
//                File markerFile = new File(stagedLanguageDir, promptFilename);
//                markerFile.createNewFile();
//                exportFile = new File(shadowLanguageDir, promptFilename);
//            } else {
//                exportFile = new File(stagedLanguageDir, promptFilename);
//            }
//            if (!exportFile.exists()) {
//                try {
//                    repository.exportSystemPromptFileWithFormat(prompt, exportFile, language, audioFormat);
//                } catch(Exception ex) {
//                    // Keep going after failing to export a prompt.
//                    builderContext.logException(ex);
//                }
//            }
//        }
//    }

    /**
     *
     * @param packageInfo Info about the package, including language and audio format.
     * @param playlistCategories A collection containing the categories. Prompts for these are exported.
     * @param shadowLanguageDir Where the real files go.
     * @param stagedLanguageDir Where the zero-byte marker files go.
     */
//    private void exportPlaylistPrompts(DeploymentInfo.PackageInfo packageInfo,
//            Collection<String> playlistCategories,
//            File shadowLanguageDir,
//            File stagedLanguageDir) {
//        File shadowCatDir = new File(shadowLanguageDir, "cat");
//        File stagedCatDir = new File(stagedLanguageDir, "cat");
//        // We've just created the parents successfully. There is no valid reason for these to fail.
//        if (builderContext.deDuplicateAudio) {
//            shadowCatDir.mkdirs();
//        }
//        stagedCatDir.mkdirs();
//
//        for (String prompt : playlistCategories) {
//            String promptFilename = prompt + '.' + audioFormat.getFileExtension();
//            File exportFile;
//            try {
//                if (builderContext.deDuplicateAudio) {
//                    // Create both marker files
//                    File markerFile = new File(stagedCatDir, promptFilename);
//                    markerFile.createNewFile();
//                    markerFile = new File(stagedCatDir, 'i' + promptFilename);
//                    markerFile.createNewFile();
//                    // The name of the short prompt, in the shadow directory.
//                    exportFile = new File(shadowCatDir, promptFilename);
//                }
//                else {
//                    // The name of the short prompt, in the non-shadowed target directory.
//                    exportFile = new File(stagedCatDir, promptFilename);
//                }
//                // If the short-prompt audio file is not in the shadow directory, copy *both* there now.
//                if (!exportFile.exists()) {
//                    repository.exportCategoryPromptPairWithFormat(packageInfo.getShortName(),
//                            prompt,
//                            exportFile,
//                        packageInfo.getLanguageCode(),
//                            audioFormat);
//                }
//            } catch(Exception ex) {
//                // Keep going after failing to export a prompt.
//                builderContext.logException(ex);
//            }
//        }
//    }

    /**
     * Given a file with a list of audio item ids, extract those audio items to the given directory. Optionally
     * add each audio item to the "contentInPackage" csv (see {@link CSV_COLUMNS_CONTENT_IN_PACKAGE} for columns.)
     * @param contentPackage Name of the package, only used for the .csv file.
     * @param list File with list of item ids.
     * @param shadowDirectory The directory to receive the actual file. A copy of the file may already be there.
     * @param targetDirectory The ultimate destination directory of the file. A 0-byte marker file is written
     *                        there at this time, filled in with the actual content at deployment time.
     * @param audioFormat The needed AudioFormat
     * @throws Exception if a file can't be read or written.
     */
//    private void exportContentAndCsvForPlaylist(
//            String contentPackage, File list,
//            File shadowDirectory,
//            File targetDirectory,
//            AudioItemRepository.AudioFormat audioFormat)
//            throws Exception {
//        builderContext.reportStatus("  Exporting list %n" + list);
//        String[] csvColumns = new String[5];
//        csvColumns[0] = deploymentInfo.getProgramId().toUpperCase();
//        csvColumns[1] = contentPackage.toUpperCase();
//        csvColumns[3] = FilenameUtils.removeExtension(list.getName());
//
//        int order = 1;
//        try (BufferedReader reader = new BufferedReader(new FileReader(list))) {
//            while (reader.ready()) {
//                String audioItemId = reader.readLine();
//                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
//                        .getMetadataStore().getAudioItem(audioItemId);
//                builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, targetDirectory));
//                String filename = repository.getAudioFilename(audioItem, audioFormat);
//
//                File exportFile;
//                if (builderContext.deDuplicateAudio) {
//                    // Leave a 0-byte marker file to indicate an audio file that should be here.
//                    File markerFile = new File(targetDirectory, filename);
//                    markerFile.createNewFile();
//                    exportFile = new File(shadowDirectory, filename);
//                } else {
//                    // Export file to actual location.
//                    exportFile = new File(targetDirectory, filename);
//                }
//                if (!exportFile.exists()) {
//                    repository.exportAudioFileWithFormat(audioItem,
//                            exportFile,
//                            audioFormat);
//                }
//
//                csvColumns[2] = audioItemId;
//                csvColumns[4] = Integer.toString(order);
//                builderContext.contentInPackageCSVWriter.writeNext(csvColumns);
//
//                order++;
//            }
//        }
//    }

    /**
     * Given a file with a list of audio item ids, extract those audio items to the given directory. Optionally
     * add each audio item to the "contentInPackage" csv.
     * @param list File with list of item ids.
     * @param targetDirectory Directory into which to extract the audio items.
     * @param audioFormat The needed AudioFormat
     * @throws Exception if a file can't be read or written.
     */
//    private void exportIntroMessage(
//            File list,
//            File targetDirectory,
//            AudioItemRepository.AudioFormat audioFormat)
//            throws Exception {
//        builderContext.reportStatus("  Exporting list %n" + list);
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(list))) {
//            while (reader.ready()) {
//                String audioItemId = reader.readLine();
//                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getAudioItem(audioItemId);
//                builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, targetDirectory));
//                File exportFile = new File(targetDirectory,"intro.a18");
//
//                repository.exportAudioFileWithFormat(audioItem, exportFile, audioFormat);
//            }
//        }
//    }
}
