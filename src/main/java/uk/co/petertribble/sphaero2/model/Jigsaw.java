package uk.co.petertribble.sphaero2.model;

import com.berray.math.Rect;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;


public class Jigsaw {

  private final JigsawParam params;
  private final BufferedImage image;
  private Image finishedImage;
  // Last in list = topmost piece
  private PiecesBin pieces;
  private boolean finished;

  public Jigsaw(JigsawParam params, BufferedImage image, PiecesBin pieces) {
    this.image = image;
    this.params = params;
    this.pieces = pieces;
  }

  public JigsawParam getParams() {
    return params;
  }

  /**
   * Returns the number of pieces which are already used in multipieces. these are considered "solved".
   */
  public int getPiecesInMultipieces() {
    return (int) pieces.getPieces().stream()
        .filter(piece -> piece instanceof MultiPiece)
        .map(Piece::getSubs)
        .mapToLong(Set::size)
        .sum();
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
    return pieces.getWidth();
  }

  public int getHeight() {
    return pieces.getHeight();
  }

  public void shuffle(int width, int height) {
    pieces.shuffle(new Rect(0, 0, width, height), true);

    finished = false;
    if (finishedImage != null) {
      finishedImage.flush();
      finishedImage = null;
    }
  }

  public void reset() {
    reset(false, image.getWidth(), image.getHeight());
  }

  public void reset(boolean shuffle, int width, int height) {
    Piece[] pieces = getParams().getCutter().cut(image);
    this.pieces.setPieces(Arrays.asList(pieces));
    if (shuffle) {
      shuffle(width, height);
    }
  }
}
