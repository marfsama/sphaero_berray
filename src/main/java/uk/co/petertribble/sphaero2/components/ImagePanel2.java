package uk.co.petertribble.sphaero2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel2 extends JPanel {
    private BufferedImage image;
    private Dimension maxSize;

    public ImagePanel2() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            // Calculate scaled dimensions maintaining aspect ratio
            Dimension scaled = getScaledDimension(
                    new Dimension(image.getWidth(), image.getHeight()),
                    getSize());

            // Draw the image centered
            int x = (getWidth() - scaled.width) / 2;
            int y = (getHeight() - scaled.height) / 2;
            g.drawImage(image, x, y, scaled.width, scaled.height, null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(0, 0);
        }

        // Get parent size if available
        Container parent = getParent();
        if (parent != null) {
            maxSize = parent.getSize();
        }

        if (maxSize == null) {
            // Default to image size if no parent info
            return new Dimension(image.getWidth(), image.getHeight());
        }

        // Calculate maximum possible size maintaining aspect ratio
        return getScaledDimension(
                new Dimension(image.getWidth(), image.getHeight()),
                maxSize);
    }

    private Dimension getScaledDimension(Dimension imageSize, Dimension boundary) {
        double widthRatio = boundary.getWidth() / imageSize.getWidth();
        double heightRatio = boundary.getHeight() / imageSize.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        return new Dimension(
                (int) (imageSize.width * ratio),
                (int) (imageSize.height * ratio));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        revalidate();
        repaint();
    }
}
