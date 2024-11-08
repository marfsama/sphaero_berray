package uk.co.petertribble.sphaero2.components.play;

import com.berray.math.Rect;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Panel with pieces to display and solve. The pieces on this panel do not necessarily be all pieces of the jigsaw.
 */
public class JigsawPiecesPanel extends JPanel {

  public static final char ROTATE_LEFT = 'E';
  public static final char ROTATE_RIGHT = 'R';
  public static final char SHUFFLE = 'S';
  // change to Next and Previous ?
  public static final char PREV_BG = 'V';
  public static final char NEXT_BG = 'B';
  public static final char PUSH = 'P';
  public static final char CLEAR = 'C';
  public static final char SCALE_IN = '+';
  public static final char SCALE_OUT = '-';

  public float scale = 1.0f;

  public Point lastClick;

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

  private static final Rectangle emptyRect = new Rectangle(0, 0, 0, 0);

  private Dimension prefSize;
  private boolean mouseDown;
  private boolean clearMode;
  // Translation from a piece's upper-left corner to the point you clicked
  // on.
  private int transX;
  private int transY;
  private int bgColor = 4;
  private int clearX0;
  private int clearY0;
  private int clearX1;
  private int clearY1;
  private Color clearColor;

  // If a keyboard command can affect a piece, it'll be this one.
  // Typically, this piece should be last in zOrder, but you never know.
  private Piece focusPiece;
  /**
   * Bin with the pieces to display and edit.
   */
  private PiecesBin piecesBin;


  public JigsawPiecesPanel() {
    setOpaque(true);
    setFocusable(true);
    setBackground(bgColors[bgColor]);
    setCursor(NORMAL_CURSOR);
    setClearColor();
    addWiring();
  }

