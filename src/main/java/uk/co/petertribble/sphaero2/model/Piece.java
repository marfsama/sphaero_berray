package uk.co.petertribble.sphaero2.model;

import uk.co.petertribble.sphaero2.cutter.BevelUtil;

import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

// ### Should Pieces be implemented as BufferedImages backed by Rasters,
// rather than images backed by MemoryImageSources?

// ### Classic piece edges are problematic. First of all, the corners are
// weird.  Secondly, the edges are probably too close together. This is
// causing the bevels to overlap very slightly, so they don't look quite
// right.

/**
 * A single piece of a jigsaw puzzle. Each piece knows the portion of the
 * image it contains, how its edges are to be drawn, and what its
 * neighboring pieces are.
 * <p>
 * When two or more Pieces are put together, the result is another
 * Piece object, a MultiPiece.
 *
 * @see MultiPiece
 */
public class Piece {
  // Class constants ------------------------------------------------------

  /**
   * A Piece must be within this many pixels of "perfect" to be considered close.
   */
  private static final int POSITION_PROXIMITY_THRESHOLD = 7;

  /**
   * A Piece must be within this many degrees of rotation from another to be considered aligned to it.
   */
  private static final int ROTATION_PROXIMITY_THRESHOLD = 5;


  /**
   * each piece has a unique id (used for saving the jigsaw to disk).
   */
  protected int id;
  /**
   * Pieces considered to be neighbors to this one.  They are the only
   * ones that can be fitted to it.
   */
  protected Set<Piece> neighbors;
  /**
   * Original image size and data.
   */
  protected int origWidth;
  protected int origHeight;
  /**
   * Current size and data, taking rotation into account.
   */
  protected int curWidth;
  protected int curHeight;
  protected int[] curData;
  /** Highlight layer. */
  protected int highlightWidth;
  protected int highlightHeight;
  protected int highlightSize = 10;
  private int[] highlightData;

  // Location in the image.
  private final int imageX;
  private final int imageY;
  // Size of the entire image.
  private final int totalWidth;
  private final int totalHeight;
  // Location in the image adjusted by current rotation.
  private int rotatedX;
  private int rotatedY;
  private final int[] origData;
  // Location in the puzzle panel.
  private int puzzleX;
  private int puzzleY;
  // Image for this Piece. null for a MultiPiece
  private Image image;
  private Image hightlightImage;

  // Accessors ------------------------------------------------------------
  // This is measured in integer degrees, 0-359.  0 is unrotated.  90 is 90
  // degrees clockwise, etc.
  private int rotation;

  // Current position (for animation)
  private int currentX;
  private int currentY;

  /**
   * Creates a new Piece.  No initial rotation is done.  (This is needed
   * by MultiPiece, which needs to set its subpieces before rotating.)
   *
   * @param data        image data
   * @param imageX      X position of image relative to entire puzzle
   * @param imageY      Y position of image relative to entire puzzle
   * @param imageWidth  width of original image
   * @param imageHeight height of original image
   * @param totalWidth  the width of the entire picture
   * @param totalHeight the height of the entire picture
   */
  protected Piece(int pieceNum, int[] data,
                  int imageX, int imageY,
                  int imageWidth, int imageHeight,
                  int totalWidth, int totalHeight) {
    this.neighbors = new HashSet<>();
    this.id = pieceNum;
    this.origData = data;
    this.imageX = imageX;
    this.imageY = imageY;

    this.puzzleX = imageX;
    this.puzzleY = imageY;
    this.currentX = imageX;
    this.currentY = imageY;

    this.curWidth = this.origWidth = imageWidth;
    this.curHeight = this.origHeight = imageHeight;
    this.totalWidth = totalWidth;
    this.totalHeight = totalHeight;
  }

