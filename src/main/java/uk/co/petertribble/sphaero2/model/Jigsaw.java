package uk.co.petertribble.sphaero2.model;

import uk.co.petertribble.sphaero2.cutter.JigsawCutter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;


public class Jigsaw {
    private final BufferedImage image;
    private Image finishedImage;
    // Last in list = topmost piece
    private PiecesBin pieces = new PiecesBin();
    private boolean finished;

    public Jigsaw(BufferedImage image) {
        this.image = image;
    }

    public PiecesBin getPieces() {
        return pieces;
    }

    public Image getFinishedImage() {
        return finishedImage;
    }

    public void setFinishedImage(Image finishedImage) {
        this.finishedImage = finishedImage;
    }

    public boolean calculateFinished() {
        return pieces.getPieces().size() == 1;
    }

    public void setFinished() {
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }


    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    // Copy pieces into zOrder, and randomize their positions.
    public void shuffle(int width, int height) {
        pieces.shuffle(width, height);

        finished = false;
        if (finishedImage != null) {
            finishedImage.flush();
            finishedImage = null;
        }
    }

    public void reset(JigsawCutter cutter) {
        Piece[] pieces = cutter.cut(image);
        this.pieces.setPieces(Arrays.asList(pieces));
        shuffle(getWidth(), getHeight());
    }

    /**
     * Push the top piece (at the front) to the bottom (the back).
     */
    public void push() {
        pieces.push();
    }
}
