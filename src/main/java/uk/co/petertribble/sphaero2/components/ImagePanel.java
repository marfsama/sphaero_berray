package uk.co.petertribble.sphaero2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private boolean scale = false;

    public ImagePanel() {
    }

    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null && !scale) {
            Insets insets = getInsets();
            return new Dimension(image.getWidth() + insets.left + insets.right, image.getHeight() + insets.top + insets.bottom);
        }

        return super.getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return super.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return super.getPreferredSize();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isScale() {
        return scale;
    }

    public void setScale(boolean scale) {
        this.scale = scale;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }

        Insets insets = getInsets();
        int innerWidth = getWidth() - insets.left - insets.right;
        int innerHeight = getHeight() - insets.top - insets.bottom;


        if (!scale) {
            g.drawImage(image, 0, 0, null);
        } else {
            float imageAspectRatio = 1.0f * image.getWidth() / image.getHeight();
            int width = Math.max(image.getWidth(), innerWidth);
            int height = Math.max(image.getHeight(), innerHeight);

            if (width > innerWidth) {
                float factor = innerWidth / (float) width;
                width = innerWidth;
                height = (int) (height * factor);
            }

            if (height > innerHeight) {
                float factor = innerHeight / (float) height;
                height = innerHeight;
                width = (int) (width * factor);
            }

            // center image
            int x = (innerWidth - width) / 2;
            int y= (innerHeight - height) / 2;

            g.drawImage(image, x, y, width, height, null);
        }
    }
}
