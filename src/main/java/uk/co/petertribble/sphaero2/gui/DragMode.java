package uk.co.petertribble.sphaero2.gui;

public enum DragMode {
  /** Drag is not active. */
  NONE,
  /** Drag Table. */
  TABLE,
  /** Drag a single piece, without selecting it. */
  SINGLE_PIECE,
  /** Drag the selected pieces, keeping the selection when the move is finished. */
  SELECTED_PIECES,
  /** Draw rectangle on the table. */
  SELECTION_RECTANGLE
}
