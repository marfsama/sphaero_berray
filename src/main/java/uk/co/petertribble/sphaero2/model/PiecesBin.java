package uk.co.petertribble.sphaero2.model;

import com.berray.math.Rect;

import java.awt.*;
import java.util.*;
import java.util.List;
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
  private PieceSet selected = new PieceSet();

  public PiecesBin(PiecesBin piecesBin) {
    this(piecesBin.idProvider, piecesBin.name, piecesBin.pieces);
  }

  public PiecesBin(AtomicInteger idProvider, String name) {
    this(idProvider, name, new ArrayList<>());
  }

  public PiecesBin(AtomicInteger idProvider, String name, List<Piece> pieces) {
    this.name = name;
    this.idProvider = idProvider;
    this.setPieces(pieces);
  }

  public List<Piece> getPieces() {
    return pieces;
  }

  public PieceSet getSelected() {
    return selected;
  }

  public PieceSet getPiecesInRect(Rect localRect) {
    PieceSet piecesInRect = new PieceSet();

    for (Piece piece : pieces) {
      if (localRect.contains(piece.getPuzzleX(), piece.getPuzzleY()) ||
          localRect.contains(piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY()) ||
          localRect.contains(piece.getPuzzleX(), piece.getPuzzleY() + piece.getCurrentHeight()) ||
          localRect.contains(piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY() + piece.getCurrentHeight())
      ) {
        piecesInRect.add(piece);
      }
    }
    return piecesInRect;
  }

  public PieceSet getPiecesInRect(Rectangle localRect) {
    PieceSet piecesInRect = new PieceSet();

    for (Piece piece : pieces) {
      if (localRect.contains(piece.getPuzzleX(), piece.getPuzzleY()) &&
              localRect.contains(piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY()) &&
              localRect.contains(piece.getPuzzleX(), piece.getPuzzleY() + piece.getCurrentHeight()) &&
              localRect.contains(piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY() + piece.getCurrentHeight())
      ) {
        piecesInRect.add(piece);
      }
    }
    return piecesInRect;
  }



  public String getName() {
    return name;
  }

  public Supplier<Integer> getIdProvider() {
    return idProvider::getAndIncrement;
  }

  public void setPieces(List<Piece> pieces) {
    // create a copy of the pieces list so we can be sure that the list is modifiable
    this.pieces = new ArrayList<>(Objects.requireNonNull(pieces));
    // find last piece id and set idProvider to the next id
    int maxId = pieces.stream().mapToInt(Piece::getId).max().orElse(0);
    idProvider.set(maxId);
  }

  /** moves all pieces out of the specified rectangle. */
  public void clear(Rectangle rectangleToKeepFree) {
    for (Piece piece : this.pieces) {

      Rectangle pieceBounds = piece.getBounds();
      if (rectangleToKeepFree.intersects(pieceBounds)) {
        // Get centers
        Point freeCenter = new Point(
                rectangleToKeepFree.x + rectangleToKeepFree.width / 2,
                rectangleToKeepFree.y + rectangleToKeepFree.height / 2);
        Point pieceCenter = new Point(
                pieceBounds.x + pieceBounds.width / 2,
                pieceBounds.y + pieceBounds.height / 2);

        // Calculate direction vector from free area to piece
        int dx = pieceCenter.x - freeCenter.x;
        int dy = pieceCenter.y - freeCenter.y;

        // Calculate intersection with rectangleToKeepFree boundaries

        // left
        double tx1 = (rectangleToKeepFree.getMinX() - pieceBounds.getMaxX()) / dx;
        // right
        double tx2 = (rectangleToKeepFree.getMaxX() - pieceBounds.getMinX()) / dx;
        // top
        double ty1 = (rectangleToKeepFree.getMinY() - pieceBounds.getMaxY()) / dy;
        // bottom
        double ty2 = (rectangleToKeepFree.getMaxY() - pieceBounds.getMinY()) / dy;

        // Find the minimal positive t (intersection point)
        double t = Double.POSITIVE_INFINITY;
        if (tx1 > 0) t = Math.min(t, tx1);
        if (tx2 > 0) t = Math.min(t, tx2);
        if (ty1 > 0) t = Math.min(t, ty1);
        if (ty2 > 0) t = Math.min(t, ty2);

        // Calculate new position (adding small epsilon to ensure no intersection)
        double epsilon = 0.1;
        double newX = pieceCenter.getX() + dx * (t + epsilon);
        double newY = pieceCenter.getY() + dy * (t + epsilon);

        // Move piece by the difference between new and current center
        int deltaX = (int)(newX - pieceCenter.getX());
        int deltaY = (int)(newY - pieceCenter.getY());

        piece.setPuzzlePosition(pieceBounds.x + deltaX, pieceBounds.y + deltaY);
      }
    }
  }

  public void arrange2(Rectangle rectangleToArrange) {
    Iterable<Piece> piecesInRect = getPiecesInRect(rectangleToArrange);
    List<Piece> piecesList = new ArrayList<>();
    piecesInRect.forEach(piecesList::add);

    if (piecesList.isEmpty()) return;

    // Calculate total area and average piece size
    int maxPieceWidth = 0;
    int maxPieceHeight = 0;

    for (Piece piece : piecesList) {
      Rectangle bounds = piece.getBounds();
      maxPieceWidth = Math.max(maxPieceWidth, bounds.width);
      maxPieceHeight = Math.max(maxPieceHeight, bounds.height);
    }

    // Calculate grid dimensions that could theoretically fit all pieces
    int cols = (int) Math.max(1, Math.floor(rectangleToArrange.getWidth() / maxPieceWidth));
    int rows = (int) Math.ceil((double) piecesList.size() / cols);

    // Calculate actual cell dimensions (may cause overlaps if space is too small)
    int cellWidth = rectangleToArrange.width / cols;
    int cellHeight = rectangleToArrange.height / rows;

    // Arrange pieces in grid
    int index = 0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        if (index >= piecesList.size()) break;

        Piece piece = piecesList.get(index++);
        Rectangle bounds = piece.getBounds();

        // Calculate position (centered in cell)
        int x = rectangleToArrange.x + c * cellWidth +
                (cellWidth - bounds.width) / 2;
        int y = rectangleToArrange.y + r * cellHeight +
                (cellHeight - bounds.height) / 2;

        piece.setPuzzlePosition(x, y);
      }
    }
  }

  public void shuffle(Rectangle destination, boolean randomizeRotation) {
    shuffle(new Rect(destination.x, destination.y, destination.width, destination.height), randomizeRotation);
  }

  public void shuffle(Rect destination, boolean randomizeRotation) {
    List<Piece> piecesToShuffle = new ArrayList<>();
    List<Piece> remainingPieces = new ArrayList<>();
    Random random = new Random();
    for (Piece piece : this.pieces) {
      if (destination.contains(piece.getPuzzleX(), piece.getPuzzleY())) {
        int x = (int) (destination.getX() + random.nextFloat() * (destination.getWidth() - piece.getCurrentWidth()));
        int y = (int) (destination.getY() + random.nextFloat() * (destination.getHeight() - piece.getCurrentHeight()));
        piece.setPuzzlePosition(x, y);
        if (randomizeRotation) {
          piece.setRotation(random.nextInt(3) * 90);
        }
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
   *
   * @return the pushed piece or null if the bin is empty.
   */
  public Piece push() {
    if (pieces.size() > 1) {
      Piece p = pieces.remove(pieces.size() - 1);
      pieces.add(0, p);
      return p;
    }
    return null;
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
