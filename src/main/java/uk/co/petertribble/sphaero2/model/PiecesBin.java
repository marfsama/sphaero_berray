package uk.co.petertribble.sphaero2.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PiecesBin {
  private List<Piece> pieces;

  public List<Piece> getPieces() {
    return pieces;
  }
  public void shuffle(int width, int height) {
    // Arrays.asList() doesn't work, so be explicit
    List<Piece> pieces = this.pieces;
    this.pieces = new ArrayList<>();
    Random random = new Random();
    for (Piece piece : pieces) {
      piece.setPuzzlePosition(
          random.nextInt(width - piece.getCurrentWidth()),
          random.nextInt(height - piece.getCurrentHeight()));
      this.pieces.add(piece);
    }
    Collections.shuffle(this.pieces);
  }

  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    Piece p = pieces.remove(pieces.size() - 1);
    pieces.add(0, p);
  }

  public int getWidth() {
    return pieces.stream().mapToInt(p -> p.getRotatedX()+p.getCurrentWidth()).max().orElse(100);
  }

  public int getHeight() {
    return pieces.stream().mapToInt(p -> p.getRotatedY()+p.getCurrentHeight()).max().orElse(100);
  }
}
