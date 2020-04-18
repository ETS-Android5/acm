package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXBusyLabel;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class EmailDialog extends JDialog {
    private String email;
    private JButton okButton;
    private final PlaceholderTextField emailField;

    /**
     * Dialog that displays a line of text with a spinner..
     *
     * @param parent Parent, for positioning.
     * @param email default email address.
     */
    public EmailDialog(Window parent, String email) {
        super(parent, "Amplio Sign In", ModalityType.DOCUMENT_MODAL);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(6, 6, 6, 6);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
        borderPanel.add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        int height = 130;
        JLabel label = new JLabel("Please enter your email address:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);//.setHorizontalAlignment(SwingConstants.CENTER);

        // Email name
        emailField = new PlaceholderTextField();
        emailField.setPlaceholder("Email Address");
        emailField.getDocument().addDocumentListener(emailDocListener);


//        Box hBox = Box.createHorizontalBox();
        okButton = new JButton("OK");
        okButton.addActionListener(this::onCreate);
        okButton.setEnabled(false);
//        hBox.add(okButton);

        setResizable(false);
//        setUndecorated(true);

        dialogPanel.add(Box.createVerticalStrut(5));
        dialogPanel.add(label);
        dialogPanel.add(Box.createVerticalStrut(5));
        dialogPanel.add(emailField);
        dialogPanel.add(Box.createVerticalStrut(5));
        dialogPanel.add(okButton);

        setSize(new Dimension(300, height));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        emailField.setText(email);

        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);
    }

    public String getEmail() {
        return emailField.getText();
    }

    private void onCreate(ActionEvent actionEvent) {
        setVisible(false);
    }

    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DocumentListener emailDocListener = new DocumentListener() {
        private void check() {
            String email = emailField.getText();
            okButton.setEnabled(StringUtils.isNotEmpty(email));
        }
        @Override
        public void insertUpdate(DocumentEvent e) {
            check();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            check();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            check();
        }
    };


}
