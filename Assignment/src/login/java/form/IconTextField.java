package login.java.form;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class IconTextField extends JTextField {

    private Icon icon;
    private String placeholder;

    public IconTextField(Icon icon, String placeholder) {
        this.icon = icon;
        this.placeholder = placeholder;

        setPreferredSize(new Dimension(250, 40));
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
       
        // Leave space for the icon (icon width + padding)
        int leftMargin = (icon != null ? icon.getIconWidth() + 20 : 35);
        setMargin(new Insets(5, leftMargin, 5, 10));
        
        setOpaque(false);
        setBorder(null);
    }


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow (soft floating effect)
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fill(new RoundRectangle2D.Float(3, 3, getWidth(), getHeight(), 20, 20));

        // Background
        Shape roundRect = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20);
        g2.setColor(Color.WHITE);
        g2.fill(roundRect);

        // Border coffee brown
        g2.setColor(new Color(245, 237, 224)); // #663300
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(roundRect);

        super.paintComponent(g2);

        // Icon
        if (icon != null) {
            icon.paintIcon(this, g2, 170, (getHeight() - icon.getIconHeight()) / 2);
        }

        // Placeholder
        if (getText().isEmpty() && !isFocusOwner()) {
            g2.setColor(new Color(150,150,150));
            g2.setFont(getFont());
            g2.drawString(placeholder, 38, getHeight() / 2 + 5);
        }

        g2.dispose();
    }
}
