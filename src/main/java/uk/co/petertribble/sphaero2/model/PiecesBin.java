package uk.co.petertribble.sphaero2.model;

import com.berray.math.Rect;
import com.berray.math.Vec2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class PiecesBin {
  /**
   * Provider which supplies unique ids
   */
  private AtomicInteger idProvider;

  /**
   * Name of this bin
   */
  private final String name;
  /**
   * List of pieces in this bin
   */
  private List<Piece> pieces;
  /**
   * Selected pieces, if any
   */
  private Set<Piece> selected = new LinkedHashSet<>();

  public PiecesBin(PiecesBin piecesBin) {
    this(piecesBin.idProvider, piecesBin.name, piecesBin.pieces);
  }

  public PiecesBin(AtomicInteger idProvider, String name) {
    this(idProvider, name, new ArrayList<>());
  }

  public PiecesBin(AtomicInteger idProvider, String name, List<Piece> pieces) {
    this.pieces = Objects.requireNonNull(pieces);
    this.name = name;
    this.idProvider = idProvider;
  }

  public List<Piece> getPieces() {
    return pieces;
  }

  public Set<Piece> getSelected() {
    return selected;
  }

  /**
   * Returns the anchor for the selected set. The anchor can be an arbitrary position. It is guaranteed to be stable (the same for 2 calls)
   * and when the whole set is @{link {@link #moveSelected(Vec2)} moved}, the anchor is moved by the same amount.
   */
  public Vec2 getSelectedAnchor() {
    Piece anchorPiece = selected.iterator().next();
    return new Vec2(anchorPiece.getPuzzleX(), anchorPiece.getPuzzleY());
  }

  public void moveSelected(Vec2 delta) {
    for (Piece piece : selected) {
      int x = (int) (piece.getPuzzleX() + delta.getX());
      int y = (int) (piece.getPuzzleY() + delta.getY());
      piece.moveTo(x, y);
    }
  }


  public boolean isSelected(Piece piece) {
    return selected.contains(piece);
  }

  public void addSelection(Piece piece) {
    selected.add(piece);
  }

  public void clearSelection() {
    selected.clear();
  }

  public void removeSelection(Piece piece) {
    selected.remove(piece);
  }


  public String getName() {
    return name;
  }

  public Supplier<Integer> getIdProvider() {
    return idProvider::getAndIncrement;
  }

  public void setPieces(List<Piece> pieces) {
    this.pieces = pieces;
    // find last piece id and set idProvider to the next id
    int maxId = pieces.stream().mapToInt(Piece::getId).max().orElse(0);
    idProvider.set(maxId);
  }

  public void clear(Rect destination, Rect rectangleToKeepFree) {
    List<Piece> piecesToShuffle = new ArrayList<>();
    List<Piece> remainingPieces = new ArrayList<>();
    Random random = new Random();
    for (Piece piece : this.pieces) {
      if (destination.contains(piece.getPuzzleX(), piece.getPuzzleY())) {
        piece.setPuzzlePosition(
            (int) (destination.getX() + random.nextInt((int) (destination.getWidth() - piece.getCurrentWidth()))),
            (int) (destination.getY() + random.nextInt((int) (destination.getHeight() - piece.getCurrentHeight()))));
        piecesToShuffle.add(piece);
      } else {
        remainingPieces.add(piece);
      }
    }
    Collections.shuffle(piecesToShuffle);
    remainingPieces.addAll(piecesToShuffle);
    this.pieces = remainingPieces;
  }

  public void shuffle(Rect destination) {
    List<Piece> piecesToShuffle = new ArrayList<>();
    List<Piece> remainingPieces = new ArrayList<>();
    Random random = new Random();
    for (Piece piece : this.pieces) {
      if (destination.contains(piece.getPuzzleX(), piece.getPuzzleY())) {
        int x = (int) (destination.getX() + random.nextFloat() * (destination.getWidth() - piece.getCurrentWidth()));
        int y = (int) (destination.getY() + random.nextFloat() * (destination.getHeight() - piece.getCurrentHeight()));
        piece.setPuzzlePosition(x, y);
        piecesToShuffle.add(piece);
      } else {
        remainingPieces.add(piece);
      }
    }
    Collections.shuffle(piecesToShuffle);
    remainingPieces.addAll(piecesToShuffle);
    this.pieces = remainingPieces;
  }

  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    Piece p = pieces.remove(pieces.size() - 1);
    pieces.add(0, p);
  }

  public void movePieceTo(Piece piece, int x, int y) {
    piece.setPuzzlePosition(x, y);
  }

  public Piece join(Piece movedPiece) {
    Piece[] result = movedPiece.join(getIdProvider());
    if (result != null) {
      Piece newPiece = result[0];
      for (int i = 1; i < result.length; i++) {
        pieces.remove(result[i]);
      }
      pieces.add(newPiece);
      return newPiece;
    }
    return null;
  }

  public int getWidth() {
    return pieces.stream().mapToInt(p -> p.getPuzzleX() + p.getCurrentWidth()).max().orElse(100);
  }

  public int getHeight() {
    return pieces.stream().mapToInt(p -> p.getPuzzleY() + p.getCurrentHeight()).max().orElse(100);
  }

  public Rect getRect() {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (Piece piece : pieces) {
      minX = Math.min(minX, piece.getPuzzleX());
      minY = Math.min(minY, piece.getPuzzleY());
      maxX = Math.max(maxX, piece.getPuzzleX() + piece.getCurrentWidth());
      maxY = Math.max(maxY, piece.getPuzzleY() + piece.getCurrentHeight());
    }

    return new Rect(minX, minY, maxX - minX, maxY - minY);
  }

  public Piece getPieceAt(int x, int y) {
    ListIterator<Piece> iter = pieces.listIterator(pieces.size());
    while (iter.hasPrevious()) {
      Piece piece = iter.previous();
      if (piece.contains(x, y)) {
        return piece;
      }
    }
    return null;
  }

  public void moveToTop(Piece piece) {
    boolean containedPiece = pieces.remove(piece);
    if (containedPiece) {
      pieces.add(piece);
    }
  }
}