  /**
   * Creates a new Piece.
   *
   * @param data        image data
   * @param imageX      X position of image relative to entire puzzle
   * @param imageY      Y position of image relative to entire puzzle
   * @param imageWidth  width of original image
   * @param imageHeight height of original image
   * @param totalWidth  the width of the entire picture
   * @param totalHeight the height of the entire picture
   * @param rotation    initial rotation
   */
  public Piece(int pieceNum, int[] data,
               int imageX, int imageY,
               int imageWidth, int imageHeight,
               int totalWidth, int totalHeight,
               int rotation) {
    this(pieceNum, data, imageX, imageY, imageWidth, imageHeight, totalWidth, totalHeight);
    forceSetRotation(rotation);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Set<Piece> getNeighbors() {
    return neighbors;
  }

  public int[] getData() {
    return origData;
  }

  public Set<Piece> getSubs() {
    return Set.of(this);
  }

  /**
   * Returns this Piece's current rotation.  The rotation is given in
   * integer degrees clockwise, and will always be between 0 and 359
   * inclusive.
   *
   * @return the Piece's current rotation.
   * @see #setRotation
   */
  public int getRotation() {
    return rotation;
  }

  /**
   * Sets this Piece's current rotation.  The rotation is given in integer
   * degrees clockwise, and should always be between 0 and 359 inclusive.
   * If the new rotation is different, this Piece's image data will be
   * recomputed.
   *
   * @param rot The new rotation
   * @throws IllegalArgumentException if rotation is not in [0,359]
   * @see #getRotation
   */
  public void setRotation(int rot) {
    if (rot != rotation) {
      forceSetRotation(rot);
    }
  }

  /**
   * Sets this Piece's current rotation.  Unlike setRotation(), this
   * method forces a recompute of the image.
   *
   * @param rot The new rotation
   */
  protected void forceSetRotation(int rot) {
    if ((rot < 0) || (rot > 359)) {
      rot = 0;
    }
    // For now, allow only 0, 90, 180, 270.
    if (rot % 90 != 0) {
      int newRot = rot / 90;
      rot = 90 * newRot;
    }
    rotation = rot;
    recomputeImageData();
    if (image != null) {
      image.flush();
    }
    image = Toolkit.getDefaultToolkit().createImage(
        new MemoryImageSource(
            curWidth, curHeight, curData, 0, curWidth));
  }

  /**
   * Sets this Piece's upper-left position relative to the upper-left
   * position of the JigsawPuzzle.
   *
   * @param x The Piece's new x position
   * @param y The Piece's new y position
   */
  public void setPuzzlePosition(int x, int y) {
    this.puzzleX = x;
    this.puzzleY = y;
  }

  /**
   * Returns this Piece's current height in pixels.  This is the height of
   * the smallest rectangle containing all of this Piece's image data at
   * its current rotation.
   *
   * @return the Piece's current height in pixels
   */
  public int getCurrentHeight() {
    return curHeight;
  }

  /**
   * Returns this Piece's current width in pixels.  This is the width of
   * the smallest rectangle containing all of this Piece's image data at
   * its current rotation.
   *
   * @return the Piece's current width in pixels
   */
  public int getCurrentWidth() {
    return curWidth;
  }

  /**
   * Returns the width of the entire picture.
   *
   * @return the entire puzzle's current width in pixels
   */
  public int getTotalWidth() {
    return totalWidth;
  }

  /**
   * Returns the height of the entire picture.
   *
   * @return the entire puzzle's current height in pixels
   */
  public int getTotalHeight() {
    return totalHeight;
  }

  /**
   * Returns this Piece's image height in pixels.  This is the height of
   * the smallest rectangle containing all of this Piece's image data in
   * its original orientation.
   *
   * @return the Piece's current image height in pixels
   */
  public int getImageHeight() {
    return origHeight;
  }

  /**
   * Returns this Piece's image width in pixels.  This is the width of the
   * smallest rectangle containing all of this Piece's image data in its
   * original orientation.
   *
   * @return the Piece's current image width in pixels
   */
  public int getImageWidth() {
    return origWidth;
  }

  /**
   * Returns this Piece's X position in the original image.
   *
   * @return the Piece's X position in the original image
   */
  public int getImageX() {
    return imageX;
  }

  /**
   * Returns this Piece's Y position in the original image.
   *
   * @return the Piece's Y position in the original image
   */
  public int getImageY() {
    return imageY;
  }

  /**
   * Returns this Piece's X position in the original image, modified by
   * its current rotation.  The origin is the center of rotation.
   *
   * @return the Piece's rotated X position in the original image
   */
  public int getRotatedX() {
    return rotatedX;
  }

  /**
   * Returns this Piece's Y position in the original image, modified by
   * its current rotation.  The origin is the center of rotation.
   *
   * @return the Piece's rotated Y position in the original image
   */
  public int getRotatedY() {
    return rotatedY;
  }

  /**
   * Returns this Piece's X position in the puzzle.
   *
   * @return this Piece's X position
   */
  public int getPuzzleX() {
    return puzzleX;
  }

  /**
   * Returns this Piece's Y position in the puzzle.
   *
   * @return this Piece's Y position
   */
  public int getPuzzleY() {
    return puzzleY;
  }

  /** Returns the bounds of the piece in the current position and rotation. */
  public Rectangle getBounds() {
    return new Rectangle(puzzleX, puzzleY, curWidth, curHeight);
  }

  /** Returns the bounds of the drawn stuff. */
  public Rectangle getDrawBounds() {
    return new Rectangle(puzzleX - highlightSize, puzzleY - highlightSize, curWidth + highlightSize * 2, curHeight + highlightSize * 2);
  }

  public int getCurrentX() {
    return currentX;
  }

  public int getCurrentY() {
    return currentY;
  }

  public void setCurrentPosition(int x, int y) {
    this.currentX = x;
    this.currentY = y;
  }

  /**
   * Returns this Piece's current image.  This will be the Piece's portion
   * of the original image, rotated by this Piece's current rotation.
   *
   * @return this Piece's portion of the overall image
   */
  public Image getImage() {
    return image;
  }

  /**
   * Returns this Piece's original image
   *
   * @return this Piece's portion of the overall image
   */
  public Image getOriginalImage() {
    return Toolkit.getDefaultToolkit().createImage(
        new MemoryImageSource(origWidth, origHeight, origData, 0, origWidth));
  }


  /**
   * Adds a Piece to this Piece's set of neighbors.
   *
   * @param neighbor the Piece to add to this Piece's set of neighbors
   */
  public void addNeighbor(Piece neighbor) {
    neighbors.add(neighbor);
  }

  /**
   * Removes the given Piece from this Piece's set of neighbors.
   *
   * @param neighbor the Piece to remove from this Piece's set of neighbors
   */
  public void removeNeighbor(Piece neighbor) {
    neighbors.remove(neighbor);
  }

  // Joining pieces -------------------------------------------------------

  /**
   * Moves this Piece to the given location, relative to the puzzle
   * panel's upper-left corner.
   *
   * @param x The Piece's new x position
   * @param y The Piece's new y position
   */
  public void moveTo(int x, int y) {
    setPuzzlePosition(x, y);
  }


  // Bevel drawing --------------------------------------------------------

  @Override
  public String toString() {
    return "Piece[iPos=(" + imageX + "," + imageY + "),"
        + "iSiz=" + origWidth + "x" + origHeight + ","
        + "rot=" + rotation + ","
        + "rPos=(" + rotatedX + "," + rotatedY + "),"
        + "pPos=(" + puzzleX + "," + puzzleY + ")]";
  }

  /**
   * Draws this Piece in the given Graphics object.  The current image
   * will be drawn, at this Piece's current puzzle position.
   *
   * @param g the Graphics object to draw to
   */
  public void draw(Graphics g) {
    draw(g, currentX, currentY);
  }

  /**
   * Draws this Piece in the given Graphics object.  The current image
   * will be drawn, at this Piece's current puzzle position.
   *
   * @param g the Graphics object to draw to
   */
  public void draw(Graphics g, int x, int y) {
    if (image != null) {
      g.drawImage(image, x, y, null);
    }
  }

  /**
   * Draws this Pieces highlight in the given Graphics object.  The current image
   * will be drawn, at this Piece's current puzzle position.
   *
   * @param g the Graphics object to draw to
   */
  public void drawHighlight(Graphics g) {
    drawHighlight(g, getCurrentX(), getCurrentY());
  }

  /**
   * Draws this Pieces highlight in the given Graphics object.  The current image
   * will be drawn, at this Piece's current puzzle position.
   *
   * @param g the Graphics object to draw to
   */
  public void drawHighlight(Graphics g, int x, int y) {
    if (hightlightImage != null) {
      g.drawImage(hightlightImage, x - highlightSize, y - highlightSize, null);
    }
  }


  /**
   * Returns whether this Piece currently contains the given point,
   * relative to the puzzle panel's upper-left corner.
   *
   * @param x The x coordinate to be checked
   * @param y The y coordinate to be checked
   * @return true if the given coordinates are inside the current Piece
   */
  public boolean contains(int x, int y) {
    int puzX = getPuzzleX();
    int puzY = getPuzzleY();
    int w = getCurrentWidth();
    int h = getCurrentHeight();
    return
        (puzX <= x) && (x <= (puzX + w - 1)) &&
            (puzY <= y) && (y <= (puzY + h - 1)) &&
            (getAlpha(x - puzX, y - puzY) != 0);
  }

  /**
   * Returns the alpha (transparency) value at the given coordinates in
   * the current image data.
   *
   * @param x The x coordinate to be checked
   * @param y The y coordinate to be checked
   * @return the alpha transparency at the given coordinates
   */
  protected int getAlpha(int x, int y) {
    int pixel = curData[y * curWidth + x];
    return (pixel >> 24) & 0xff;
  }

  /**
   * Returns whether this piece is located and oriented close enough to
   * the given Piece to be fitted.
   *
   * @param piece The other Piece to check for closeness
   * @return true if this Peiec and the given Piece are located and oriented
   * close enough together to be fitted
   */
  protected boolean isCloseTo(Piece piece) {
    // Don't even bother if they're not aligned.
    int rotD = Math.abs(piece.getRotation() - rotation);
    rotD = Math.min(rotD, 360 - rotD);
    if (rotD > ROTATION_PROXIMITY_THRESHOLD) {
      return false;
    }
    int puzXD = getPuzzleX() - piece.getPuzzleX();
    int puzYD = getPuzzleY() - piece.getPuzzleY();
    int rotXD = getRotatedX() - piece.getRotatedX();
    int rotYD = getRotatedY() - piece.getRotatedY();
    return
        (Math.abs(puzXD - rotXD) <= POSITION_PROXIMITY_THRESHOLD) &&
            (Math.abs(puzYD - rotYD) <= POSITION_PROXIMITY_THRESHOLD);
  }

  /**
   * Checks whether any of this Piece's neighbors are located and oriented
   * close enough to be joined to this one.
   *
   * @return an array of Pieces, or null if no neighbors were close enough;
   * if the array is non-null, the first Piece will be the new one;
   * subsequent Pieces will be the ones it was built from
   */
  public Piece[] join(Supplier<Integer> idProvider) {
    Set<Piece> close = new HashSet<Piece>();
    for (Piece piece : neighbors) {
      if (piece.isCloseTo(this)) {
        close.add(piece);
      }
    }
    if (close.isEmpty()) {
      return null;
    }

    // We can't just return the new MultiPiece, because the JigsawPuzzle
    // needs to know what pieces the new one was formed from that are
    // currently in its list.  These might include other MultiPieces, which
    // wouldn't be in the new Piece's subpiece list.
    Piece newPiece = MultiPiece.join(this, close);
    newPiece.id = idProvider.get();
    Piece[] ret = new Piece[close.size() + 2];
    ret[0] = newPiece;
    ret[1] = this;
    this.image.flush();
    int i = 2;
    for (Piece piece : close) {
      ret[i] = piece;
      piece.image.flush();
      i++;
    }
    System.gc();
    return ret;
  }

  // 4-way rotation -------------------------------------------------------

  /**
   * Sets this Piece's rotated position and size, based on its current
   * rotation.  The entire puzzle is rotated about the origin, and then
   * translated so that its new upper left corner is at the origin.  Each
   * piece's rotated position is then defined by its new upper left corner
   * in the rotated puzzle.
   */
  protected void setRotatedPosition() {
    if (rotation == 0) {
      rotatedX = imageX;
      rotatedY = imageY;
      curWidth = origWidth;
      curHeight = origHeight;
    } else if (rotation == 90) {
      rotatedX = totalHeight - imageY - origHeight;
      rotatedY = imageX;
      curWidth = origHeight;
      curHeight = origWidth;
    } else if (rotation == 180) {
      rotatedX = totalWidth - imageX - origWidth;
      rotatedY = totalHeight - imageY - origHeight;
      curWidth = origWidth;
      curHeight = origHeight;
    } else if (rotation == 270) {
      rotatedX = imageY;
      rotatedY = totalWidth - imageX - origWidth;
      curWidth = origHeight;
      curHeight = origWidth;
    }
  }

  /**
   * Recomputes this Piece's current image data and size from its original
   * image data and rotation.
   */
  public void recomputeImageData() {
    setRotatedPosition();
    if (rotation == 0) {
      curData = origData.clone();
    } else if (rotation == 90) {
      curData = new int[origData.length];
      for (int i = 0; i < curWidth; i++) {
        for (int j = 0; j < curHeight; j++) {
          curData[j * curWidth + i] =
              origData[(origHeight - i - 1) * origWidth + j];
        }
      }
    } else if (rotation == 180) {
      curData = new int[origData.length];
      /*
       * for (int i = 0; i < curWidth; i++)
       * for (int j = 0; j < curHeight; j++)
       * curData[j*curWidth+i] =
       * origData[(origHeight-j-1)*origWidth + (origWidth-i-1)];
       */
      // it's just a reverse
      for (int i = 0; i < origData.length; i++) {
        curData[i] = origData[origData.length - i - 1];
      }
    } else if (rotation == 270) {
      curData = new int[origData.length];
      for (int i = 0; i < curWidth; i++) {
        for (int j = 0; j < curHeight; j++) {
          curData[j * curWidth + i] =
              origData[i * origWidth + (origWidth - j - 1)];
        }
      }
    }
    curData = BevelUtil.bevel(curData, curWidth, curHeight, 5);
    highlightData = BevelUtil.glow(curData, curWidth, curHeight, highlightSize, 0x40FFFF00);
    highlightWidth = curWidth + highlightSize * 2;
    highlightHeight = curHeight + highlightSize * 2;
    image = Toolkit.getDefaultToolkit().createImage(
        new MemoryImageSource(curWidth, curHeight, curData, 0, curWidth));
    hightlightImage = Toolkit.getDefaultToolkit().createImage(
            new MemoryImageSource(highlightWidth, highlightHeight, highlightData, 0, highlightWidth));
  }
}
