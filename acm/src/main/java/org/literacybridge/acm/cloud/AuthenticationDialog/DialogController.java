package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.AuthenticationDialog.DialogPanel.PasswordInfo;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Arrays.stream;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class DialogController extends JDialog {
    enum Panels {
        SignInPanel(450, SignInPanel::new),
        SignUpPanel(320, SignUpPanel::new),
        ResetPanel(280, ResetPanel::new),
        ConfirmPanel(450, ConfirmPanel::new),
        EmailPanel(100, EmailPanel::new),
        ProgramPanel(100, ProgramPanel::new);

        int minimumHeight;
        BiFunction<DialogController, Panels, DialogPanel> ctor;

        Panels(int minimumHeight, BiFunction<DialogController, Panels, DialogPanel> ctor) {
            this.minimumHeight = minimumHeight;
            this.ctor = ctor;
        }
    }

    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private JLabel authMessage;
    private final Map<Panels,DialogPanel> cardPanelMap = new HashMap<>();
    private DialogPanel currentPanel = null;

    Authenticator.CognitoInterface cognitoInterface;

    private void makePanel(Panels panel) {
        DialogPanel newPanel = panel.ctor.apply(this, panel);
        cardPanel.add(panel.name(), newPanel);
        cardPanelMap.put(panel, newPanel);
    }

    public DialogController(Window owner, Authenticator.CognitoInterface cognitoInterface) {
        super(owner, "Amplio Sign In", ModalityType.DOCUMENT_MODAL);
        this.cognitoInterface = cognitoInterface;

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        borderPanel.add(cardPanel, BorderLayout.CENTER);
        cardPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Create the panels, add them to the cardPanel, and to the dialogPanelMap.
        stream(Panels.values()).forEach(this::makePanel);

        ActionListener escListener = e -> currentPanel.onCancel(e);

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = e -> currentPanel.onEnter();

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);


        activateCard(Panels.SignInPanel);

        // Center horizontally and in the top 2/3 of screen.
        setMinimumSize(new Dimension(450, 250));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);

    }

    private SignInPanel signInPanel() {
        return ((SignInPanel)cardPanelMap.get(Panels.SignInPanel));
    }
    private SignUpPanel signUpPanel() {
        return ((SignUpPanel)cardPanelMap.get(Panels.SignUpPanel));
    }
    private ResetPanel resetPanel() {
        return ((ResetPanel)cardPanelMap.get(Panels.ResetPanel));
    }
    public boolean isRememberMeSelected() {
        return signInPanel().isRememberMeSelected();
    }

    public String getUsername() {
        return signInPanel().getUsername();
    }

    String getNewUsername() {
        return signUpPanel().getUsername();
    }

    public String getPassword() {
        return signInPanel().getPassword();
    }

    public void setSavedCredentials(String username, String password) {
        signInPanel().setSavedCredentials(username, password);
    }

    private void activateCard(Panels panel) {
        currentPanel = cardPanelMap.get(panel);
        cardLayout.show(cardPanel, panel.name());
    }

    /**
     * Displays a message to the user when the sign-in fails.
     * @param message to be shown.
     */
    void setMessage(String message) {
        if (authMessage == null) {
            authMessage = new JLabel();
            authMessage.setBorder(new EmptyBorder(5,10,10, 5));
            add(authMessage, BorderLayout.SOUTH);
        }
        authMessage.setText(message);
    }

    void clearMessage() {
        if (authMessage != null) {
            UIUtils.setLabelText(authMessage, "");
        }
    }

    void gotoSignUpCard() {
        if (getHeight() < 320) {
            setMinimumSize(new Dimension(getWidth(), 320));
        }
        activateCard(Panels.SignUpPanel);
    }

    void gotoResetCard() {
        if (getHeight() < 280) {
            setMinimumSize(new Dimension(getWidth(), 280));
        }
        activateCard(Panels.ResetPanel);
    }

    void gotoConfirmationCard() {
        activateCard(Panels.ConfirmPanel);
    }

    /**
     * Called by the sub-panels when user clicks OK on the panel.
     * @param panel that clicked OK.
     */
    void ok(DialogPanel panel) {
        if (panel.panel == Panels.SignInPanel) {
            setVisible(false);
        } else if (panel.panel == Panels.ResetPanel) {
            PasswordInfo pwd = resetPanel().getPassword();
            signInPanel().setPassword(pwd);
            activateCard(Panels.SignInPanel);
        } else if (panel.panel == Panels.ConfirmPanel) {
            PasswordInfo pwd = signUpPanel().getPassword();
            signInPanel().setPassword(pwd);
            String username = signUpPanel().getUsername();
            signInPanel().setUsername(username);
            activateCard(Panels.SignInPanel);
        }
    }

    /**
     * Called by the sub-panels when user cancels. The result may just be to
     * switch back to the sign-in panel, or may be to cancel.
     * @param panel that clicked cancel.
     */
    void cancel(DialogPanel panel) {
        if (panel.panel == Panels.SignInPanel) {
            setVisible(false);
        }
        activateCard(Panels.SignInPanel);
    }
}
