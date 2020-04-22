package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class PromptWelcomePage extends AcmAssistantPage<PromptImportContext> {
    private final PromptFilter promptFilter;

    private final JTable promptTable;
    private final PromptModel promptModel;
    private final JComboBox<String> languageChooser;

    private DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
    private final JScrollPane previewScrollPane;
    private final JCheckBox previewMessage;

    PromptWelcomePage(PageHelper<PromptImportContext> listener) {
        super(listener);
        getProgramInformation();

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the System Prompts Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through importing system prompts for a language. Steps to import prompts:</p>"
            + "<ol>"
            + "<li> Choose the language for which you need to import prompts. Review the prompts that need audio, in the list below.</li>"
            + "<li> Choose the files and folders containing the audio for the prompts.</li>"
            + "<li> The Assistant will automatically make any matches that it can. You then "
            + "have an opportunity to match remaining files, or to \"unmatch\" files as needed.</li>"
            + "<li> You review and approve the final prompt-to-file matches.</li>"
            + "<li> The audio files are copied into the project, and placed in appropriate folders.</li>"
            + "</ol>"

            + "</html>");
        add(welcome, gbc);

        // Language chooser, in a HorizontalBox.
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Choose the Language: "));
        languageChooser = new LanguageChooser();
        fillLanguageChooser();
        languageChooser.addActionListener(this::onSelection);
        hbox.add(languageChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        add(new JLabel("Click 'Next' when you are ready to continue."), gbc);

        gbc.insets.bottom = 0;

        previewMessage = new JCheckBox("Only show Prompts without recordings.", false);
        add(previewMessage, gbc);

        promptModel = new PromptModel();
        promptTable = new JTable(promptModel);
        promptTable.setDefaultRenderer(Object.class, new RecipientCellRenderer());

        TableRowSorter<PromptModel> sorter = new TableRowSorter<>(promptModel);
        sorter.setComparator(0, new PromptImportAssistant.PromptIdSorter());
        promptFilter = new PromptFilter();
        sorter.setRowFilter(promptFilter);
        promptTable.setRowSorter(sorter);
        promptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        previewScrollPane = new JScrollPane(promptTable);
        promptTable.setFillsViewportHeight(true);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(previewScrollPane, gbc);
        previewScrollPane.setVisible(false);
        previewMessage.setVisible(false);

        // Absorb any extra space.
        gbc.weighty = 0.001;
        add(new JLabel(), gbc);

        previewMessage.addActionListener(ev->{
            boolean on = ((JCheckBox)ev.getSource()).isSelected();
            promptFilter.setPredicate(promptId ->
                !on || !context.promptHasRecording.get(promptId));
            promptModel.fireTableDataChanged();
        });
    }

    private void fillLanguageChooser() {
        languageChooser.removeAllItems();
        languageChooser.insertItemAt("Choose...", 0);
        context.configLanguagecodes.forEach(languageChooser::addItem);

        Set<String> languageStrings = context.configLanguagecodes
            .stream()
            .map(AcmAssistantPage::getLanguageAndName)
            .collect(Collectors.toSet());
        setComboWidth(languageChooser, languageStrings, "Choose...");
        languageChooser.setMaximumSize(languageChooser.getPreferredSize());

        if (context.configLanguagecodes.size() == 1) {
            languageChooser.setSelectedIndex(1);
        } else {
            languageChooser.setSelectedIndex(0);
        }
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        if (StringUtils.isNotBlank(context.languagecode)) {
            languageChooser.setSelectedItem(context.languagecode);
        } else {
            languageChooser.setSelectedIndex(0);
        }
        onSelection(null);
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        context.languagecode = getSelectedLanguage();
    }

    @Override
    protected String getTitle() {
        return "Introduction";
    }

    /**
     * Called when a selection changes. Inspect the deployment and language
     * selections, and if both have a selection, enable the "Next" button.
     *
     * @param actionEvent is unused. (Examine entire state when any part changes.
     */
    private void onSelection(@SuppressWarnings("unused") ActionEvent actionEvent) {
        String languagecode = getSelectedLanguage();
        languageChooser.setBorder(languagecode == null ? redBorder : blankBorder);

        if (languagecode != null) {
            context.tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
            context.tbOptionsDir = new File(context.tbLoadersDir, "TB_Options");
            context.languagesDir = new File(context.tbOptionsDir, "languages");
            context.languageDir = new File(context.languagesDir, languagecode);
            context.promptsDir = new File(context.languageDir, "cat");
        }

        fillPromptList();

        setComplete(languagecode != null);
    }

    private String getSelectedLanguage() {
        int langIx = languageChooser.getSelectedIndex();
        if (langIx <= 0) return null;
        return languageChooser.getItemAt(langIx);
    }

    private void getProgramInformation() {
        String project = ACMConfiguration.cannonicalProjectName(dbConfig.getSharedACMname());
        File programSpecDir = ACMConfiguration.getInstance().getProgramSpecDirFor(project);

        context.programSpec = new ProgramSpec(programSpecDir);
        context.specLanguagecodes = context.programSpec.getLanguageCodes();
        // Use a linked hash set to preserve the order of languages, as specified in the config file.
        context.configLanguagecodes = dbConfig.getAudioLanguages()
            .stream()
            .map(Locale::getLanguage)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void fillPromptList() {
        findPromptsWithRecordings();
    }

    private void findPromptsWithRecordings() {
        context.promptHasRecording.clear();
        String languagecode = getSelectedLanguage();
        if (StringUtils.isNotEmpty(languagecode)) {
            for (String promptId : context.promptsInfo.getIds()) {
                PromptsInfo.PromptInfo promptInfo = context.promptsInfo.getPrompt(promptId);
                String filename = promptId + ".a18";
                File promptFile = new File(promptInfo.isTutorialPrompt() ? context.promptsDir : context.languageDir, filename);
                context.promptHasRecording.put(promptId, promptFile.exists());
            }
        }
        promptModel.fireTableDataChanged();
        List<SizingParams> ps = Collections.singletonList(new SizingParams(0, Short.MIN_VALUE, 10, 40));
        sizeColumns(promptTable, ps);

        boolean visible = context.promptHasRecording.size() > 0;
        previewMessage.setVisible(visible);
        previewScrollPane.setVisible(visible);
    }

    private class RecipientCellRenderer extends DefaultTableCellRenderer {
        RecipientCellRenderer() {
            super();
            setOpaque(true);
        }
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            Color bg = (row%2 == 0) ? bgColor : bgAlternateColor;
            if (isSelected) {
                bg = bgSelectionColor;
            }
            super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);
            if (column == 0) {
                int modelRow = promptTable.convertRowIndexToModel(row);
                String id = context.promptsInfo.getIds().get(modelRow);
                setIcon(context.promptHasRecording.getOrDefault(id, true) ? soundImage : noSoundImage);
            } else {
                setIcon(null);
            }
            setBackground(bg);
            return this;
        }
    }

    class PromptFilter extends RowFilter<PromptModel, Integer> {
        private Predicate<String> predicate;
        void setPredicate(Predicate<String> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean include(Entry<? extends PromptModel, ? extends Integer> entry) {
            if (predicate == null) return true;
            int rowIx = entry.getIdentifier();
            String promptId = context.promptsInfo.getIds().get(rowIx);
            return predicate.test(promptId);
        }
    }

    private class PromptModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return context.promptHasRecording.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0: return "Id";
            case 1: return "Filename";
            case 2: return "Definition";
            }
            return "";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String id = context.promptsInfo.getIds().get(rowIndex);
            switch (columnIndex) {
            case 0:
                return id;
            case 1:
                return context.promptsInfo.getPrompt(id).getFilename();
            case 2:
                return context.promptsInfo.getPrompt(id).getText();
            }
            return null;
        }

    }

}
