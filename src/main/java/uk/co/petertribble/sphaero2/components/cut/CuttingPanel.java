package uk.co.petertribble.sphaero2.components.cut;

import javax.swing.*;
import java.awt.*;

public class CuttingPanel extends JPanel {

    public CuttingPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);
        setAlignmentX(JComponent.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(getProgressBarPanel());
        add(Box.createVerticalGlue());
    }

    private static JProgressBar getProgressBarPanel() {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(50);
        progressBar.setMaximumSize(new Dimension(500, -1));
        progressBar.setBorder(BorderFactory.createTitledBorder("Cutting in progress"));
        progressBar.setStringPainted(true);
        progressBar.setString("Cutting in progress");

        return progressBar;
    }
}
