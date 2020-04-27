package org.literacybridge.acm.gui.Assistant;

import org.literacybridge.acm.gui.UIConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlexTextField extends PlaceholderTextField {
    private IconHelper helper;
    private IconHelper getHelper() {
        if (helper == null) {
            helper = new IconHelper(this);
            helper.setClickHandler(this::onClicked);
        }
        return helper;
    }

    private boolean isPassword = false;
    private boolean revealPasswordEnabled = false;
    private boolean isPasswordRevealed = false;

    private final ImageIcon eyeIcon = new ImageIcon(UIConstants.getResource("eye_256.png"));
    private final ImageIcon noEyeIcon = new ImageIcon(UIConstants.getResource("no-eye_256.png"));

    public FlexTextField() {
        this(null, null, 0);
    }

    public FlexTextField(Document pDoc, String pText, int pColumns)
    {
        super(pDoc, pText, pColumns);
    }

    public FlexTextField(int pColumns) {
        this(null, null, pColumns);
    }

    public FlexTextField(String pText) {
        this(null, pText, 0);
    }

    public FlexTextField(String pText, int pColumns) {
        this(null, pText, pColumns);
    }

    @Override
    protected void paintComponent(Graphics pG) {
        super.paintComponent(pG);
        getHelper().onPaintComponent(pG);
    }

    public void setIcon(ImageIcon icon) {
        getHelper().onSetIcon(icon);
    }

    public void setIconSpacing(int spacing) {
        getHelper().onSetIconSpacing(spacing);
    }

    public void setIconRight(boolean right) {
        getHelper().onSetRight(right);
    }

    @Override
    public void setBorder(Border border) {
        getHelper().onSetBorder(border);
        super.setBorder(getHelper().getBorder());
    }

    public void setGreyBorder() {
        getHelper().onSetGreyBorder();
    }

    public boolean isPassword() {
        return isPassword;
    }

    public void setIsPassword(boolean password) {
        if (isPassword != password) {
            if (password) {
                revealPasswordEnabled = false;
                isPasswordRevealed = false;
                setMaskChar('*');
            } else {
                setMaskChar((char)'\0');
            }
            setIcon(null);
            System.out.printf("Set null 2 icon\n");
            isPassword = password;
        }
    }

    public boolean isRevealPasswordEnabled() {
        return revealPasswordEnabled;
    }

    public void setRevealPasswordEnabled(boolean revealPasswordEnabled) {
        if (this.revealPasswordEnabled != revealPasswordEnabled) {
            if (revealPasswordEnabled) {
                setIconRight(true);
                setIcon(eyeIcon);
                System.out.printf("Set eye 1 icon\n");
            } else {
                setIcon(null);
                System.out.printf("Set null 3 icon\n");
            }
            this.revealPasswordEnabled = revealPasswordEnabled;
        }
    }

    public boolean isPasswordRevealed() {
        return isPasswordRevealed;
    }

    public void setPasswordRevealed(boolean passwordRevealed) {
        isPasswordRevealed = passwordRevealed;
    }

    private void revealPassword(boolean reveal) {
        if (reveal == isPasswordRevealed) return;
        isPasswordRevealed = reveal;
        setMaskChar(reveal?(char)'\0':'*');
        if (revealPasswordEnabled) {
            setIcon(reveal?noEyeIcon:eyeIcon);
            System.out.printf("Set %s icon\n", reveal?"no eye 1":"eye 2");
        }
    }

    private void onClicked() {
        if (!isPassword || !revealPasswordEnabled) {
            return;
        }
        isPasswordRevealed = !isPasswordRevealed;
        if (isPasswordRevealed) {
            setMaskChar((char)'\0');
            setIcon(noEyeIcon);
            System.out.printf("Set no-eye 2 icon\n");
        } else {
            setMaskChar('*');
            setIcon(eyeIcon);
            System.out.printf("Set eye 3 icon\n");
        }
    }



    static class IconHelper {
        private static final int ICON_SPACING = 4;

        private Color borderColor;
        private Border border;
        private ImageIcon givenIcon;
        private Icon scaledIcon;
        private Border originalBorder;
        private final PlaceholderTextField textField;
        private int iconSpacing = ICON_SPACING;
        private boolean iconRight = false;

        private final int height;

        IconHelper(PlaceholderTextField component) {
            textField = component;
            originalBorder = component.getBorder();
            border = originalBorder;

            MouseAdapter mouseListener = new MouseAdapter() {
                private boolean in = false;
                private boolean hitTest(MouseEvent e) {
                    if (scaledIcon == null) return false;
                    int x = e.getX();
                    Insets iconInsets = originalBorder.getBorderInsets(textField);
                    int iconHitX0 = iconRight ? textField.getWidth()- scaledIcon.getIconWidth()-iconInsets.right : 0;
                    int iconHitX1 = iconRight ? textField.getWidth() : scaledIcon.getIconWidth() + iconInsets.left;
                    return (x >= iconHitX0 && x <= iconHitX1);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hitTest(e)) {
                        System.out.printf("Hit at (%d,%d)\n", e.getX(), e.getY());
                        onClicked();
                    } else {
                        super.mouseClicked(e);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    in = hitTest(e);
                    textField.setCursor(Cursor.getPredefinedCursor(in ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    boolean newIn = hitTest(e);
                    if (newIn != in) {
                        in = newIn;
                        textField.setCursor(Cursor.getPredefinedCursor(in ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
                    }
                }
            };
            component.addMouseListener(mouseListener);
            component.addMouseMotionListener(mouseListener);

            height = component.getHeight();
            ComponentListener resizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    int h = textField.getFontMetrics(textField.getFont()).getHeight();
                    System.out.printf("Resized to height %d\n", h);
                    resetBorder();
                }
            };
            component.addComponentListener(resizeListener);
        }

        /**
         * The click handler is called when the icon is clicked.
         */
        Runnable clickHandler = null;
        private void setClickHandler(Runnable clickHandler) {
            this.clickHandler = clickHandler;
        }
        void onClicked() {
            if (clickHandler != null) {
                clickHandler.run();
            }
        }

        Border getBorder() {
            return border;
        }

        void onPaintComponent(Graphics g) {
            if (scaledIcon != null) {
                Insets iconInsets = originalBorder.getBorderInsets(textField);
                int iconPaintOffsetX = iconRight ?
                                       textField.getWidth() - scaledIcon.getIconWidth() - iconInsets.right
                                           - iconSpacing :
                                       iconInsets.left + iconSpacing;
                scaledIcon.paintIcon(textField, g, iconPaintOffsetX, iconInsets.top);
            }
        }

        void onSetBorder(Border border) {
            originalBorder = border;

            if (givenIcon == null) {
                this.border = border;
            } else {
                int nw = givenIcon.getIconWidth();
                int nh = givenIcon.getIconHeight();
                int h = textField.getFontMetrics(textField.getFont()).getHeight();
                if (h == 0) h = textField.getPreferredSize().height;
                if(nh != h)
                {
                    nh = h;
                    nw = (givenIcon.getIconWidth() * nh) / givenIcon.getIconHeight();
                }

                scaledIcon = new ImageIcon(givenIcon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH));

                int w = scaledIcon.getIconWidth() + iconSpacing * 2;
                int l = iconRight ? 0 : w;
                int r = iconRight ? w : 0;
                Border margin = BorderFactory.createMatteBorder(0, l, 0, r, borderColor);
                this.border = BorderFactory.createCompoundBorder(border, margin);
            }
        }

        private void onSetGreyBorder() {
            borderColor = new Color(0, 0, 0, 0.07f);
        }

        void onSetIcon(ImageIcon icon) {
            this.scaledIcon = null;
            this.givenIcon = icon;
            resetBorder();
        }

        private void resetBorder() {
            textField.setBorder(originalBorder);
        }

        public void onSetIconSpacing(int spacing) {
            iconSpacing = spacing;
        }

        public void onSetRight(boolean right) {
            if (iconRight != right) {
                iconRight = right;
                resetBorder();
            }
        }
    }
}
