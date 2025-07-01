package uk.co.petertribble.sphaero2.components.play;

public enum DragMode {
  /** Drag is not active. */
  NONE,
  /** Drag the selected pieces, keeping the selection when the move is finished. */
  PIECES,
  /** Draw rectangle on the table. */
  MOVE_SELECTION_RECTANGLE,
  /** Drags a rectangle on the table. */
  DRAG_SELECTION_RECTANGLE
}
