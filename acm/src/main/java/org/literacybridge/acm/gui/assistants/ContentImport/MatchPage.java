package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.core.spec.ContentSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class MatchPage extends AssistantPage<ContentImportContext> {
    private static final int DEFAULT_THRESHOLD = 80;
    private static final int MINIMUM_THRESHOLD = 60;
    public static int fuzzyThreshold = DEFAULT_THRESHOLD;

    private final JLabel deployment;
    private final JLabel language;

    private ContentImportContext context;
    private MetadataStore store = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore();
    private MatcherTable table;
    private MatcherTableModel model;
    private JButton unMatch;
    private JButton manualMatch;

    static {
        // Configure the renderers for the match table.
        MatchTableRenderers.colorCodeMatches = false;
        MatchTableRenderers.isColorCoded = true;
    }

    public MatchPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets tight = new Insets(0, 0, 5, 0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.LINE_START,
            GridBagConstraints.BOTH,
            tight,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with content. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Next\" to continue.</p>"
                + "</html>");
        add(welcome, gbc);


        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = parameterText();
        hbox.add(language);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        gbc.weighty = 1.0;
        add(makeTable(), gbc);
        gbc.weighty = 0;

        add(makeManualMatchButtons(), gbc);

    }

    /**
     * Creates the table and sets the model and various renderers, filters, etc.
     */
    private Component makeTable() {
        table = new MatcherTable();
        model = table.getModel();

        table.setFilter(this::filter);

        MatchTableRenderers mtr = new MatchTableRenderers(table);
        table.setRenderer(MatcherTableModel.Columns.Left, mtr.getAudioItemRenderer());
        table.setRenderer(MatcherTableModel.Columns.Update, mtr.getUpdatableRenderer());
        table.setRenderer(MatcherTableModel.Columns.Status, mtr.getStatusRenderer());
        table.setRenderer(MatcherTableModel.Columns.Right, mtr.getMatcherRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(224, 224, 224));
        table.setGridColor(Color.RED);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        table.addMouseListener(tableMouseListener);

        return scrollPane;
    }

    /**
     * The filter for showing and hiding matched columns.
     *
     * @param t   The item to be filtered.
     * @param <T> Type of the item, some subclass of MatchableItem.
     * @return true if the item shoudl be shown.
     */
    private <T extends MatchableItem<?, ?>> boolean filter(T t) {
        MATCH match = t.getMatch();
        return match != MATCH.NONE;
    }

    private Box makeManualMatchButtons() {
        // Control buttons
        Box hbox = Box.createHorizontalBox();
        unMatch = new JButton("Unmatch");
        unMatch.addActionListener(this::onUnMatch);
        unMatch.setToolTipText("Unmatch the Audio Item from the File.");
        hbox.add(unMatch);
        hbox.add(Box.createHorizontalStrut(5));
        manualMatch = new JButton("Manual Match");
        manualMatch.addActionListener(this::onManualMatch);
        manualMatch.setToolTipText("Manually match this file with an message.");
        hbox.add(manualMatch);
        hbox.add(Box.createHorizontalStrut(5));

        unMatch.setEnabled(false);
        manualMatch.setEnabled(false);

        hbox.add(Box.createHorizontalGlue());

        table.getSelectionModel().addListSelectionListener(ev -> enableButtons());

        return hbox;
    }

    MouseListener tableMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                MatchableImportableAudio selectedRow = selectedRow();
                if (selectedRow != null && selectedRow.getMatch().isSingle()) {
                    onManualMatch(null);
                }
            }
        }
    };

    private void onUnMatch(ActionEvent actionEvent) {
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            MatchableImportableAudio row = model.getRowAt(modelRow);
            if (row != null && row.getMatch().isMatch()) {
                context.matcher.unMatch(modelRow);
                model.fireTableDataChanged();
            }
        }
    }

    private void onManualMatch(ActionEvent actionEvent) {
        MatchableImportableAudio selectedRow = selectedRow();
        MatchableImportableAudio chosenMatch = null;

        ManualMatchDialog dialog = new ManualMatchDialog();
        if (selectedRow.getMatch()==MATCH.LEFT_ONLY) {
            chosenMatch = dialog.getFileForAudioItem(selectedRow,
                context.matcher.matchableItems);
        } else if (selectedRow.getMatch()==MATCH.RIGHT_ONLY) {
            chosenMatch = dialog.getAudioItemForFile(selectedRow,
                context.matcher.matchableItems);
        }

        if (chosenMatch != null) {
            context.matcher.setMatch(chosenMatch, selectedRow);
            model.fireTableDataChanged();
        }
    }

    private void enableButtons() {
        MatchableImportableAudio row = selectedRow();
        unMatch.setEnabled(row != null && row.getMatch().isMatch());
        manualMatch.setEnabled(row != null && !row.getMatch().isMatch());
    }
    
    private MatchableImportableAudio selectedRow() {
        MatchableImportableAudio row = null;
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            row = model.getRowAt(modelRow);
        }
        return row;
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        String languagecode = context.languagecode;

        // Fill deployment and language
        deployment.setText(Integer.toString(context.deploymentNo));
        language.setText(languagecode);

        int deploymentNo = Integer.parseInt(deployment.getText());

        // List of titles (left side list)
        List<ImportableAudioItem> titles = new ArrayList<>();
        List<ContentSpec.PlaylistSpec> contentPlaylistSpecs = context.programSpec.getContentSpec()
                                                                                 .getDeployment(deploymentNo)
                                                                                 .getPlaylistSpecs();
        for (ContentSpec.PlaylistSpec contentPlaylistSpec : contentPlaylistSpecs) {
            String playlistName = WelcomePage.qualifiedPlaylistName(contentPlaylistSpec.getPlaylistTitle(), deploymentNo, languagecode);
            Playlist playlist = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().findPlaylistByName(playlistName);
            for (ContentSpec.MessageSpec messageSpec : contentPlaylistSpec.getMessagesForLanguage(languagecode)) {
                ImportableAudioItem importableAudio = new ImportableAudioItem(messageSpec, playlist);
                // See if we already have this title in the ACM.
                AudioItem audioItem = findAudioItemForTitle(messageSpec.title, languagecode);
                importableAudio.setItem(audioItem);
                titles.add(importableAudio);
            }
        }
        // List of files (right side list)
        List<ImportableFile> files = context.importableFiles.stream()
            .map(ImportableFile::new)
            .collect(Collectors.toList());

        if (progressing) {
            context.matcher.setData(titles, files, MatchableImportableAudio::new);
            model.setData(context.matcher.matchableItems);
            autoMatch();
        } else {
            model.fireTableDataChanged();
        }

        setComplete();
    }
    
    /**
     * Performs a sequence of exact/fuzzy/token matches (using the current value of threshold),
     * and then sorts the contents.
     * @return a MatchStats describing how many matches were made.
     */
    Matcher.MatchStats autoMatch() {
        Matcher.MatchStats result = new Matcher.MatchStats();
        if (context.matcher != null) {
            result.add(context.matcher.findExactMatches());
            result.add(context.matcher.findFuzzyMatches(fuzzyThreshold));
            result.add(context.matcher.findTokenMatches(fuzzyThreshold));
            context.matcher.sort();
            model.fireTableDataChanged();
            System.out.println(result.toString());
        }
        return result;
    }

    /**
     * Given a message title (ie, from the Program Spec), see if we already have such an
     * audio item in the desired language.
     * @param title The title to search for.
     * @param languagecode The language in which we want the audio item.
     * @return the AudioItem if it exists, otherwise null.
     */
    private AudioItem findAudioItemForTitle(String title, String languagecode) {
        List<Category> categoryList = new ArrayList<>();
        List<Locale> localeList = Arrays.asList(new RFC3066LanguageCode(languagecode).getLocale());

        SearchResult searchResult = store.search(title, categoryList, localeList);
        // Filter because search will return near matches.
        AudioItem item = searchResult.getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .filter(i->i.getTitle().equals(title))
            .findAny()
            .orElse(null);
        return item;
    }

    @Override
    protected String getTitle() {
        return "Auto-match Files with Content";
    }
}
