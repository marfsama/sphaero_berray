package uk.co.petertribble.sphaero2.components;

import uk.co.petertribble.sphaero2.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DropManager {

  /** All panels which can have pieces. Sorted by z-order, first entry is the top panel. */
  private List<PiecesPanel> piecesPanels = new ArrayList<>();

  /** true when a drag is currently in progress, false otherwise. */
  private boolean dragInProgress = false;

  /** the piece which is currently draggde .*/
  private Piece currentDragedPiece;
  /** panel from which the piece is from. */
  private PiecesPanel sourcePanel;
  /** the last panel which has shown the dragged piece. */
  private PiecesPanel lastPanel;


  public void clear() {
    piecesPanels.clear();
    dragInProgress = false;
    currentDragedPiece = null;
    sourcePanel = null;
    lastPanel = null;
  }

  public void addPiecesPanel(PiecesPanel piecesPanel) {
    piecesPanels.add(piecesPanel);
  }

  public void updateDrag(Point p) {

  }


  public void finishDrag() {

  }

}
