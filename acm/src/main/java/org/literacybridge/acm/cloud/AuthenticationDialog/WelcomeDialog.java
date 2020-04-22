package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.Arrays.stream;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Panels.*;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class WelcomeDialog extends JDialog {
    private String username;
    private String email;
    private String password;
    private boolean isSavedPassword;

    private boolean success = false;
    private boolean offlineEmailChoice = true;

    public boolean isSuccess() {
        return success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
        this.isSavedPassword = false;
    }

    public boolean isSavedPassword() {
        return isSavedPassword;
    }

    enum Panels {
        NullPanel(100, DialogPanel::new),
        SignInPanel(260, SignInPanel::new),
        SignUpPanel(320, SignUpPanel::new),
        ResetPanel(280, ResetPanel::new),
        ConfirmPanel(450, ConfirmPanel::new),
        EmailPanel(130, EmailPanel::new),
        ProgramPanel(450, ProgramPanel::new);

        int minimumHeight;
        BiFunction<WelcomeDialog, Panels, DialogPanel> ctor;

        Panels(int minimumHeight, BiFunction<WelcomeDialog, Panels, DialogPanel> ctor) {
            this.minimumHeight = minimumHeight;
            this.ctor = ctor;
        }
    }

    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private JLabel authMessage;
    private final Map<Panels,DialogPanel> cardPanelMap = new HashMap<>();
    private DialogPanel currentPanel = null;

    final Authenticator.CognitoInterface cognitoInterface;
    final Set<Authenticator.SigninOptions> options;

    private void makePanel(Panels panel) {
        DialogPanel newPanel = panel.ctor.apply(this, panel);
        cardPanel.add(panel.name(), newPanel);
        cardPanelMap.put(panel, newPanel);
    }

    public WelcomeDialog(Window owner, Set<Authenticator.SigninOptions> options, Authenticator.CognitoInterface cognitoInterface) {
        super(owner, "Amplio Sign In", ModalityType.DOCUMENT_MODAL);
        this.cognitoInterface = cognitoInterface;
        this.options = options;

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
        if (cognitoInterface.isOnline()) {
            makePanel(SignInPanel);
        } else {
            makePanel(EmailPanel);
        }
        currentPanel = new ArrayList<>(cardPanelMap.values()).get(0);
//        stream(Panels.values()).forEach(this::makePanel);

        ActionListener escListener = e -> currentPanel.onCancel(e);

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = e -> currentPanel.onEnter();

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);

        // Defer activating the card, so that post construction properties can be set.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                dialogShown();
            }
        });

        // Center horizontally and in the top 2/3 of screen.
        setMinimumSize(new Dimension(450, currentPanel.panel.minimumHeight));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);
    }

    private void dialogShown() {
        currentPanel.onShown();
    }



    private SignInPanel signInPanel() {
        return ((SignInPanel)cardPanelMap.get(SignInPanel));
    }
    private SignUpPanel signUpPanel() {
        return ((SignUpPanel)cardPanelMap.get(SignUpPanel));
    }
    private ConfirmPanel confirmPanel() {
        return ((ConfirmPanel)cardPanelMap.get(ConfirmPanel));
    }
    private ResetPanel resetPanel() {
        return ((ResetPanel)cardPanelMap.get(ResetPanel));
    }
    private EmailPanel emailPanel() {
        return ((EmailPanel)cardPanelMap.get(EmailPanel));
    }
    private ProgramPanel programPanel() {
        return ((ProgramPanel)cardPanelMap.get(ProgramPanel));
    }

    public boolean isRememberMeSelected() {
        return signInPanel().isRememberMeSelected();
    }

    public String getPassword() {
        return password;
    }

    public void setSavedCredentials(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.isSavedPassword = true;
    }

    private void activateCard(Panels newPanel) {
        if (!cardPanelMap.containsKey(newPanel)) {
            makePanel(newPanel);
        }
        int deltaFromNominal = getHeight()-currentPanel.panel.minimumHeight;
        System.out.printf("transition card %s -> %s, cur height: %d, cur min: %d, new min: %d, delta: %d\n",
            currentPanel.panel.name(), newPanel.name(),
            getHeight(),
            currentPanel.panel.minimumHeight,
            newPanel.minimumHeight,
            deltaFromNominal);
        if (getHeight() != newPanel.minimumHeight+deltaFromNominal) {
            setMinimumSize(new Dimension(getWidth(), newPanel.minimumHeight));
            setSize(new Dimension(getWidth(), newPanel.minimumHeight+deltaFromNominal));
        }
        currentPanel = cardPanelMap.get(newPanel);
        cardLayout.show(cardPanel, newPanel.name());
        currentPanel.onShown();
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
        activateCard(SignUpPanel);
    }

    void gotoResetCard() {
        activateCard(ResetPanel);
    }

    void gotoConfirmationCard() {
        activateCard(ConfirmPanel);
    }

    void gotoProgramSelection() {
        if (options.contains(Authenticator.SigninOptions.CHOOSE_PROGRAM)) {
            activateCard(ProgramPanel);
        } else {
            success = true;
            setVisible(false);
        }
    }

    /**
     * Called by the sub-panels when user clicks OK on the panel.
     * @param senderPanel that clicked OK.
     */
    void ok(DialogPanel senderPanel) {
        switch (senderPanel.panel) {
        case SignInPanel:
        case EmailPanel:
            gotoProgramSelection();
            break;

        case ResetPanel:
        case ConfirmPanel:
            activateCard(SignInPanel);
            break;

        case ProgramPanel:
            success = true;
            setVisible(false);
            break;
        }
    }

    /**
     * Called by the sub-panels when user cancels. The result may just be to
     * switch back to the sign-in panel, or may be to cancel.
     * @param senderPanel that clicked cancel.
     */
    void cancel(DialogPanel senderPanel) {
        switch (senderPanel.panel) {
        case SignInPanel:
            setVisible(false);
            break;

        case SignUpPanel:
        case ResetPanel:
            activateCard(SignInPanel);
            break;

        case ConfirmPanel:
        case EmailPanel:
        case ProgramPanel:
            setVisible(false);
            break;
        }
    }

    void SdkClientException(DialogPanel senderPanel) {
        if (senderPanel.panel == SignInPanel) {
            if (options.contains(Authenticator.SigninOptions.OFFLINE_EMAIL_CHOICE)) {
                activateCard(EmailPanel);
            } else if (StringUtils.isNotBlank(email)) {
                activateCard(ProgramPanel);
            } else {
                // Ends the dialog, with failure.
                setVisible(false);
            }
        }
    }
}
