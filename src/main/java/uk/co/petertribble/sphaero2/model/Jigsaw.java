package uk.co.petertribble.sphaero2.model;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


public class Jigsaw {

  private AtomicInteger idProvider = new AtomicInteger();

  private final JigsawParam params;
  private final BufferedImage image;
  private Image finishedImage;
  // Last in list = topmost piece
  private PiecesBin pieces;
  private boolean finished;
  private List<PiecesBin> bins = new ArrayList<>();

  public Jigsaw(JigsawParam params, BufferedImage image) {
    this.image = image;
    this.params = params;
    this.pieces = new PiecesBin(idProvider, "main");
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

  public List<PiecesBin> getPiecesBins() {
    return bins;
  }

  public void addBin(String name) {
    bins.add(new PiecesBin(idProvider, name));
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

  // Copy pieces into zOrder, and randomize their positions.
  public void shuffle(int width, int height) {
    pieces.shuffle(width, height);

    finished = false;
    if (finishedImage != null) {
      finishedImage.flush();
      finishedImage = null;
    }
  }

  public void reset() {
    Piece[] pieces = getParams().getCutter().cut(image);
    this.pieces.setPieces(Arrays.asList(pieces));
    shuffle(image.getWidth(), image.getHeight());
  }

  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    pieces.push();
  }
}
