package org.literacybridge.acm.cloud.AuthenticationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class DialogPanel extends JPanel {

    final WelcomeDialog welcomeDialog;
    final WelcomeDialog.Panels panel;
    private final String dialogTitle;

    DialogPanel(WelcomeDialog welcomeDialog,
        String dialogTitle,
        WelcomeDialog.Panels panel) {
        this.welcomeDialog = welcomeDialog;
        this.dialogTitle = dialogTitle;
        this.panel = panel;

        addComponentListener(componentAdapter);
    }

    DialogPanel(WelcomeDialog welcomeDialog, WelcomeDialog.Panels panel) {
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