  private void addWiring() {
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

      @Override
      public void keyPressed(KeyEvent e) {
        keyPressed0(e);
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
    piecesBin.shuffle(new Rect(0,0, getWidth(), getHeight()));
    repaint();
  }

  /**
   * Push the top piece (at the front) to the bottom (the back).
   */
  public void push() {
    piecesBin.push();
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    Graphics2D g = (Graphics2D) graphics.create();
    g.scale(scale, scale);

    if (piecesBin == null) {
      return;
    }

    Map<Integer, Piece> piecesMap = new HashMap<>();
    piecesBin.getPieces().forEach(piece ->piecesMap.put(piece.getId(), piece));

    for (Piece piece : piecesBin.getPieces()) {
      piece.draw(g);
    }
    // draw lines from last piece to all its neighbors
    if (piecesBin.getPieces().size() > 0) {
      Piece lastPiece = piecesBin.getPieces().get(piecesBin.getPieces().size() - 1);
      /*
      for (Piece neighbour : lastPiece.getNeighbors()) {
        g.setColor(Color.GREEN);
        g.drawLine(lastPiece.getPuzzleX()+lastPiece.getCurrentWidth()/2, lastPiece.getPuzzleY()+lastPiece.getCurrentHeight()/2,
            neighbour.getPuzzleX()+neighbour.getCurrentWidth()/2, neighbour.getPuzzleY()+neighbour.getCurrentHeight()/2);
      }
       */
    }

    if (lastClick != null) {
      g.setColor(Color.WHITE);
      g.drawArc(lastClick.x - 10, lastClick.y - 10, 20, 20, 0, 360);
    }

    if (clearMode && mouseDown) {
      int cx = Math.min(clearX0, clearX1);
      int cy = Math.min(clearY0, clearY1);
      int cw = Math.abs(clearX0 - clearX1);
      int ch = Math.abs(clearY0 - clearY1);
      g.setColor(clearColor);
      g.fillRect(cx, cy, cw, ch);
    }
  }

  public float getScale() {
    return scale;
  }

  public void setScale(float scale) {
    this.scale = scale;
  }

  private void setClearMode(boolean flag) {
    clearMode = flag;
    setCursor(clearMode ? CLEAR_CURSOR : NORMAL_CURSOR);
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

    this.lastClick = new Point(x, y);
    repaint();

    requestFocus();
    if (piecesBin == null) {
      return;
    }
    mouseDown = true;
    if (clearMode) {
      startClearRect(e);
    } else {
      grabPiece(e);
    }
  }

  protected void mouseDragged0(MouseEvent e) {
    if (piecesBin == null) {
      return;
    }

    if (clearMode) {
      dragClearRect(e);
    } else {
      dragPiece(e);
    }
  }

  protected void mouseReleased0(MouseEvent e) {
    if (piecesBin == null) {
      return;
    }
    mouseDown = false;
    if (clearMode) {
      finishClearRect(e);
    } else {
      releasePiece();
    }
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

  private void grabPiece(MouseEvent e) {
    int jigsawX = (int) (e.getX() / scale);
    int jigsawY = (int) (e.getY() / scale);
    focusPiece = null;
    List<Piece> pieces = piecesBin.getPieces();
    ListIterator<Piece> iter = pieces.listIterator(pieces.size());
    while (focusPiece == null && iter.hasPrevious()) {
      Piece piece = iter.previous();
      if (piece.contains(jigsawX, jigsawY)) {
        focusPiece = piece;
        iter.remove();
      }
    }
    if (focusPiece != null) {
      pieces.add(focusPiece);
      transX = jigsawX - focusPiece.getPuzzleX();
      transY = jigsawY - focusPiece.getPuzzleY();
      // The focusPiece might have moved up in Z-order. At worst, we have
      // to repaint its bounding rectangle.
      repaint(0, (int) (focusPiece.getPuzzleX() * scale), (int) (focusPiece.getPuzzleY() * scale),
          (int) (focusPiece.getCurrentWidth() * scale), (int) (focusPiece.getCurrentHeight() * scale));
    }
  }

  private void dragPiece(MouseEvent e) {
    if (focusPiece == null) {
      return;
    }
    int jigsawX = (int) (e.getX() / scale);
    int jigsawY = (int) (e.getY() / scale);

    int prevX = focusPiece.getPuzzleX();
    int prevY = focusPiece.getPuzzleY();
    int prevW = focusPiece.getCurrentWidth();
    int prevH = focusPiece.getCurrentHeight();
    focusPiece.moveTo(jigsawX - transX, jigsawY - transY);
    // Repaint the focusPiece' previous and current bounding rects.
    repaint(0, (int) (prevX * scale), (int) (prevY * scale), (int) (prevW * scale) + 1, (int) (prevH * scale) + 1);
    repaint(0L, (int) (focusPiece.getPuzzleX() * scale), (int) (focusPiece.getPuzzleY() * scale),
        (int) (focusPiece.getCurrentWidth() * scale), (int) (focusPiece.getCurrentHeight() * scale));
  }

  private void releasePiece() {
    if (focusPiece == null) {
      return;
    }
    Piece newPiece = piecesBin.join(focusPiece);
    if (newPiece != null) {
      // Joined pieces may be of any size and number. Mouse release isn't
      // a terribly frequent event, so just repaint the whole thing.
      repaint();
      focusPiece = newPiece;
      pieceJoined(focusPiece);
    }
  }

  protected void pieceJoined(Piece focusPiece) {
  }

  private void startClearRect(MouseEvent e) {
    clearX0 = (int) (e.getX() / scale);
    clearY0 = (int) (e.getY() / scale);
  }

  private void dragClearRect(MouseEvent e) {
    int prevX1 = clearX1;
    int prevY1 = clearY1;
    clearX1 = (int) (e.getX() / scale);
    clearY1 = (int) (e.getY() / scale);
    int x = Math.min(clearX0, prevX1);
    int y = Math.min(clearY0, prevY1);
    int w = Math.abs(clearX0 - prevX1);
    int h = Math.abs(clearY0 - prevY1);
    repaint(0, x, y, w, h);
    x = Math.min(clearX0, clearX1);
    y = Math.min(clearY0, clearY1);
    w = Math.abs(clearX0 - clearX1);
    h = Math.abs(clearY0 - clearY1);
    repaint(0, x, y, w, h);
  }

  private void finishClearRect(MouseEvent e) {
    clearX1 = (int) (e.getX() * scale);
    clearY1 = (int) (e.getY() * scale);
    int cx0 = Math.max(0, Math.min(clearX0, clearX1));
    int cy0 = Math.max(0, Math.min(clearY0, clearY1));
    int cx1 = Math.min(getWidth(), Math.max(clearX0, clearX1));
    int cy1 = Math.min(getHeight(), Math.max(clearY0, clearY1));
    for (Piece piece : piecesBin.getPieces()) {
      if (intersects(piece, cx0, cy0, cx1, cy1)) {
        shuffle(piece, cx0, cy0, cx1, cy1);
      }
    }
    repaint();
  }


  /**
   * Return whether the piece intersects with the rectangle defined by the
   * given points.  ### Not perfect; returns true for some pieces that it
   * shouldn't.  Ideally, it should grab the part of the Piece in the
   * rectangle, and search it for non-transparent pixels.  Costly, so be
   * careful.
   * (x1,y1) guaranteed to be SE of (x0,y0)
   */
  private boolean intersects(Piece piece, int x0, int y0, int x1, int y1) {
    int px = piece.getPuzzleX();
    int py = piece.getPuzzleY();
    int pw = piece.getCurrentWidth();
    int ph = piece.getCurrentHeight();
    Rectangle r = new Rectangle(x0, y0, x1 - x0, y1 - y0);
    Rectangle rp = new Rectangle(px, py, pw, ph);
    return r.intersects(rp);
  }


  /**
   * Shuffle piece randomly, but keeping it out of the rectangle defined
   * by the given points.
   * (x1,y1) guaranteed to be SE of (x0,y0)
   */
  private void shuffle(Piece piece, int x0, int y0, int x1, int y1) {
    // Make the rectangle denoting where the Piece could be placed in the
    // whole panel.  Top point will be (0,0).
    int w = getWidth() - piece.getCurrentWidth();
    int h = getHeight() - piece.getCurrentHeight();
    // If w or h is negative, the piece is too big to be shuffled, so quit.
    if (w < 0 || h < 0) {
      return;
    }

    // Define the endpoints of the rectangle the Piece must avoid.
    int ax = Math.max(0, x0 - piece.getCurrentWidth());
    int ay = Math.max(0, y0 - piece.getCurrentHeight());
    // int aw = x1 - ax;
    int ah = y1 - ay;

    // Now define four rectangles forming the shape where the NW piece
    // corner could go.  I'll use BorderLayout rectangles as a guide.

    // FIXME we could calculate the areas directly and only create
    // the one rectangle we need for the shuffle call, or even not
    // create any rectangles as we only need the height and width
    Rectangle north = (ay == 0) ? emptyRect :
        new Rectangle(0, 0, w, ay);
    Rectangle south = (y1 >= h) ? emptyRect :
        new Rectangle(0, y1 + 1, w, h - y1);
    Rectangle west = (ax == 0 || ah == 0) ? emptyRect :
        new Rectangle(0, ay, ax, ah);
    Rectangle east = (x1 >= w || ah == 0) ? emptyRect :
        new Rectangle(x1, ay, w - x1, ah);

    int nArea = north.width * north.height;
    int sArea = south.width * south.height;
    int wArea = west.width * west.height;
    int eArea = east.width * east.height;
    int totalArea = nArea + sArea + wArea + eArea;

    int rand = (int) (Math.random() * totalArea);

    rand -= nArea;
    if (rand < 0) {
      shuffle(piece, north);
      return;
    }
    rand -= sArea;
    if (rand < 0) {
      shuffle(piece, south);
      return;
    }
    rand -= wArea;
    if (rand < 0) {
      shuffle(piece, west);
      return;
    }
    shuffle(piece, east);
  }

  private void shuffle(Piece piece, Rectangle rect) {
    int dx = (int) (Math.random() * rect.width);
    int dy = (int) (Math.random() * rect.height);
    piece.moveTo(rect.x + dx, rect.y + dy);
  }

  // Keyboard event handling ----------------------------------------------

  void keyTyped0(KeyEvent e) {
    char ch = Character.toUpperCase(e.getKeyChar());
    if (ch == PREV_BG) {
      prevBackground();
    } else if (ch == NEXT_BG) {
      nextBackground();
    } else if (ch == SCALE_IN) {
      updateScale(scale * 1.5f);
    } else if (ch == SCALE_OUT) {
      updateScale(scale / 1.5f);
    }
    if (piecesBin == null) {
      return;
    }
    if (ch == ROTATE_LEFT) {
      rotatePiece(270);
    } else if (ch == ROTATE_RIGHT) {
      rotatePiece(90);
    } else if (ch == SHUFFLE) {
      shuffle();
    } else if (ch == PUSH) {
      push();
    } else if (ch == CLEAR) {
      toggleClearMode();
    }
  }

  private void updateScale(float newScale) {
    scale = newScale;
    revalidate();
  }

  void keyPressed0(KeyEvent e) {
    if (piecesBin != null) {
      if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT) {
        rotatePiece(270);
      } else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT) {
        rotatePiece(90);
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
        push();
      }
    }
  }

  private void rotatePiece(int amount) {
    if (focusPiece == null) {
      return;
    }
    int newRotation = focusPiece.getRotation() + amount;
    newRotation %= 360;
    int prevW = focusPiece.getCurrentWidth();
    int prevH = focusPiece.getCurrentHeight();
    int prevX = focusPiece.getPuzzleX();
    int prevY = focusPiece.getPuzzleY();
    focusPiece.setRotation(newRotation);
    // Make the piece appear to rotate about its center.
    // ### Feature: When the mouse is down, rotate about the cursor instead
    //   of the center.
    int currW = focusPiece.getCurrentWidth();
    int currH = focusPiece.getCurrentHeight();
    int currX = prevX + (prevW - currW) / 2;
    int currY = prevY + (prevH - currH) / 2;
    focusPiece.moveTo(currX, currY);
    repaint(0, (int) (prevX * scale), (int) (prevY * scale), (int) (prevW * scale), (int) (prevH * scale));
    repaint(0, (int) (currX * scale), (int) (currY * scale), (int) (currW * scale), (int) (currH * scale));
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

  private void toggleClearMode() {
    // can't toggle clear mode while dragging
    if (!mouseDown) {
      setClearMode(!clearMode);
    }
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


}
