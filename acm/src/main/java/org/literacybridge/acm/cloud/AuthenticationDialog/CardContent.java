package org.literacybridge.acm.cloud.AuthenticationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class CardContent extends JPanel {

    final WelcomeDialog welcomeDialog;
    final WelcomeDialog.Cards panel;
    private final String dialogTitle;

    CardContent(WelcomeDialog welcomeDialog,
        String dialogTitle,
        WelcomeDialog.Cards panel) {
        this.welcomeDialog = welcomeDialog;
        this.dialogTitle = dialogTitle;
        this.panel = panel;

        addComponentListener(componentAdapter);
    }

    CardContent(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        this(welcomeDialog, "Authentication", panel);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    ComponentAdapter componentAdapter = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent evt) {
            welcomeDialog.setTitle(dialogTitle);
//            onShown();
        }
    };

    /**
     * Called when the card is shown.
     */
    void onShown() {
        // Override as needed
    }

    void onEnter() {
        // Override as needed
    }

    void onCancel(ActionEvent e) {
        // Override as needed
    }

    void ok() {
        welcomeDialog.ok(this);
    }
    void cancel() {
        welcomeDialog.cancel(this);
    }


}
