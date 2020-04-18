package org.literacybridge.acm.cloud.AuthenticationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class DialogPanel extends JPanel {

    final DialogController dialogController;
    final DialogController.Panels panel;
    private final String dialogTitle;

    DialogPanel(DialogController dialogController, String dialogTitle, DialogController.Panels panel) {
        this.dialogController = dialogController;
        this.dialogTitle = dialogTitle;
        this.panel = panel;

        addComponentListener(componentAdapter);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    ComponentAdapter componentAdapter = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent evt) {
            dialogController.setTitle(dialogTitle);
            onShown();
        }
    };

    void onShown() {
        // Override as needed
    }

    void onEnter() {
        // Override as needed
    }

    void onCancel(ActionEvent e) {
        // Override as needed
    }

    /**
     * Apache made the ImmutableTriple final, so we can't provide a reasonably named alias.
     *
     * I wonder what they were thinking?
     */
    static class PasswordInfo  {
        public final String password;
        public final Boolean showEnabled;
        public final Boolean showSelected;

        public PasswordInfo(String password, Boolean showEnabled, Boolean showSelected) {
            this.password = password;
            this.showEnabled = showEnabled;
            this.showSelected = showSelected;
        }
    }

}
