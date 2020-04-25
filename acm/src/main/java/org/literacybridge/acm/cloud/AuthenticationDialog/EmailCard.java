package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class EmailCard extends CardContent {
    private static final String DIALOG_TITLE = "Enter email address";

    private final JButton okButton;
    private final PlaceholderTextField emailField;

    public EmailCard(WelcomeDialog welcomeDialog,
        WelcomeDialog.Cards panel)
    {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // User name
        emailField = new PlaceholderTextField();
        emailField.setPlaceholder("Email Address");
        emailField.addKeyListener(textKeyListener);
        emailField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(emailField, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        Box hBox = Box.createHorizontalBox();
        okButton = new JButton("OK");
        okButton.addActionListener(this::onOk);
        okButton.setEnabled(false);
        hBox.add(okButton);
        hBox.add(Box.createHorizontalGlue());

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    @Override
    void onShown() {
        emailField.setText(welcomeDialog.getEmail());
    }

    /**
     * User clicked "Sign in" pressed enter.
     * @param actionEvent is ignored.
     */
    private void onOk(ActionEvent actionEvent) {
        welcomeDialog.setEmail(emailField.getText());
        ok();
    }

    /**
     * Sets the enabled state of controls, based on which other controls have contents.
     */
    private void enableControls() {
        okButton.setEnabled(emailField.getText().length() > 0);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final KeyListener textKeyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
            enableControls();
        }
    };

    /**
     * Used to enable the ok button if an email address is pasted into the email field (because
     * we're not listening to that key, we'd otherwise miss the presence of the email address).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener textDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            enableControls();
        }
        @Override
        public void removeUpdate(DocumentEvent e) {
            enableControls();
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            enableControls();
        }
    };

}
