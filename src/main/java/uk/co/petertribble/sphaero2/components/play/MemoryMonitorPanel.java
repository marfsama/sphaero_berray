package uk.co.petertribble.sphaero2.components.play;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;

public class MemoryMonitorPanel extends JPanel {
    private final JProgressBar memoryBar;
    private final JLabel memoryLabel;
    private final Timer updateTimer;
    private final DecimalFormat format = new DecimalFormat("#,##0.0");
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    public MemoryMonitorPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Create components
        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(false);
        memoryBar.setPreferredSize(new Dimension(80, 16));

        memoryLabel = new JLabel();
        memoryLabel.setFont(memoryLabel.getFont().deriveFont(10f));
        memoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Add components
        add(memoryBar);
        add(memoryLabel);

        // Create update timer
        updateTimer = new Timer(1000, e -> updateMemoryUsage());
        updateTimer.start();

        // Initial update
        updateMemoryUsage();
    }

    private void updateMemoryUsage() {
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();

        double percentUsed = (double) used / max * 100;
        memoryBar.setValue((int) percentUsed);

        // Update color based on usage
        if (percentUsed > 90) {
            memoryBar.setForeground(new Color(200, 0, 0)); // Red
        } else if (percentUsed > 70) {
            memoryBar.setForeground(new Color(255, 165, 0)); // Orange
        } else {
            memoryBar.setForeground(new Color(0, 180, 0)); // Green
        }

        // Update label text
        String usedMB = format.format(used / (1024 * 1024));
        String maxMB = format.format(max / (1024 * 1024));
        memoryLabel.setText(usedMB + " / " + maxMB + " MB");
    }

    public void dispose() {
        updateTimer.stop();
    }

}