package org.literacybridge.acm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.ToolbarView;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository.GCInfo;
import org.literacybridge.acm.repository.WavCaching;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Playlist;

public class Application extends JXFrame {
    private static final Logger LOG = Logger.getLogger(Application.class.getName());

    private static final long serialVersionUID = -7011153239978361786L;

    // message pump
    private static SimpleMessageService simpleMessageService = new SimpleMessageService();

    private Color backgroundColor;
    private final ACMStatusBar statusBar;
    private final BackgroundTaskManager taskManager;

    public static SimpleMessageService getMessageService() {
        return simpleMessageService;
    };


    // file system monitor for the audio devices
    private static FileSystemMonitor fileSystemMonitor = new FileSystemMonitor();

    public static FileSystemMonitor getFileSystemMonitor() {
        return fileSystemMonitor;
    }

    private static FilterState filterState = new FilterState();

    public static FilterState getFilterState() {
        return filterState;
    }

    // application instance
    private static Application application;

    public static Application getApplication() {
        return application;
    }

    private SimpleSoundPlayer player = new SimpleSoundPlayer();

    private Application(SplashScreen splashScreen) throws IOException {
        super();
        this.backgroundColor = getBackground();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (!ACMConfiguration.getCurrentDB().getControlAccess().isSandbox())
                        ACMConfiguration.getCurrentDB().getControlAccess().updateDB();
                    ACMConfiguration.closeCurrentDB();
                }
                catch(Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        String title = new String(LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM", LanguageUtil.getUILanguage()));
        title += " (" + Constants.ACM_VERSION + ")";
        if (ACMConfiguration.getACMname() != null)
            title += "                   " + ACMConfiguration.getACMname();
        else if (ACMConfiguration.getCurrentDB().getSharedACMname() != null)
            title += "                   " + ACMConfiguration.getCurrentDB().getSharedACMname();
        String dbVersion = ACMConfiguration.getCurrentDB().getControlAccess().getCurrentZipFilename();
        dbVersion = dbVersion.replaceAll("db", "");
        dbVersion = dbVersion.replaceAll(".zip", "");
        title += " (v" + dbVersion + ")";
        if (ACMConfiguration.getCurrentDB().getControlAccess().isSandbox())
            title += "               CHANGES WILL *NOT* BE SAVED!   ";

        setTitle(title);
        // toolbar view on top
        ResourceView resourceView = new ResourceView();
        ToolbarView toolbarView = new ToolbarView(resourceView.audioItemView);
        add(toolbarView, BorderLayout.PAGE_START);
        add(resourceView, BorderLayout.CENTER);

        statusBar = new ACMStatusBar();
        setStatusBar(statusBar);
        taskManager = new BackgroundTaskManager(statusBar);

        try {
            splashScreen.setProgressLabel("Updating index...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // starts file system monitor after UI has been initialized
        fileSystemMonitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());
        fileSystemMonitor.start();
    }

    @Override
    public void setBackground(Color bgColor) {
        // Workaround for weird bug in seaglass look&feel that causes a
        // java.awt.IllegalComponentStateException when e.g. a combo box
        // in this dialog is clicked on
        if (bgColor.getAlpha() == 0) {
            super.setBackground(backgroundColor);
        } else {
            super.setBackground(bgColor);
            backgroundColor = bgColor;
        }
    }

    public void setStatusMessage(String message) {
        statusBar.setStatusMessage(message);
    }

    public void setProgressMessage(String message) {
        statusBar.setProgressMessage(message);;
    }

    public BackgroundTaskManager getTaskManager() {
        return taskManager;
    }

    public SimpleSoundPlayer getSoundPlayer() {
        return this.player;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("starting main()");
        CommandLineParams params = new CommandLineParams();
        CmdLineParser parser = new CmdLineParser(params);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -cp acm.jar;lib/* org.literacybridge.acm.gui.Application [options...]");
            parser.printUsage(System.err);
            return;
        }
        startUp(params);
    }

    public static void startUp(CommandLineParams params) throws Exception {
        // TODO: This method is mostly about setting up the GUI, with a little bit of
        // reading configuration and opening the database. That's fine, but the method
        // is called by non-GUI utilities, just for the side effect of getting the database
        // open. After Michael checks in his major database migration, we should factor out
        // the configuration and database code, so the non-GUI utilities don't call this.
        boolean showUI = !params.disableUI;
        SplashScreen splash = null;

        if (showUI) {
            splash = new SplashScreen();

            // set look & feel

            // Not sure why, but making this call before setting the seaglass look and feel
            // prevents an UnsatisfiedLinkError to be thrown
            final LookAndFeel defaultLAF = UIManager.getLookAndFeel();
            try {
                UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
            } catch (Exception e) {
                try {
                    LOG.log(Level.WARNING, "Unable to set look and feel.", e);
                    UIManager.setLookAndFeel(defaultLAF);
                } catch (Exception e1) {
                    LOG.log(Level.WARNING, "Unable to set look and feel.", e1);
                }
            }

            splash.showSplashScreen();

            if (Runtime.getRuntime().maxMemory() < 400 * 1024 * 1024) {
                JOptionPane.showMessageDialog(null, "Not enough memory available for JVM. Please make sure your"
                        + " java command contains the argument -XMX512m (or more).");
                System.exit(0);
            }
        }


        //		String dbDirName = null, repositoryDirName= null;
        // initialize config and generate random ID for this acm instance
        if (showUI) {
            splash.setProgressLabel("Initializing...");
        }
        ACMConfiguration.initialize(params);

        // init database
        try {
            // TODO: when we have a homescreen this call will be delayed until the user selects a DB
            // TODO: createEmtpyDB should be factored out when the UI has a create DB button.
            ACMConfiguration.setCurrentDB(params.sharedACM, true);

            // DB migration if necessary
            System.out.print("Updating database ... ");
            if (showUI) {
                splash.setProgressLabel("Updating database...");
            }
            System.out.println("done.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unable to connect to database. Please try restarting the ACM.");
            System.exit(0);
        }


        if (showUI) {
            application = new Application(splash);
            splash.setProgressLabel("Initialization complete. Launching UI...");
            application.setSize(1000, 725);

            application.setVisible(true);
            application.toFront();
            splash.close();

            LOG.log(Level.INFO, "ACM successfully started.");
            WavCaching caching = new WavCaching();
            GCInfo gcInfo = ACMConfiguration.getCurrentDB().getRepository().needsGc();

            if (gcInfo.isGcRecommended()) {
                long sizeMB = gcInfo.getCurrentSizeInBytes() / 1024 / 1024;
                if (!caching.hasUncachedA18Files()) {
                    int answer = JOptionPane.showOptionDialog(null, "The WAV cache is currently using " + sizeMB
                            + " MB disk space and a cleanup is recommended. Perform cleanup?", "WAV Cache",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.YES_OPTION);
                    JOptionPane.showMessageDialog(null, "Not enough memory available for JVM. Please make sure your"
                            + " java command contains the argument -XMX512m (or more).");

                    if (answer == JOptionPane.YES_OPTION) {
                        ACMConfiguration.getCurrentDB().getRepository().gc();
                    }
                }
            }
            if (ACMConfiguration.getCurrentDB().shouldPreCacheWav()) {
                caching.cacheNewA18Files();
            }
        }
    }

    public static class FilterState {
        private String previousFilterState = null;

        private String filterString;
        private List<Category> filterCategories;
        private List<Locale> filterLanguages;
        private Playlist selectedPlaylist;

        public synchronized String getFilterString() {
            return filterString;
        }
        public synchronized void setFilterString(String filterString) {
            this.filterString = filterString;
            updateResult();
        }
        public synchronized List<Category> getFilterCategories() {
            return filterCategories;
        }
        public synchronized void setFilterCategories(
                List<Category> filterCategories) {
            this.filterCategories = filterCategories;
            updateResult();
        }

        public synchronized List<Locale> getFilterLanguages() {
            return filterLanguages;
        }
        public synchronized void setFilterLanguages(List<Locale> filterLanguages) {
            this.filterLanguages = filterLanguages;
            updateResult();
        }

        public synchronized void setSelectedTag(Playlist selectedPlaylist) {
            this.selectedPlaylist = selectedPlaylist;
            updateResult();
        }

        public synchronized Playlist getSelectedPlaylist() {
            return selectedPlaylist;
        }

        public void updateResult() {
            updateResult(false);
        }

        public void updateResult(boolean force) {
            if (!force && previousFilterState != null && previousFilterState.equals(this.toString())) {
                return;
            }

            previousFilterState = this.toString();

            final IDataRequestResult result;

            if (selectedPlaylist == null) {
                result = DataRequestService.getInstance().getData(
                        LanguageUtil.getUserChoosenLanguage(),
                        filterString, filterCategories, filterLanguages);
            } else {
                result = DataRequestService.getInstance().getData(
                        LanguageUtil.getUserChoosenLanguage(),
                        filterString, selectedPlaylist);
            }

            // call UI back
            Runnable updateUI = new Runnable() {
                @Override
                public void run() {
                    Application.getMessageService().pumpMessage(result);
                }
            };

            if (SwingUtilities.isEventDispatchThread()) {
                updateUI.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(updateUI);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override public String toString() {
            StringBuilder builder = new StringBuilder();
            if (filterString != null) {
                builder.append("FS:").append(filterString);
                builder.append(",");
            }
            if (filterCategories != null && !filterCategories.isEmpty()) {
                for (Category cat : filterCategories) {
                    builder.append("FC:").append(cat.getUuid());
                    builder.append(",");
                }
            }
            if (filterLanguages != null && !filterLanguages.isEmpty()) {
                for (Locale lang : filterLanguages) {
                    builder.append("FL:").append(lang.getLanguage()).append("-").append(lang.getCountry());
                    builder.append(",");
                }
            }
            if (selectedPlaylist != null) {
                builder.append("ST:").append(selectedPlaylist.getName());
                builder.append(",");
            }
            return builder.toString();
        }
    }

}
