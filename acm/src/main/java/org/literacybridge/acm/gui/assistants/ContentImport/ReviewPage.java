package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ReviewPage extends AssistantPage<ContentImportContext> {

    private final DefaultListModel<String> importPreviewModel;
    private ContentImportContext context;

    ReviewPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,15,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/>When you are satisfied with these imports, click \"Finish\" to perform the import. "

                + "</html>");
        add(welcome, gbc);

        // Title preview.
        JLabel importPreviewLabel = new JLabel("Files to be imported:");
        insets = new Insets(0,0,00,0);
        gbc.insets = insets;
        add(importPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));
        importPreviewModel = new DefaultListModel<>();
        JList<String> importPreview = new JList<>(importPreviewModel);
        JScrollPane importPreviewScroller = new JScrollPane(importPreview);
        panel.add(importPreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        insets = new Insets(0,10,20,30);
        gbc.insets = insets;
        add(panel, gbc);

        // Absorb any vertical space.
        //gbc.weighty = 1.0;
        //add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;
        importPreviewModel.clear();
        // For the imports, create a "item from \n file" label, and add to the preview.
        matcher.matchableItems.stream()
            .filter(item -> item.getMatch().isMatch() && item.getLeft().isImportable())
            .map(this::reviewString)
            .forEach(importPreviewModel::addElement);

        if (importPreviewModel.isEmpty()) {
            String noItems = "<html><i><span style='font-weight:100;font-family:Helvetica;font-size:2.0em;'>"
                + "No files selected to import."
                + "</span></i></html>";
            importPreviewModel.addElement(noItems);
        }

        setComplete();
    }

    /**
     * Formats one import item, with audio name, import operation, and file name.
     * @param importable the MatchableImportableAudio
     * @return a string describing the operation.
     */
    private String reviewString(MatchableImportableAudio importable) {
        return String.format("<html>"
                + "%s"  // audio title
                + "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + "<i>%s "  // operation (update / import)
                + "from </i>"
                + "---&gt;&nbsp;&nbsp;"
                + "<span style='font-family:Courier'>"
                + "%s"  // file name
                + "</span></html>",
            importable.getLeft(),
            importable.getOperation(),
            importable.getRight().getFile().getName());
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Review Files to Import";
    }

}
