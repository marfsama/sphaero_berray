package uk.co.petertribble.sphaero2.model;

import com.berray.math.Vec2;

import java.awt.*;
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

  public void moveBy(int deltaX, int deltaY) {
    moveBy(deltaX, deltaY, false);
  }

  public void moveBy(int deltaX, int deltaY, boolean setCurrentPosition) {
    for (Piece piece : pieces) {
      int x = piece.getPuzzleX() + deltaX;
      int y = piece.getPuzzleY() + deltaY;
      piece.moveTo(x, y);
      if (setCurrentPosition) {
        piece.setCurrentPosition(x, y);
      }
    }
  }


  /**
   * Returns the anchor for the selected set. The anchor can be an arbitrary position. It is guaranteed to be stable (the same for 2 calls)
   * and when the whole set is @{link {@link #moveBy(Vec2)} (Vec2)} moved}, the anchor is moved by the same amount.
   */
  public Vec2 getAnchor() {
    if (pieces.isEmpty()) {
      return Vec2.origin();
    }
    Piece anchorPiece = pieces.iterator().next();
    return new Vec2(anchorPiece.getPuzzleX(), anchorPiece.getPuzzleY());
  }

  /**
   * Returns the anchor for the selected set. The anchor can be an arbitrary position. It is guaranteed to be stable (the same for 2 calls)
   * and when the whole set is @{link {@link #moveBy(Vec2)} (Vec2)} moved}, the anchor is moved by the same amount.
   */
  public Point getAnchorPoint() {
    if (pieces.isEmpty()) {
      return new Point();
    }
    Piece anchorPiece = pieces.iterator().next();
    return new Point(anchorPiece.getPuzzleX(), anchorPiece.getPuzzleY());
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

  public void stack(Point center) {
    for (Piece piece : pieces) {
      piece.moveTo(center.x - piece.getCurrentWidth() / 2, center.y - piece.getCurrentHeight() / 2);
    }
  }


  public void addAll(Iterable<Piece> others) {
    others.forEach(pieces::add);
  }
}
