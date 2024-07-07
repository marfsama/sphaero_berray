package uk.co.petertribble.sphaero2.components;

import uk.co.petertribble.sphaero2.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

public class DragManager extends JPanel implements MouseListener, MouseMotionListener {

  /**
   * All panels which can have pieces. Sorted by z-order, first entry is the top panel.
   */
  private List<JigsawPiecesPanel> piecesPanels = new ArrayList<>();

  /**
   * true when a drag is currently in progress, false otherwise.
   */
  private boolean dragInProgress = false;

  /**
   * the piece which is currently draggde .
   */
  private Piece currentDraggedPiece;
  /**
   * current position of the piece in absolute screen coordinates
   */
  private Point currentDraggedPosition;
  /**
   * panel from which the piece is from.
   */
  private PiecesPanel sourcePanel;
  /**
   * the last panel which has shown the dragged piece.
   */
  private PiecesPanel lastPanel;

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (currentDraggedPiece != null && currentDraggedPosition != null) {
      Point locationOnScreen = this.getLocationOnScreen();
      int x = currentDraggedPosition.x - locationOnScreen.x;
      int y = currentDraggedPosition.y - locationOnScreen.y;
      g.drawRect(x, y, 100, 100);
    }
  }

  public void clear() {
    piecesPanels.forEach(panel -> {
      panel.removeMouseListener(DragManager.this);
      panel.removeMouseMotionListener(DragManager.this);
    });
    piecesPanels.clear();
    dragInProgress = false;
    currentDraggedPiece = null;
    sourcePanel = null;
    lastPanel = null;
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
      this.currentDraggedPiece = panel.getPieceAt(e.getPoint());
      this.currentDraggedPosition = e.getLocationOnScreen();
      this.sourcePanel = panel;
      System.out.println("clicked at " + currentDraggedPosition);
    } else {
      this.currentDraggedPiece = null;
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    dragInProgress = false;
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
      currentDraggedPosition = e.getLocationOnScreen();
      repaint();
    }

  }

  @Override
  public void mouseMoved(MouseEvent e) {

  }
}
