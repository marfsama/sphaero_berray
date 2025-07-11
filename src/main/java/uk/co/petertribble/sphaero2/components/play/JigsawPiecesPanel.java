package uk.co.petertribble.sphaero2.components.play;

import com.berray.math.Rect;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PieceSet;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Panel with pieces to display and solve. The pieces on this panel do not necessarily be all pieces of the jigsaw.
 */
public class JigsawPiecesPanel extends JPanel {

  public static final char ROTATE_LEFT = 'E';
  public static final char ROTATE_RIGHT = 'R';
  public static final char STACK = 'S';
  public static final char ARRANGE = 'A';
  // change to Next and Previous ?
  public static final char PREV_BG = 'V';
  public static final char NEXT_BG = 'B';
  public static final char PUSH = 'P';
  public static final char CLEAR = 'C';
  public static final char SCALE_IN = '+';
  public static final char SCALE_OUT = '-';

  private float scale = 1.0f;

  /** Point where the mouse was clicked when the drag started. */
  private Point dragStart;
  /** Anchor of the thing that is dragged when the drag started. This is either the top left corner of the selection
   * rectangle or the {@link PieceSet#getAnchor() anchor}  of the {@link PieceSet}. */
  private Point dragAnchor;

  // Available background colors
  private static final Color[] bgColors = {
      Color.BLACK,
      new Color(48, 0, 0),
      new Color(0, 48, 0),
      new Color(0, 0, 48),
      new Color(48, 48, 48),
      new Color(96, 0, 0),
      new Color(0, 96, 0),
      new Color(0, 0, 96),
      new Color(96, 96, 96),
      new Color(144, 0, 0),
      new Color(0, 144, 0),
      new Color(0, 0, 144),
      new Color(144, 144, 144),
  };

  private static final Color CLEAR_COLOR_W = new Color(255, 255, 255, 48);
  private static final Color CLEAR_COLOR_B = new Color(0, 0, 0, 48);

