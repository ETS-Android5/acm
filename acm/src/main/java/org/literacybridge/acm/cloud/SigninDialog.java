package org.literacybridge.acm.cloud;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public final class SigninDialog extends JDialog {

    private final JCheckBox showPassword;
    private final PlaceholderTextField passwordField;
    private final JCheckBox rememberMe;
    private final PlaceholderTextField usernameField;
    private JLabel authFailureMessage;

    private SigninDialog(Window owner, String prompt) {
        super(owner, String.format("%s Sign In", prompt), ModalityType.DOCUMENT_MODAL);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        JPanel dialogPanel = new JPanel();
        borderPanel.add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // User name
        usernameField = new PlaceholderTextField();
        usernameField.setPlaceholder("User Name or Email Address");
        dialogPanel.add(usernameField, gbc);

        // Password
        passwordField = new PlaceholderTextField();
        passwordField.setPlaceholder("Password");
        passwordField.setMaskChar('*');
        dialogPanel.add(passwordField, gbc);

        // Option checkboxes, and forgot password link.
        Box hBox = Box.createHorizontalBox();
        Box vBox = Box.createVerticalBox();
        showPassword = new JCheckBox("Show password");
        showPassword.addActionListener(this::onShowPassword);
        vBox.add(showPassword);
        rememberMe = new JCheckBox("Remember me");
        rememberMe.addActionListener(this::onRememberMe);
        vBox.add(rememberMe);
        hBox.add(vBox);
        hBox.add(Box.createHorizontalGlue());

        ActionLabel forgotPassword = new ActionLabel("Forgot password?");
        forgotPassword.addActionListener(this::onForgotPassword);

        hBox.add(forgotPassword);
        hBox.add(Box.createHorizontalStrut(10));

        dialogPanel.add(hBox, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        JButton signIn = new JButton("Sign In");
        signIn.addActionListener(this::onOk);
        hBox.add(signIn);
        hBox.add(Box.createHorizontalGlue());          
        ActionLabel signUp = new ActionLabel("No user id? Click here!");
        signUp.addActionListener(this::onSignUp);

        hBox.add(signUp);
        hBox.add(Box.createHorizontalStrut(10));

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        SwingUtils.addEscapeListener(this);
        setMinimumSize(new Dimension(450, 250));

    }

    private void setMessage(String message) {
        if (authFailureMessage == null) {
            authFailureMessage = new JLabel();
            authFailureMessage.setBorder(new EmptyBorder(5,10,10, 5));
            add(authFailureMessage, BorderLayout.SOUTH);
        }
        authFailureMessage.setText(message);
    }

    private void onSignUp(ActionEvent actionEvent) {
        JOptionPane.showMessageDialog(this, "Please use the Amplio Dashboard to create a new User Id.",
            "Create User Id", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onForgotPassword(ActionEvent actionEvent) {
        if (StringUtils.isEmpty(usernameField.getText())) {
            setMessage("Please enter the user id or email for which to reset the password.");
            return;
        }
        setMessage(null);
//        Authenticator.getInstance().resetPassword(usernameField.getText());
        ResetDialog.showDialog(this, usernameField.getText());
    }

    private void onRememberMe(ActionEvent actionEvent) {

    }

    private void onShowPassword(ActionEvent actionEvent) {
        passwordField.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }

    /**
     * If the user made any net changes, commit them.
     *
     * @param e is unused.
     */
    private void onOk(ActionEvent e) {
        Authenticator authenticator = Authenticator.getInstance();
        setMessage(null);
        authenticator.authenticate(usernameField.getText(), passwordField.getText());

        if (!authenticator.isAuthenticated()) {
            setMessage(authenticator.getAuthMessage());
        } else {
            setVisible(false);
        }
    }

    public static boolean showDialog(Window parent, String prompt) {
        SigninDialog dialog = new SigninDialog(parent, prompt);
        dialog.setVisible(true);
        return Authenticator.getInstance().isAuthenticated();
    }

    public static boolean showDialog(Window parent) {
        return showDialog(parent, "Amplio");
    }

}
