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

import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ConfirmCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.EmailCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ProgramCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ResetCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.SignInCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.SignUpCard;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class WelcomeDialog extends JDialog {
    private String username;
    private String email;
    private String password;
    private boolean isSavedPassword;
    private String program;
    private boolean isSandboxSelected;

    private boolean success = false;

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

    public String getProgram() {
        return program;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setPassword(String password) {
        this.password = password;
        this.isSavedPassword = false;
    }

    void setProgram(String program) {
        this.program = program;
    }

    public boolean isSavedPassword() {
        return isSavedPassword;
    }

    public boolean isSandboxSelected() {
        return isSandboxSelected;
    }
    void setSandboxSelected(boolean sandboxSelected) {
        isSandboxSelected = sandboxSelected;
    }

    enum Cards {
        NullCard(100, CardContent::new),
        SignInCard(260, SignInCard::new),
        SignUpCard(320, SignUpCard::new),
        ResetCard(280, ResetCard::new),
        ConfirmCard(450, ConfirmCard::new),
        EmailCard(130, EmailCard::new),
        ProgramCard(450, ProgramCard::new);

        int minimumHeight;
        BiFunction<WelcomeDialog, Cards, CardContent> ctor;

        Cards(int minimumHeight, BiFunction<WelcomeDialog, Cards, CardContent> ctor) {
            this.minimumHeight = minimumHeight;
            this.ctor = ctor;
        }
    }

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;

    private JLabel authMessage;
    private final Map<Cards, CardContent> cardMap = new HashMap<>();
    private CardContent currentCard;

    final Authenticator.CognitoInterface cognitoInterface;
    final Set<Authenticator.SigninOptions> options;

    private void makePanel(Cards card) {
        CardContent newCard = card.ctor.apply(this, card);
        cardsContainer.add(card.name(), newCard);
        cardMap.put(card, newCard);
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
        cardsContainer = new JPanel(cardLayout);
        borderPanel.add(cardsContainer, BorderLayout.CENTER);
        cardsContainer.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Create the panels, add them to the cardPanel, and to the dialogPanelMap.
        if (cognitoInterface.isOnline()) {
            makePanel(SignInCard);
        } else {
            makePanel(EmailCard);
        }
        currentCard = new ArrayList<>(cardMap.values()).get(0);
//        stream(Panels.values()).forEach(this::makePanel);

        ActionListener escListener = e -> currentCard.onCancel(e);

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = e -> currentCard.onEnter();

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
        setMinimumSize(new Dimension(450, currentCard.panel.minimumHeight));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);
    }

    private void dialogShown() {
        currentCard.onShown();
    }



    private org.literacybridge.acm.cloud.AuthenticationDialog.SignInCard signInCard() {
        return ((org.literacybridge.acm.cloud.AuthenticationDialog.SignInCard) cardMap.get(SignInCard));
    }
//    private org.literacybridge.acm.cloud.AuthenticationDialog.SignUpCard signUpCard() {
//        return ((org.literacybridge.acm.cloud.AuthenticationDialog.SignUpCard) cardMap.get(SignUpCard));
//    }
//    private org.literacybridge.acm.cloud.AuthenticationDialog.ConfirmCard confirmCard() {
//        return ((org.literacybridge.acm.cloud.AuthenticationDialog.ConfirmCard) cardMap.get(ConfirmCard));
//    }
//    private org.literacybridge.acm.cloud.AuthenticationDialog.ResetCard resetCard() {
//        return ((org.literacybridge.acm.cloud.AuthenticationDialog.ResetCard) cardMap.get(ResetCard));
//    }
//    private org.literacybridge.acm.cloud.AuthenticationDialog.EmailCard emailCard() {
//        return ((org.literacybridge.acm.cloud.AuthenticationDialog.EmailCard) cardMap.get(EmailCard));
//    }
//    private org.literacybridge.acm.cloud.AuthenticationDialog.ProgramCard programCard() {
//        return ((org.literacybridge.acm.cloud.AuthenticationDialog.ProgramCard) cardMap.get(ProgramCard));
//    }

    public boolean isRememberMeSelected() {
        return signInCard().isRememberMeSelected();
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

    private void activateCard(Cards newPanel) {
        if (!cardMap.containsKey(newPanel)) {
            makePanel(newPanel);
        }
        int deltaFromNominal = getHeight()- currentCard.panel.minimumHeight;
        System.out.printf("transition card %s -> %s, cur height: %d, cur min: %d, new min: %d, delta: %d\n",
            currentCard.panel.name(), newPanel.name(),
            getHeight(),
            currentCard.panel.minimumHeight,
            newPanel.minimumHeight,
            deltaFromNominal);
        if (getHeight() != newPanel.minimumHeight+deltaFromNominal) {
            setMinimumSize(new Dimension(getWidth(), newPanel.minimumHeight));
            setSize(new Dimension(getWidth(), newPanel.minimumHeight+deltaFromNominal));
        }
        currentCard = cardMap.get(newPanel);
        cardLayout.show(cardsContainer, newPanel.name());
        currentCard.onShown();
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
        activateCard(SignUpCard);
    }

    void gotoResetCard() {
        activateCard(ResetCard);
    }

    void gotoConfirmationCard() {
        activateCard(ConfirmCard);
    }

    void gotoProgramSelection() {
        if (options.contains(Authenticator.SigninOptions.CHOOSE_PROGRAM)) {
            activateCard(ProgramCard);
        } else {
            success = true;
            setVisible(false);
        }
    }

    /**
     * Called by the sub-panels when user clicks OK on the panel.
     * @param senderPanel that clicked OK.
     */
    void ok(CardContent senderPanel) {
        switch (senderPanel.panel) {
        case SignInCard:
        case EmailCard:
            gotoProgramSelection();
            break;

        case ResetCard:
        case ConfirmCard:
            activateCard(SignInCard);
            break;

        case ProgramCard:
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
    void cancel(CardContent senderPanel) {
        switch (senderPanel.panel) {
        case SignInCard:

        case ConfirmCard:
        case EmailCard:
        case ProgramCard:
            setVisible(false);
            break;

        case SignUpCard:
        case ResetCard:
            activateCard(SignInCard);
            break;
        }
    }

    void SdkClientException(CardContent senderPanel) {
        if (senderPanel.panel == SignInCard) {
            if (options.contains(Authenticator.SigninOptions.OFFLINE_EMAIL_CHOICE)) {
                activateCard(EmailCard);
            } else if (StringUtils.isNotBlank(email)) {
                activateCard(ProgramCard);
            } else {
                // Ends the dialog, with failure.
                setVisible(false);
            }
        }
    }
}
