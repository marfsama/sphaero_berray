package uk.co.petertribble.sphaero2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Jigsaw {
  private final BufferedImage image;
  Image finishedImage;
  // Last in list = topmost piece
  List<Piece> zOrder;
  private boolean finished;

  public void setFinished() {
    this.finished = true;
  }

  public boolean isFinished() {
    return finished;
  }

  public Jigsaw(BufferedImage image) {
    this.image = image;
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
    // Arrays.asList() doesn't work, so be explicit
    List<Piece> pieces = zOrder;
    zOrder = new ArrayList<Piece>();
    for (Piece piece : pieces) {
      piece.setPuzzlePosition(
          (int) (Math.random() * (width - piece.getCurrentWidth())),
          (int) (Math.random() * (height - piece.getCurrentHeight())));
      zOrder.add(piece);
    }
    Collections.shuffle(zOrder);

    finished = false;
    if (finishedImage != null) {
      finishedImage.flush();
      finishedImage = null;
    }
  }

  public void reset(JigsawCutter cutter) {
    Piece[] pieces = cutter.cut(image);
    zOrder = new ArrayList<>(Arrays.asList(pieces));
    shuffle(getWidth(), getHeight());
  }

  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    Piece p = zOrder.remove(zOrder.size() - 1);
    zOrder.add(0, p);
  }
}
