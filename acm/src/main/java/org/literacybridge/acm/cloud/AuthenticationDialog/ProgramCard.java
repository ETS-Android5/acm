package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.redBorder;

public class ProgramCard extends CardContent {
    private static final String DIALOG_TITLE = "Select Program";

    private JButton okButton;

    private final JList<String> choicesList;
    private JCheckBox forceSandbox;
    private final JScrollPane choicesListScrollPane;

    public ProgramCard(WelcomeDialog welcomeDialog,
        WelcomeDialog.Cards panel)
    {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel promptLabel = new JLabel("<html>Choose the ACM to open.</html>");

        choicesList = new JList<>();
        choicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addFocusListener(listFocusListener);
        choicesList.addMouseListener(listMouseListener);

        dialogPanel.add(setupButtons(), BorderLayout.SOUTH);
        forceSandbox.setSelected(false);
        JPanel jpanel = new JPanel(new BorderLayout(5, 5));
        jpanel.add(promptLabel, BorderLayout.NORTH);
        choicesListScrollPane = new JScrollPane(choicesList);
        jpanel.add(choicesListScrollPane, BorderLayout.CENTER);
        choicesListScrollPane.setBorder(redBorder);
        dialogPanel.add(jpanel);

        addComponentListener(componentAdapter);
    }
    /**
     * Create sandbox checkbox and the OK and Cancel buttons and their listeners.
     */
    private JComponent setupButtons() {
        Box hbox = Box.createHorizontalBox();
        forceSandbox = new JCheckBox("Use demo mode");
        if (welcomeDialog.options.contains(Authenticator.SigninOptions.OFFER_DEMO_MODE)) {
            hbox.add(forceSandbox);
        }
        hbox.add(Box.createHorizontalGlue());

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        okButton.setEnabled(false);
        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(5));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancel());

        hbox.add(cancelButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.setBorder(new EmptyBorder(4,10,4,8));
        return hbox;
    }

    @Override
    void onShown() {
        // Build the list of ACM names from which to choose. The list depends on whether we're
        // authenticated (not being authenticated is roughly equivalent to not having network
        // access, so we give access to all local programs). The list also depends on the option
        // LOCAL_DATA_ONLY, because the ACM can only work with local data.
        //
        List<String> acmNames;
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            acmNames = new ArrayList<>(welcomeDialog.cognitoInterface.getPrograms().keySet());
            if (welcomeDialog.options.contains(Authenticator.SigninOptions.LOCAL_DATA_ONLY)) {
                List<String> localNames = Authenticator.getInstance().getLocallyAvailablePrograms();
                acmNames = acmNames.stream()
                    .filter(localNames::contains)
                    .collect(Collectors.toList());
            }
        } else {
            acmNames = Authenticator.getInstance().getLocallyAvailablePrograms();
        }
        choicesList.setListData(acmNames.toArray(new String[0]));
    }

    @Override
    void onEnter() {
        onOk();
    }

    void onOk() {
        welcomeDialog.setProgram(getSelectedItem());
        welcomeDialog.setSandboxSelected(forceSandbox.isSelected());
        ok();
    }

    /**
     * Mouse listener so we can accept a match on a double click.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MouseListener listMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) onOk();
        }
    };

    /**
     * Listens for list selection, and enables the OK button whenever an item is selected.
     * @param listSelectionEvent isunused.
     */
    private void listSelectionListener(@SuppressWarnings("unused") ListSelectionEvent listSelectionEvent) {
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        okButton.setEnabled(haveSelection);
        getRootPane().setDefaultButton(haveSelection?okButton:null);
        setListBorder();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final FocusListener listFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            setListBorder();
        }

        @Override
        public void focusLost(FocusEvent e) {
            setListBorder();
        }
    };

    private void setListBorder() {
        boolean haveFocus = choicesList.hasFocus();
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        int ix = (haveSelection?0:2) + (haveFocus?0:1);
        choicesListScrollPane.setBorder(borders[ix]);
    }

    private final Color borderColor = new Color(136, 176, 220);
    private final RoundedLineBorder[] borders = {
        new RoundedLineBorder(borderColor, 2, 6),
        new RoundedLineBorder(borderColor, 1, 6, 2),
        new RoundedLineBorder(Color.RED, 2, 6),
        new RoundedLineBorder(Color.RED, 1, 6, 2)
    };

    public String getSelectedItem() {
        return choicesList.getSelectedValue();
    }

}
