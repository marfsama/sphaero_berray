package uk.co.petertribble.sphaero2.model;

import com.berray.math.Vec2;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set of orderes pieces. This class enables some actions like move to be applied to the whole set of pieces.
 */
public class PieceSet implements Iterable<Piece> {

  private Set<Piece> pieces = new LinkedHashSet<>();

  @Override
  public Iterator<Piece> iterator() {
    return pieces.iterator();
  }

  public boolean contains(Piece piece) {
    return pieces.contains(piece);
  }


  public void add(Piece piece) {
    pieces.add(piece);
  }

  public void clear() {
    pieces.clear();
  }

  public void remove(Piece piece) {
    pieces.remove(piece);
  }

  public boolean isEmpty() {
    return pieces.isEmpty();
  }

  public void moveBy(Vec2 delta) {
    for (Piece piece : pieces) {
      int x = (int) (piece.getPuzzleX() + delta.getX());
      int y = (int) (piece.getPuzzleY() + delta.getY());
      piece.moveTo(x, y);
    }
  }

  /**
   * Returns the anchor for the selected set. The anchor can be an arbitrary position. It is guaranteed to be stable (the same for 2 calls)
   * and when the whole set is @{link {@link #moveBy(Vec2)} (Vec2)} moved}, the anchor is moved by the same amount.
   */
  public Vec2 getAnchor() {
    Piece anchorPiece = pieces.iterator().next();
    return new Vec2(anchorPiece.getPuzzleX(), anchorPiece.getPuzzleY());

  }

  public Vec2 getCenter() {
    float x = 0;
    float y = 0;
    for (Piece piece : pieces) {
      x += piece.getPuzzleX();
      y += piece.getPuzzleY();
    }
    return new Vec2(x / pieces.size(), y / pieces.size());
  }

  public void stack(Vec2 center) {
    for (Piece piece : pieces) {
      piece.moveTo((int) center.getX(), (int) center.getY());
    }
  }

  public void addAll(Iterable<Piece> others) {
    others.forEach(pieces::add);
  }
}
