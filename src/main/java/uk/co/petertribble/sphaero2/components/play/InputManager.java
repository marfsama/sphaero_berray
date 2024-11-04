package uk.co.petertribble.sphaero2.components.play;

import uk.co.petertribble.sphaero2.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class InputManager extends JPanel implements MouseListener, MouseMotionListener, ComponentListener {

  /**
   * All panels which can have pieces. Sorted by z-order, first entry is the top panel.
   */
  private List<JigsawPiecesPanel> piecesPanels = new ArrayList<>();

  /**
   * true when a drag is currently in progress, false otherwise.
   */
  private boolean dragInProgress = false;

  /**
   * the piece which is currently dragged .
   */
  private Piece currentDraggedPiece;
  /**
   * current position of the piece in absolute screen coordinates
   */
  private Point currentDraggedPosition;
  private Point pieceRelativePos;
  /**
   * panel from which the piece is from.
   */
  private JigsawPiecesPanel sourcePanel;


  public InputManager() {
    setOpaque(false);
  }

  public void clear() {
    piecesPanels.forEach(panel -> {
      panel.removeMouseListener(InputManager.this);
      panel.removeMouseMotionListener(InputManager.this);
    });
    piecesPanels.clear();
    dragInProgress = false;
    currentDraggedPiece = null;
    sourcePanel = null;
  }

  public void addPiecesPanel(JigsawPiecesPanel piecesPanel) {
    piecesPanels.add(piecesPanel);
    piecesPanel.addMouseListener(this);
    piecesPanel.addMouseMotionListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    var source = e.getSource();
    if (source instanceof JigsawPiecesPanel) {
      JigsawPiecesPanel panel = (JigsawPiecesPanel) source;
      Point point = e.getPoint();
      this.currentDraggedPiece = panel.getPieceAt(point);
      if (currentDraggedPiece != null) {
        this.currentDraggedPosition = e.getLocationOnScreen();
        this.sourcePanel = panel;
        int deltaX = (int) (point.x - currentDraggedPiece.getPuzzleX() * panel.getScale());
        int deltaY = (int) (point.y - currentDraggedPiece.getPuzzleY() * panel.getScale());
        this.pieceRelativePos = new Point(deltaX, deltaY);
      }
    } else {
      this.currentDraggedPosition = null;
      this.currentDraggedPiece = null;
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (!dragInProgress) {
      return;
    }
    dragInProgress = false;
    // get panel over which the mouse cursor is
    Point locationOnScreen = e.getLocationOnScreen();
    ListIterator<JigsawPiecesPanel> iterator = this.piecesPanels.listIterator(this.piecesPanels.size());
    while (iterator.hasPrevious()) {
      JigsawPiecesPanel panel = iterator.previous();
      Point panelOnScreen = panel.getLocationOnScreen();
      Dimension size = panel.getSize();
      Point relativePos = new Point(locationOnScreen.x-panelOnScreen.x, locationOnScreen.y-panelOnScreen.y );
      if (relativePos.x > 0 && relativePos.y > 0 && relativePos.x < size.width && relativePos.y < size.height) {
        if (sourcePanel != panel) {
          // drop piece in this panel
          System.out.println("drop to " + panel.getPiecesBin().getName() + " @ " + relativePos);
          currentDraggedPiece.setPuzzlePosition((int) (relativePos.x/panel.getScale()), (int) (relativePos.y/panel.getScale()));
          panel.getPiecesBin().getPieces().add(currentDraggedPiece);
          sourcePanel.getPiecesBin().getPieces().remove(currentDraggedPiece);
          break;
        }
      }
    }

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (!dragInProgress && currentDraggedPiece != null) {
      dragInProgress = true;
    }
    currentDraggedPosition = e.getLocationOnScreen();
    //System.out.println("dragged to " + currentDraggedPosition);
    repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    // find current position and piece
    Point point = e.getPoint();
    //sourcePanel.getMousePosition();


  }

  @Override
  public void componentResized(ComponentEvent e) {
    setSize(e.getComponent().getSize());
    invalidate();
  }

  @Override
  public void componentMoved(ComponentEvent e) {

  }

  @Override
  public void componentShown(ComponentEvent e) {

  }

  @Override
  public void componentHidden(ComponentEvent e) {

  }
}
