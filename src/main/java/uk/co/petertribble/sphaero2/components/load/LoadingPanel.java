package uk.co.petertribble.sphaero2.components.load;

import uk.co.petertribble.sphaero2.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.util.Deque;

public class LoadingPanel extends JPanel {

    private JProgressBar progressBar;
    private Deque<Piece> pieces;

    public LoadingPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);
        setAlignmentX(JComponent.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(createProgressBarPanel());
        add(Box.createVerticalGlue());
    }

    private JProgressBar createProgressBarPanel() {
        this.progressBar = new JProgressBar(0, 100);
        progressBar.setValue(50);
        progressBar.setMaximumSize(new Dimension(500, -1));
        progressBar.setBorder(BorderFactory.createTitledBorder("Loading"));
        progressBar.setStringPainted(true);
        progressBar.setString("Loading pieces");

        return progressBar;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Piece piece : pieces) {
            piece.draw(g);
        }

    }


    public void setPieces(Deque<Piece> pieces) {
        this.pieces = pieces;
    }
}