  private static final Cursor
      NORMAL_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR),
      CLEAR_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);

  private Dimension prefSize;
  // Translation from a piece's upper-left corner to the point you clicked
  // on.
  private int bgColor = 4;

  private DragMode dragMode = DragMode.NONE;
  private Rectangle selectionRectangle;
  private boolean selectionEnabled = false;

  private Color clearColor;

  // If a keyboard command can affect a piece, it'll be this one.
  // Typically, this piece should be last in zOrder, but you never know.
  private Piece focusPiece;
  /**
   * Bin with the pieces to display and edit.
   */
  private PiecesBin piecesBin;
  /** Set of pieces which are selected at the moment. */
  private PieceSet selection = new PieceSet();
  /** Pieces in the selection rectangle when is was drawn. */
  private PieceSet piecesInSelectionRectangle;

  private final List<Piece> animatingPieces = new ArrayList<>();
  private final Map<Piece, AnimationData> animationDataMap = new HashMap<>();
  private final Timer animationTimer;


  public JigsawPiecesPanel() {
    setOpaque(true);
    setFocusable(true);
    setBackground(bgColors[bgColor]);
    setCursor(NORMAL_CURSOR);
    setClearColor();
    addListeners();
    animationTimer = new Timer(10, this::timerAction);
  }

  public void setSelectionMode(boolean enabled) {
    this.selectionEnabled = enabled;
  }

  private float easeOut(float t) {
    return 1 - (1 - t) * (1 - t);
  }

  private void animatePieceTo(Piece piece, int endX, int endY, int durationMs) {
    // If already animating, remove from current animation
    if (animatingPieces.contains(piece)) {
      animatingPieces.remove(piece);
      animationDataMap.remove(piece);
    }

    // Set up new animation
    AnimationData data = new AnimationData(
            piece.getCurrentX(),
            piece.getCurrentY(),
            endX,
            endY,
            durationMs
    );

    // Update final puzzle position
    piece.setPuzzlePosition(endX, endY);

    animatingPieces.add(piece);
    animationDataMap.put(piece, data);

    if (!animationTimer.isRunning()) {
      animationTimer.start();
    }
  }

  public void animateAllToPuzzlePositions(int durationMs) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("method can only be called from the EDT");
    }
    for (Piece piece : piecesBin.getPieces()) {
      if (piece.getCurrentX() != piece.getPuzzleX() ||
              piece.getCurrentY() != piece.getPuzzleY()) {
        animatePieceTo(piece, piece.getPuzzleX(), piece.getPuzzleY(), durationMs);
      }
    }
  }

  private void addListeners() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        mousePressed0(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        mouseReleased0(e);
      }
    });
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        mouseDragged0(e);
      }
    });
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        keyTyped0(e);
      }
    });
  }

  public void setPiecesBin(PiecesBin piecesBin) {
    this.piecesBin = piecesBin;
  }

  public PiecesBin getPiecesBin() {
    return piecesBin;
  }

  /**
   * Move current pieces around randomly, randomize z-order, but don't
   * randomize rotation.
   */
  public void shuffle() {
    shuffle((int) (getWidth() / scale), (int) (getHeight() / scale));
  }

  public void shuffle(int width, int height) {
    shuffle(0, 0, width, height, false);
  }

  public void shuffleSelection() {
    if (selectionRectangle != null) {
      shuffle(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width, selectionRectangle.height, false);
    }
  }

  public void shuffle(int x, int y, int width, int height, boolean randomizeRotation) {
    piecesBin.shuffle(new Rect(x, y, width, height), randomizeRotation);
    animateAllToPuzzlePositions(500);
    repaint();
  }

  public void clearSelection() {
    if (selectionRectangle != null) {
      clear(selectionRectangle);
    }
  }

  public void clear(Rectangle rectangleToClear) {
    piecesBin.clear(rectangleToClear);
    animateAllToPuzzlePositions(500);
    repaint();
  }

  public void arrange() {
    if (selectionRectangle != null) {
      piecesBin.arrange2(selectionRectangle);
      animateAllToPuzzlePositions(500);
      repaint();
    }
  }

  public void repaintPieces() {
    piecesBin.getPieces().forEach(piece -> {
      piece.setCurrentPosition(piece.getPuzzleX(), piece.getPuzzleY());
    });

  }




  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    Piece piece = piecesBin.push();
    repaintRectangleScaled(piece.getDrawBounds());
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    Graphics2D g = (Graphics2D) graphics.create();
    g.scale(scale, scale);

    if (piecesBin == null) {
      return;
    }


    for (Piece piece : piecesBin.getPieces()) {
      if (selection.contains(piece)) {
        piece.drawHighlight(g);
      }
      piece.draw(g);
    }

    if (selectionRectangle != null) {
      g.setColor(clearColor);
      int x = Math.min(selectionRectangle.x, selectionRectangle.x + selectionRectangle.width);
      int y = Math.min(selectionRectangle.y, selectionRectangle.y + selectionRectangle.height);
      int width = Math.abs(selectionRectangle.width);
      int height = Math.abs(selectionRectangle.height);

      g.fillRect(x, y, width, height);
    }
  }

  public float getScale() {
    return scale;
  }

  public void setScale(float scale) {
    this.scale = scale;
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    if (piecesBin == null) {
      return super.getPreferredSize();
    }
    computePreferredSize();
    return prefSize;
  }

  /**
   * Ideally, the preferred area is roughly 1.5 times the area of the
   * image, and the preferred width is 5/3 of the preferred height.
   * However, if the result would be smaller than the image in either
   * dimension, it is enlarged to allow the image to fit.
   */
  private void computePreferredSize() {
    int iWidth = piecesBin.getWidth();
    int iHeight = piecesBin.getHeight();
    int area = iWidth * iHeight * 3 / 2;
    int width = (int) Math.sqrt(area * 5 / 3);
    int height = width * 3 / 5;
    width = Math.max(width, iWidth);
    height = Math.max(height, iHeight);
    prefSize = new Dimension((int) (width * scale), (int) (height * scale));
  }

  // Mouse event handling -------------------------------------------------

  protected void mousePressed0(MouseEvent e) {
    int x = (int) (e.getX() / scale);
    int y = (int) (e.getY() / scale);

    this.dragStart = new Point(x, y);

    requestFocus();
    if (piecesBin == null) {
      return;
    }

    // first check if the click is inside the selection rectangle
    if (selectionRectangle != null && selectionRectangle.contains(x,y)) {
      dragMode = DragMode.MOVE_SELECTION_RECTANGLE;
      dragAnchor = selectionRectangle.getLocation();
      // get pieces which are now in the selection rectangle (before moving)
      this.piecesInSelectionRectangle = piecesBin.getPiecesInRect(selectionRectangle);

      return;
    }

    // try to grab a piece.
    Piece grabbedPiece = grabPiece(e);
    // when a piece is grabbed (and therefore should be moved)...
    if (grabbedPiece != null) {
      // ... clear the selection rectangle and return
      selectionRectangle = null;
      dragMode = DragMode.PIECES;
      return;
    }

    // no piece grabbed. start selection rectangle
    selectionRectangle = new Rectangle((int) (e.getX() / scale), (int) (e.getY() / scale), 1, 1);
    dragMode = DragMode.DRAG_SELECTION_RECTANGLE;

  }

  protected void mouseDragged0(MouseEvent e) {
    if (piecesBin == null) {
      return;
    }

    if (dragMode == DragMode.MOVE_SELECTION_RECTANGLE) {
      moveSelectionRect(e);
      return;
    }

    if (dragMode == DragMode.DRAG_SELECTION_RECTANGLE) {
      dragSelectionRect(e);
      return;
    }

    if (dragMode == DragMode.PIECES) {
      dragPiece(e);
      return;
    }
  }

  protected void mouseReleased0(MouseEvent e) {
    if (piecesBin == null) {
      return;
    }
    if (dragMode == DragMode.DRAG_SELECTION_RECTANGLE) {
      finishSelectionRect(e);
    } else if (dragMode == DragMode.MOVE_SELECTION_RECTANGLE){
      // remove selection rectangle if it was not dragged
      if (dragStart.equals(e.getPoint())) {
        repaintRectangleScaled(selectionRectangle);
        selectionRectangle = null;
      }
    } else if (dragMode == DragMode.PIECES){
      releasePiece();
    }
    dragMode = DragMode.NONE;
  }

  public Piece getPieceAt(Point p) {
    int jigsawX = (int) (p.getX() / scale);
    int jigsawY = (int) (p.getY() / scale);
    List<Piece> pieces = piecesBin.getPieces();
    ListIterator<Piece> iter = pieces.listIterator(pieces.size());
    while (iter.hasPrevious()) {
      Piece piece = iter.previous();
      if (piece.contains(jigsawX, jigsawY)) {
        return piece;
      }
    }
    return null;
  }

  /**
   * Tries to grab the piece at the specified point. If there is a piece, it is set as the current focus piece and
   * returned. Else null is returned.
   */
  private Piece grabPiece(MouseEvent e) {
    int jigsawX = (int) (e.getX() / scale);
    int jigsawY = (int) (e.getY() / scale);
    focusPiece = null;
    List<Piece> pieces = piecesBin.getPieces();
    ListIterator<Piece> iter = pieces.listIterator(pieces.size());
    while (focusPiece == null && iter.hasPrevious()) {
      Piece piece = iter.previous();
      if (piece.contains(jigsawX, jigsawY)) {
        focusPiece = piece;
        // remove piece from whichever position it is currently
        iter.remove();
      }
    }
    // if the user clicked a piece
    if (focusPiece != null) {
      // add the piece to the stack again, this time on top
      pieces.add(focusPiece);
      if (selectionEnabled || (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
        // add or remove a piece from the selection
        selection.toggle(focusPiece);
      } else {
        // select a single piece. so clear and redraw the current selection
        selection.forEach(piece -> repaintRectangleScaled(piece.getDrawBounds()));
        selection.clear();
        selection.add(focusPiece);
      }

      dragAnchor = selection.getAnchorPoint();
      // The focusPiece might have moved up in Z-order. At worst, we have
      // to repaint its bounding rectangle.
      repaintRectangleScaled(focusPiece.getDrawBounds());
    }
    return focusPiece;
  }

  private void dragPiece(MouseEvent e) {
    if (focusPiece == null) {
      return;
    }
    int jigsawX = (int) (e.getX() / scale);
    int jigsawY = (int) (e.getY() / scale);

    Point currentAnchor = selection.getAnchorPoint();

    // calculate how far the selection was already moved since the start of the drag
    int startDeltaX = currentAnchor.x - dragAnchor.x;
    int startDeltaY = currentAnchor.y - dragAnchor.y;

    // calculate how much the rectangle should be moved since the start of the drag.
    int dragDeltaX = jigsawX - dragStart.x;
    int dragDeltaY = jigsawY - dragStart.y;

    // revert the already moved delta and add the new delta
    int deltaX = dragDeltaX - startDeltaX;
    int deltaY = dragDeltaY - startDeltaY;

    // repaint current selection
    selection.forEach(piece -> repaintRectangleScaled(piece.getDrawBounds()));
    // move selection to new place
    selection.moveBy(deltaX, deltaY, true);
    // repaint moved selection
    selection.forEach(piece -> repaintRectangleScaled(piece.getDrawBounds()));
  }

  private void repaintRectangleScaled(Rectangle rect) {
    int x = Math.min(rect.x, rect.x + rect.width);
    int y = Math.min(rect.y, rect.y + rect.height);
    int width = Math.abs(rect.width);
    int height = Math.abs(rect.height);

    repaint(0, (int) (x * scale), (int) (y * scale), (int) ((width + 1) * scale), (int) ((height + 1) * scale));
  }

  private void releasePiece() {
    if (focusPiece == null) {
      return;
    }
    Piece newPiece = piecesBin.join(focusPiece);
    if (newPiece != null) {
      // Joined pieces may be of any size and number. Mouse release isn't
      // a terribly frequent event, so just repaint the whole thing.
      focusPiece = newPiece;
      // when the pieces are joined, clear the selection and add the new piece as the single selected item
      selection.clear();
      selection.add(focusPiece);
      pieceJoined(focusPiece);
    }
  }

  protected void pieceJoined(Piece focusPiece) {
  }

  private void dragSelectionRect(MouseEvent e) {
    // repaint (clear) old selection rectangle
    repaintRectangleScaled(selectionRectangle);

    // calculate new selection rectangle
    int x1 = (int) (e.getX() / scale);
    int y1 = (int) (e.getY() / scale);

    selectionRectangle.width = x1 - selectionRectangle.x;
    selectionRectangle.height = y1 - selectionRectangle.y;

    // repaint new selection rectangle
    repaintRectangleScaled(selectionRectangle);
  }

  private void moveSelectionRect(MouseEvent e) {
    // repaint (clear) old selection rectangle
    repaintRectangleScaled(selectionRectangle);

    // calculate new position in puzzle coordinate system
    int x1 = (int) (e.getX() / scale);
    int y1 = (int) (e.getY() / scale);


    // calculate delta the rectangle was moved since the start of the drag
    int startDeltaX = selectionRectangle.x - dragAnchor.x;
    int startDeltaY = selectionRectangle.y - dragAnchor.y;

    // calculate how much the rectangle should be moved since the start of the drag.
    int dragDeltaX = x1 - dragStart.x;
    int dragDeltaY = y1 - dragStart.y;

    // revert the already moved delta and add the new delta
    int deltaX = dragDeltaX - startDeltaX;
    int deltaY = dragDeltaY - startDeltaY;

    // move rectangle...
    selectionRectangle.x += deltaX;
    selectionRectangle.y += deltaY;
    // ...  and pieces
    piecesInSelectionRectangle.moveBy(deltaX, deltaY, true);

    // repaint new selection rectangle
    repaintRectangleScaled(selectionRectangle);
  }


  private void finishSelectionRect(MouseEvent e) {
    // normalize selection rect (width and height > 0)
    int x = Math.min(selectionRectangle.x, selectionRectangle.x + selectionRectangle.width);
    int y = Math.min(selectionRectangle.y, selectionRectangle.y + selectionRectangle.height);
    int width = Math.abs(selectionRectangle.width);
    int height = Math.abs(selectionRectangle.height);
    selectionRectangle.x = x;
    selectionRectangle.y = y;
    selectionRectangle.width = width;
    selectionRectangle.height = height;

    repaint();
  }

  // Keyboard event handling ----------------------------------------------

  void keyTyped0(KeyEvent e) {
    char ch = Character.toUpperCase(e.getKeyChar());
    switch (ch) {
      case PREV_BG:
        prevBackground();
        break;
      case NEXT_BG:
        nextBackground();
        break;
      case SCALE_IN:
        updateScale(scale * 1.5f);
        break;
      case SCALE_OUT:
        updateScale(scale / 1.5f);
        break;
      case STACK:
        stack();
        break;
      case ARRANGE:
        arrange();
        break;
      case ROTATE_LEFT:
      case KeyEvent.VK_LEFT:
        rotatePiece(270);
        break;
      case ROTATE_RIGHT:
      case KeyEvent.VK_RIGHT:
        rotatePiece(90);
        break;
      case PUSH:
        push();
        break;
    }
    }

  private void updateScale(float newScale) {
    setScale(newScale);
    revalidate();
    repaint();
  }

  private void rotatePiece(int amount) {
    if (focusPiece == null) {
      return;
    }
    int newRotation = focusPiece.getRotation() + amount;
    newRotation %= 360;
    Rectangle prev = focusPiece.getDrawBounds();
    focusPiece.setRotation(newRotation);
    // Make the piece appear to rotate about its center.
    // ### Feature: When the mouse is down, rotate about the cursor instead
    //   of the center.
    int currW = focusPiece.getCurrentWidth();
    int currH = focusPiece.getCurrentHeight();
    int currX = prev.x + (prev.width - currW) / 2;
    int currY = prev.y + (prev.height - currH) / 2;
    focusPiece.moveTo(currX, currY);
    repaintRectangleScaled(prev);
    repaintRectangleScaled(focusPiece.getDrawBounds());
  }

  private void prevBackground() {
    bgColor--;
    if (bgColor < 0) {
      bgColor = bgColors.length - 1;
    }
    setBackground(bgColors[bgColor]);
    setClearColor();
    repaint();
  }

  private void nextBackground() {
    bgColor++;
    if (bgColor >= bgColors.length) {
      bgColor = 0;
    }
    setBackground(bgColors[bgColor]);
    setClearColor();
    repaint();
  }

  private void setClearColor() {
    clearColor = isBright(bgColors[bgColor]) ? CLEAR_COLOR_B
        : CLEAR_COLOR_W;
  }

  private boolean isBright(Color c) {
    float[] hsb =
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    return hsb[2] > 0.5;
  }

  public int getZOrder() {
    Container component = this;

    do {
      var parent = component.getParent();
      if (parent instanceof JLayeredPane) {
        JLayeredPane layeredPane = (JLayeredPane) parent;
        return layeredPane.getLayer(component);
      }
      component = parent;
    } while (component != null);

    return 0;
  }


  protected void stack() {
    if (selectionRectangle != null) {
      int centerX = selectionRectangle.x + selectionRectangle.width / 2;
      int centerY = selectionRectangle.y + selectionRectangle.height / 2;
      PieceSet selected = piecesBin.getPiecesInRect(selectionRectangle);
      selected.stack(new Point(centerX, centerY));
      animateAllToPuzzlePositions(500);
      repaint();
      return;
    }

    // no selection rectangle. so stack the current selection
    selection.stack(selection.getCenterPoint());
    animateAllToPuzzlePositions(500);
  }

  private void timerAction(ActionEvent e) { // ~60fps
    long currentTime = System.currentTimeMillis();
    boolean anyAnimationsRunning = false;
    Rectangle repaintRectangle = new Rectangle(0, 0, -1, -1);

    // Process all animating pieces
    Iterator<Piece> iterator = animatingPieces.iterator();
    while (iterator.hasNext()) {
      Piece piece = iterator.next();
      AnimationData data = animationDataMap.get(piece);

      long elapsed = currentTime - data.getStartTime();
      float progress = Math.min(1f, (float) elapsed / data.getDuration());

      // repaint current position piece
      repaintRectangle = repaintRectangle.union(piece.getDrawBounds());

      // Apply easing function (quadratic ease-out)
      progress = easeOut(progress);

      // Calculate current position
      int currentX = (int) (data.getStartX() + (data.getEndX() - data.getStartX()) * progress);
      int currentY = (int) (data.getStartY() + (data.getEndY() - data.getStartY()) * progress);

      piece.setCurrentPosition(currentX, currentY);

      // repaint new position piece
      repaintRectangle = repaintRectangle.union(piece.getDrawBounds());


      if (progress >= 1f) {
        // Animation complete
        iterator.remove();
        animationDataMap.remove(piece);
      } else {
        anyAnimationsRunning = true;
      }
    }

    if (repaintRectangle != null) {
      repaintRectangleScaled(repaintRectangle);
    }

    if (!anyAnimationsRunning) {
      animationTimer.stop();
    }
  }


}
