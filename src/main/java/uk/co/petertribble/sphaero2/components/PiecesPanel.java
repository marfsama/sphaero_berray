package uk.co.petertribble.sphaero2.components;

import uk.co.petertribble.sphaero2.model.Piece;

import java.awt.*;

/**
 * A panel which has pieces.
 */
public interface PiecesPanel {

  /**
   * Returns the name of the pieces panel.
   */
  String getName();

  Dimension getSize();

  Point getLocation();

  int getZOrder();

  void setDragPiece(Piece piece);

  void dropPiece(Piece piece);

  Piece startDragPiece(Point p);

  void finishDragPiece(Piece piece);

}
