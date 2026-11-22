package com.javachat.user.ui.account;

import com.javachat.user.ui.chat.ChatTheme;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Consistent rounded action button for account and security controls. */
final class AccountButton extends JButton {
    enum Style { PRIMARY, SECONDARY, DANGER }

    private final Style style;

    AccountButton(String label, Style style) {
        super(label);
        this.style = style;
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(Math.max(110, metrics.stringWidth(getText()) + 34), 44);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background;
        Color foreground;
        Color outline;
        if (!isEnabled()) {
            background = new Color(239, 240, 236);
            foreground = new Color(151, 157, 153);
            outline = new Color(226, 229, 224);
        } else if (style == Style.PRIMARY) {
            background = getModel().isPressed() ? new Color(19, 101, 99)
                    : getModel().isRollover() ? ChatTheme.TEAL_DARK : ChatTheme.TEAL;
            foreground = Color.WHITE;
            outline = background;
        } else if (style == Style.DANGER) {
            background = getModel().isPressed() ? new Color(251, 227, 222)
                    : getModel().isRollover() ? new Color(255, 240, 235) : Color.WHITE;
            foreground = new Color(165, 55, 43);
            outline = new Color(222, 174, 164);
        } else {
            background = getModel().isPressed() ? new Color(219, 240, 235)
                    : getModel().isRollover() ? new Color(236, 249, 244) : Color.WHITE;
            foreground = ChatTheme.INK;
            outline = ChatTheme.BORDER;
        }
        RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, getWidth() - 2,
                getHeight() - 2, 14, 14);
        g.setColor(background);
        g.fill(shape);
        g.setColor(outline);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape);
        if (hasFocus()) {
            g.setColor(ChatTheme.TEAL_DARK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(3, 3, getWidth() - 6,
                    getHeight() - 6, 12, 12));
        }
        g.setFont(getFont());
        g.setColor(foreground);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g.dispose();
    }
}
