package uk.co.petertribble.sphaero2.components.select;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Panel to show the image properties.
 */
public class ImagePropertiesPanel extends JPanel {
    private JLabel imagePreview;
    private JLabel dimensionsLabel;
    private JLabel sizeLabel;
    private JLabel typeLabel;
    private JLabel selectionLabel;

    // New components
    private JComboBox<JigsawCutter> cutterComboBox;
    private JSpinner pieceCountSpinner;
    private JCheckBox scaleToFitCheckBox;

    // image scaling and centering in preview label
    private double scaleFactor = 1.0;
    private Point previewOffset;

    // Selection rectangle tracking
    private Point startPoint;
    private Rectangle selectionRect;
    private Point dragOffset; // For moving existing selection
    private BufferedImage currentImage;
    private Point scaledSize;
    private boolean isMovingSelection = false;

    // Constants for thumbnail sizing
    private static final int MAX_THUMB_WIDTH = 180;
    private static final int MAX_THUMB_HEIGHT = 120;
    private static final int PREFERRED_WIDTH = 200;
    private static final int PREFERRED_HEIGHT = 200;

    public ImagePropertiesPanel() {
        initComponents();
        setupMouseListeners();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Main content panel with image and properties side by side
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));

        // Image preview area (left side)
        imagePreview = new JLabel("No image selected", JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (selectionRect != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(0, 100, 255, 150));
                    g2d.fill(selectionRect);
                    g2d.setColor(Color.BLUE);
                    g2d.draw(selectionRect);
                    g2d.dispose();
                }
            }
        };
        imagePreview.setHorizontalAlignment(JLabel.CENTER);
        imagePreview.setPreferredSize(new Dimension(MAX_THUMB_WIDTH, MAX_THUMB_HEIGHT));
        imagePreview.setBorder(BorderFactory.createEtchedBorder());

        // Properties panel (right side)
        JPanel propertiesPanel = createPropertiesPanel();


        contentPanel.add(imagePreview, BorderLayout.WEST);
        contentPanel.add(propertiesPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createPropertiesPanel() {
        // Create FormLayout with 4 columns and appropriate gaps
        FormLayout layout = new FormLayout(
                "right:pref, 5dlu, pref:grow, 10dlu, right:pref, 5dlu, pref:grow", // columns
                "p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p"); // rows

        JPanel propertiesPanel = new JPanel(layout);
        propertiesPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        CellConstraints cc = new CellConstraints();

        // Row 1: Dimensions and Cutter
        propertiesPanel.add(new JLabel("Dimensions:"), cc.xy(1, 1));
        propertiesPanel.add(dimensionsLabel = new JLabel("-"), cc.xy(3, 1));

        propertiesPanel.add(new JLabel("Cutter:"), cc.xy(5, 1));
        cutterComboBox = new JComboBox<>(JigsawCutter.cutters);
        propertiesPanel.add(cutterComboBox, cc.xy(7, 1));

        // Row 2: File Size and Pieces
        propertiesPanel.add(new JLabel("File Size:"), cc.xy(1, 3));
        propertiesPanel.add(sizeLabel = new JLabel("-"), cc.xy(3, 3));

        propertiesPanel.add(new JLabel("Number of Pieces:"), cc.xy(5, 3));
        pieceCountSpinner = new JSpinner(new SpinnerNumberModel(100, 4, 1000, 1));
        propertiesPanel.add(pieceCountSpinner, cc.xy(7, 3));

        // Row 3: Type and Scaling
        propertiesPanel.add(new JLabel("Type:"), cc.xy(1, 5));
        propertiesPanel.add(typeLabel = new JLabel("-"), cc.xy(3, 5));

        propertiesPanel.add(new JLabel("Scaling:"), cc.xy(5, 5));
        scaleToFitCheckBox = new JCheckBox("Scale to fit");
        propertiesPanel.add(scaleToFitCheckBox, cc.xy(7, 5));

        // Row 4: Selection
        propertiesPanel.add(new JLabel("Selection:"), cc.xy(1, 7));
        propertiesPanel.add(selectionLabel = new JLabel("None"), cc.xyw(3, 7, 5));

        return propertiesPanel;
    }

    private void setupMouseListeners() {
        imagePreview.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentImage == null) {
                    return;
                }
                Point clickPoint = e.getPoint();

                // Check if we're clicking inside existing selection
                if (selectionRect != null && selectionRect.contains(clickPoint)) {
                    isMovingSelection = true;
                    dragOffset = new Point(
                            clickPoint.x - selectionRect.x,
                            clickPoint.y - selectionRect.y
                    );
                } else {
                    // Start new selection
                    isMovingSelection = false;
                    startPoint = clickPoint;
                    selectionRect = new Rectangle(startPoint);
                }
                updateSelectionLabel();
                imagePreview.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionRect != null) {
                    if (!isMovingSelection) {
                        int clampX = Math.max(0, previewOffset.x - selectionRect.x);
                        int clampY = Math.max(0, previewOffset.y - selectionRect.y);

                        selectionRect.x += clampX;
                        selectionRect.y += clampY;
                        selectionRect.width -= clampX;
                        selectionRect.height -= clampY;

                        int remainingWidth = previewOffset.x + scaledSize.x - selectionRect.x;
                        int remainingHeight = previewOffset.y + scaledSize.y - selectionRect.y;

                        System.out.print("previewOffset: "+previewOffset.x+"x"+previewOffset.y);
                        System.out.print(" scaledSize: "+scaledSize.x+"x"+scaledSize.y);
                        System.out.print(" selection: "+selectionRect.x+"x"+selectionRect.y+" - "+selectionRect.width+"x"+selectionRect.height);
                        System.out.print(" remaining: "+remainingWidth+"x"+remainingHeight);
                        System.out.println();

                        selectionRect.width = Math.min(selectionRect.width, remainingWidth);
                        selectionRect.height = Math.min(selectionRect.height, remainingHeight);

                    }

                    if (selectionRect.width < 5 || selectionRect.height < 5) {
                        selectionRect = null;
                    }

                    updateSelectionLabel();
                }
                isMovingSelection = false;
                imagePreview.repaint();
            }
        });

        imagePreview.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentImage == null) {
                    return;
                }

                Point currentPoint = e.getPoint();

                if (isMovingSelection) {
                    // Move existing selection
                    int newX = currentPoint.x - dragOffset.x;
                    int newY = currentPoint.y - dragOffset.y;

                    // Constrain to image bounds
                    newX = Math.max(previewOffset.x, Math.min(newX, imagePreview.getWidth() - selectionRect.width - previewOffset.x));
                    newY = Math.max(previewOffset.y, Math.min(newY, imagePreview.getHeight() - selectionRect.height - previewOffset.y));

                    selectionRect.setLocation(newX, newY);
                } else if (startPoint != null) {
                    // Resize selection
                    selectionRect.setBounds(
                            Math.min(startPoint.x, currentPoint.x),
                            Math.min(startPoint.y, currentPoint.y),
                            Math.abs(currentPoint.x - startPoint.x),
                            Math.abs(currentPoint.y - startPoint.y)
                    );
                }

                updateSelectionLabel();
                imagePreview.repaint();
            }
        });
        imagePreview.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (scaledSize != null) {
                    previewOffset = new Point((imagePreview.getWidth() - scaledSize.x) / 2, (imagePreview.getHeight() - scaledSize.y) / 2);
                }
            }
        });
    }

    private void updateSelectionLabel() {
        if (selectionRect != null && currentImage != null) {
            // Convert from display coordinates to image coordinates
            int x1 = (int) ((selectionRect.x - previewOffset.x) / scaleFactor);
            int y1 = (int) ((selectionRect.y - previewOffset.y) / scaleFactor);
            int x2 = (int) ((selectionRect.x  - previewOffset.x + selectionRect.width) / scaleFactor);
            int y2 = (int) ((selectionRect.y  - previewOffset.y + selectionRect.height) / scaleFactor);

            // Ensure coordinates are within image bounds
            x1 = Math.max(0, Math.min(x1, currentImage.getWidth()));
            y1 = Math.max(0, Math.min(y1, currentImage.getHeight()));
            x2 = Math.max(0, Math.min(x2, currentImage.getWidth()));
            y2 = Math.max(0, Math.min(y2, currentImage.getHeight()));

            selectionLabel.setText(String.format("Selection: %d x %d - %d x %d (size: %d x %d)", x1, y1, x2, y2, x2 - x1, y2 - y1));
        } else {
            selectionLabel.setText("Selection: None");
        }
    }

    public Rectangle getImageSelection() {
        if (selectionRect == null || currentImage == null) {
            return null;
        }

        // Convert from display coordinates to image coordinates
        int x1 = (int) ((selectionRect.x - previewOffset.x) / scaleFactor);
        int y1 = (int) ((selectionRect.y - previewOffset.y) / scaleFactor);
        int x2 = (int) ((selectionRect.x  - previewOffset.x + selectionRect.width) / scaleFactor);
        int y2 = (int) ((selectionRect.y  - previewOffset.y + selectionRect.height) / scaleFactor);

        // Ensure coordinates are within image bounds
        x1 = Math.max(0, Math.min(x1, currentImage.getWidth()));
        y1 = Math.max(0, Math.min(y1, currentImage.getHeight()));
        x2 = Math.max(0, Math.min(x2, currentImage.getWidth()));
        y2 = Math.max(0, Math.min(y2, currentImage.getHeight()));
        return new Rectangle(x1, y1, x2-x1, y2-y1);
    }

    // Getters for the new controls
    public String getSelectedCutter() {
        return (String)cutterComboBox.getSelectedItem();
    }

    public int getPieceCount() {
        return (Integer)pieceCountSpinner.getValue();
    }

    public boolean shouldScaleToFit() {
        return scaleToFitCheckBox.isSelected();
    }

    /**
     * Sets the image to display in the preview panel
     *
     * @param image The BufferedImage to display (can be null to clear)
     */
    public void setImage(BufferedImage image) {
        this.currentImage = image;
        if (image == null) {
            clearPreview();
            return;
        }

        updateImageInfo(image);
        displayThumbnail(image);
        resetSelection();
    }

    /**
     * Sets the image to display by loading from a file
     *
     * @param file The image file to load (can be null to clear)
     */
    public void setImage(File file) {
        if (file == null || !file.isFile()) {
            clearPreview();
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new Exception("Unsupported image format");
            }

            setImage(image);


            // Update file-specific information
            sizeLabel.setText("File size: " + formatFileSize(file.length()));
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1).toUpperCase() : "UNKNOWN";
            typeLabel.setText("Type: " + extension);

        } catch (Exception e) {
            showError("Invalid image");
        }
    }

    private void updateImageInfo(BufferedImage image) {
        // Update dimensions
        dimensionsLabel.setText(String.format("Dimensions: %d × %d",
                image.getWidth(), image.getHeight()));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void displayThumbnail(BufferedImage image) {
        // Calculate thumbnail size preserving aspect ratio
        double widthRatio = MAX_THUMB_WIDTH / (double) image.getWidth();
        double heightRatio = MAX_THUMB_HEIGHT / (double) image.getHeight();
        scaleFactor = Math.min(widthRatio, heightRatio);

        int thumbWidth = (int) (image.getWidth() * scaleFactor);
        int thumbHeight = (int) (image.getHeight() * scaleFactor);

        scaledSize = new Point(thumbWidth, thumbHeight);
        Image scaledImage = image.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);
        imagePreview.setIcon(new ImageIcon(scaledImage));
        imagePreview.setText("");

        previewOffset = new Point((imagePreview.getWidth() - scaledSize.x) / 2, (imagePreview.getHeight() - scaledSize.y) / 2);
    }


    private void resetSelection() {
        selectionRect = null;
        updateSelectionLabel();
        imagePreview.repaint();
    }

    private void clearPreview() {
        imagePreview.setIcon(null);
        imagePreview.setText("No image selected");
        dimensionsLabel.setText("Dimensions: -");
        sizeLabel.setText("File size: -");
        typeLabel.setText("Type: -");
        resetSelection();
    }

    private void showError(String message) {
        imagePreview.setIcon(null);
        imagePreview.setText(message);
        dimensionsLabel.setText("Dimensions: -");
        sizeLabel.setText("File size: -");
        typeLabel.setText("Type: -");
        resetSelection();
    }

}
