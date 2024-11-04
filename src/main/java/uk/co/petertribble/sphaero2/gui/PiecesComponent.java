package uk.co.petertribble.sphaero2.gui;

import com.berray.GameObject;
import com.berray.components.core.Component;
import com.berray.event.CoreEvents;
import com.berray.event.KeyEvent;
import com.berray.event.MouseEvent;
import com.berray.event.MouseWheelEvent;
import com.berray.math.Rect;
import com.berray.math.Vec2;
import com.berray.math.Vec3;
import com.raylib.Raylib;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import java.util.Map;

import static com.berray.event.CoreEvents.*;
import static com.raylib.Raylib.*;

public class PiecesComponent extends Component {

  private final PiecesBin pieces;
  private final Map<Integer, PieceDescription> pieceDescriptions;
  private DragMode dragMode;
  /**
   * Position, where the mouse button was initially pressed down, in game object coordinate space.
   */
  private Vec2 mouseDownPosition;
  /**
   * Position, where the mouse button was initially pressed down, in window coordinate space.
   */
  private Vec2 mouseDownPositionWindow;
  /**
   * Position anchor at which the dragging started. This can be a piece (if a piece is dragged) or the game object
   * (when the table is dragged).
   */
  private Vec2 dragStart;
  private Piece clickedPiece;
  private Vec2 mousePos;

  /**
   * scale ranges from 1 - 15, where 10 is original scale. Smaller values is zoom out.
   */
  private int scaleFactor = 10;

  private Rect selectionRectangle = null;
  private boolean selectionModifierPressed = false;


  public PiecesComponent(PiecesBin pieces, Map<Integer, PieceDescription> pieceDescriptions) {
    super("pieceBoard", "area", "pos", "scale");
    this.pieces = pieces;
    this.pieceDescriptions = pieceDescriptions;
  }

  @Override
  public void add(GameObject gameObject) {
    super.add(gameObject);
    registerGetter("size", this::getSize);

    on(MOUSE_CLICK, this::onMouseClick);
    on(MOUSE_PRESS, this::onMousePress);
    on(MOUSE_WHEEL_MOVE, this::onMouseWheelMove);
    on(DRAG_START, this::onDragStart);
    on(DRAG_FINISH, this::onDragFinish);
    on(DRAGGING, this::onDragging);
    onGame(KEY_PRESS, this::onKeyPress);
    onGame(CoreEvents.KEY_DOWN, this::onKeyDown);
    onGame(CoreEvents.KEY_UP, this::onKeyUp);

    // override bounding box so we get all mouse events.
    registerGetter("boundingBox", () -> new Rect(0, 0, gameObject.getGame().width(), gameObject.getGame().height()));

    registerBoundProperty("scaleFactor", this::getScaleFactor, this::setScaleFactor);

    // debug
    registerGetter("mousePos", () -> mousePos);
    on(HOVER, (MouseEvent event) -> mousePos = event.getGameObjectPos());
  }

  public int getScaleFactor() {
    return scaleFactor;
  }

  public void setScaleFactor(int scaleFactor) {
    if (scaleFactor < 1) {
      scaleFactor = 1;
    }
    if (scaleFactor > 15) {
      scaleFactor = 15;
    }
    this.scaleFactor = scaleFactor;

    float scale = scaleFactor / 10.0f;

    gameObject.set("scale", new Vec2(scale, scale));
  }

  private void onMouseWheelMove(MouseWheelEvent event) {
    Vec2 oldPos = event.getGameObjectPos();
    setScaleFactor((int) (scaleFactor + event.getWheelDelta()));

    // calculate position the mouse cursor should have when it is in the same game object position
    Vec3 newWorldPos = gameObject.getWorldTransform().multiply(oldPos.getX(), oldPos.getY(), 0);

    // move pieces table so the expected new window pos is the current pos
    Vec2 delta = event.getWindowPos().sub(newWorldPos.toVec2());
    gameObject.doAction("moveBy", delta);
  }

  private void onKeyUp(KeyEvent event) {
    if (event.getKeyCode() == KEY_LEFT_SHIFT || event.getKeyCode() == KEY_RIGHT_SHIFT) {
      Raylib.SetMouseCursor(Raylib.MOUSE_CURSOR_DEFAULT);
      selectionModifierPressed = false;
      if (dragMode == DragMode.SELECTION_RECTANGLE) {
        dragMode = DragMode.NONE;
      }
    }
  }

  private void onKeyDown(KeyEvent event) {
    if (event.getKeyCode() == KEY_LEFT_SHIFT || event.getKeyCode() == KEY_RIGHT_SHIFT) {
      Raylib.SetMouseCursor(Raylib.MOUSE_CURSOR_CROSSHAIR);
      selectionModifierPressed = true;
    }
  }

  private void onKeyPress(KeyEvent event) {
    // rotate clockwise
    switch (event.getKeyCode()) {
      case KEY_E:
        if (clickedPiece != null) {
          clickedPiece.setRotation((clickedPiece.getRotation() + 90) % 360);
        }
        break;
      // rotate counterclockwise
      case KEY_W:
        if (clickedPiece != null) {
          clickedPiece.setRotation((clickedPiece.getRotation() + 270) % 360);
        }
        break;
      // shuffle. Either shuffle pieces in selection rectangle or shuffle pieces on screen
      case KEY_SPACE: {
        Vec3 scale = gameObject.get("scale");
        var game = gameObject.getGame();
        if (selectionRectangle != null) {
          Vec2 pos = gameObject.worldPosToLocalPos(selectionRectangle.getPos());
          pieces.shuffle(new Rect(pos.getX(), pos.getY(), selectionRectangle.getWidth() / scale.getX(), selectionRectangle.getHeight() / scale.getY()));
        } else {
          Vec2 pos = gameObject.get("pos");
          pieces.shuffle(new Rect(-pos.getX(), -pos.getY(), (game.width() / scale.getX()), (game.height() / scale.getY())));
        }
      }
      break;
      // stack. Only works with selection. Move all pieces in selection to the center of the selection
      case KEY_S:
        if (selectionRectangle != null) {
          Vec3 scale = gameObject.get("scale");
          Vec2 pos = gameObject.worldPosToLocalPos(selectionRectangle.getPos());
          Rect localRect = new Rect(pos.getX(), pos.getY(), selectionRectangle.getWidth() / scale.getX(), selectionRectangle.getHeight() / scale.getY());
          Vec2 center = localRect.getCenter();
          for (Piece piece : pieces.getPieces()) {
            if (localRect.contains(piece.getPuzzleX(), piece.getPuzzleY()) ||
                localRect.contains(piece.getPuzzleX()+piece.getCurrentWidth(), piece.getPuzzleY()) ||
                localRect.contains(piece.getPuzzleX(), piece.getPuzzleY() + piece.getCurrentHeight()) ||
                localRect.contains(piece.getPuzzleX()+piece.getCurrentWidth(), piece.getPuzzleY() + piece.getCurrentHeight())
            ) {
              pieces.movePieceTo(piece, (int) center.getX(), (int) center.getY());
            }
          }
        }
        break;
    }
  }

  private void onMousePress(MouseEvent event) {
    mouseDownPosition = event.getGameObjectPos();
    mouseDownPositionWindow = event.getWindowPos();
  }

  private void onDragStart(MouseEvent event) {
    if (selectionModifierPressed) {
      dragMode = DragMode.SELECTION_RECTANGLE;
      dragStart = mouseDownPosition;
      GameObject selectionNode = gameObject.getChildren("selectionRectangle").get(0);
      selectionNode.set("pos", dragStart);
      selectionNode.set("size", new Vec2(1, 1));
      selectionNode.setPaused(false);
      return;
    }

    this.clickedPiece = pieces.getPieceAt((int) mouseDownPosition.getX(), (int) mouseDownPosition.getY());
    if (clickedPiece == null) {
      this.dragMode = DragMode.TABLE;
      this.dragStart = gameObject.get("pos");
      return;
    }
    // a piece was clicked.
    if (pieces.isSelected(clickedPiece)) {
      dragMode = DragMode.SELECTED_PIECES;
      dragStart = pieces.getSelectedAnchor();
    } else {
      pieces.clearSelection();
      pieces.moveToTop(clickedPiece);
      dragMode = DragMode.SINGLE_PIECE;
      dragStart = new Vec2(clickedPiece.getPuzzleX(), clickedPiece.getPuzzleY());
    }
  }

  private void onDragging(MouseEvent event) {
    Vec2 clickPosition = event.getGameObjectPos();
    Vec2 delta = clickPosition.sub(mouseDownPosition);

    switch (dragMode) {
      case TABLE:
        // table must be moved in window coordinate space
        Vec2 windowDelta = event.getWindowPos().sub(mouseDownPositionWindow);
        Vec2 pos = dragStart.add(windowDelta);
        gameObject.set("pos", pos);
        break;
      case SELECTION_RECTANGLE:
        // table must be moved in window coordinate space
        GameObject selectionNode = gameObject.getChildren("selectionRectangle").get(0);
        selectionNode.set("size", delta);
        break;
      case SELECTED_PIECES:
        Vec2 anchor = pieces.getSelectedAnchor();
        // calculate vector by which we moved the pieces already
        var alreadyMoved = anchor.sub(dragStart);
        // calculate delta to total drag vector
        // this is the amount we need to move the selection
        var selectedDelta = delta.sub(alreadyMoved);
        if (selectedDelta.lengthSquared() > 1.0f) {
          pieces.moveSelected(selectedDelta);
        }
        break;
      case SINGLE_PIECE:
        float x = dragStart.getX() + delta.getX();
        float y = dragStart.getY() + delta.getY();
        pieces.movePieceTo(clickedPiece, (int) x, (int) y);
        break;
    }
  }

  private void onDragFinish(MouseEvent event) {
    switch (dragMode) {
      case SINGLE_PIECE:
        Piece newPiece = pieces.join(clickedPiece);
        if (newPiece != null) {
          clickedPiece = newPiece;
        }
        break;
      case SELECTED_PIECES:
        for (Piece piece : pieces.getSelected()) {
          pieces.join(piece);
        }
        break;
      case SELECTION_RECTANGLE:
        Vec2 delta = event.getGameObjectPos().sub(mouseDownPosition);
        selectionRectangle = new Rect(dragStart, delta).normalize();
    }
    this.dragMode = DragMode.NONE;
  }

  private void onMouseClick(MouseEvent event) {
    Vec2 clickPosition = event.getGameObjectPos();
    Vec2 mouseMoved = mouseDownPosition.sub(clickPosition);

    // only accept click when the mouse was not moved (else it is a drag)
    MouseEvent.ButtonState leftButton = event.getButtonState(MouseEvent.Button.LEFT);
    if (mouseMoved.lengthSquared() < 0.1f) {
      // left mouse clicked: move piece to top
      if (leftButton == MouseEvent.ButtonState.RELEASED_AND_UP) {
        if (selectionModifierPressed) {
          // select/unselect piece
          Piece clickedPiece = pieces.getPieceAt((int) clickPosition.getX(), (int) clickPosition.getY());
          if (clickedPiece != null) {
            if (pieces.isSelected(clickedPiece)) {
              pieces.removeSelection(clickedPiece);
            } else {
              pieces.addSelection(clickedPiece);
            }
            this.clickedPiece = clickedPiece;
          }

        } else {
          // select/move single piece
          // clear selection rectangle
          selectionRectangle = null;
          GameObject selectionNode = gameObject.getChildren("selectionRectangle").get(0);
          selectionNode.setPaused(true);
          pieces.clearSelection();

          Piece clickedPiece = pieces.getPieceAt((int) clickPosition.getX(), (int) clickPosition.getY());
          if (clickedPiece != null) {
            this.clickedPiece = clickedPiece;
            pieces.moveToTop(clickedPiece);
          }
        }
      }
      // right click: rotate piece clockwise
      if (event.getButtonState(MouseEvent.Button.RIGHT) == MouseEvent.ButtonState.RELEASED_AND_UP) {
        Piece clickedPiece = pieces.getPieceAt((int) clickPosition.getX(), (int) clickPosition.getY());
        if (clickedPiece != null) {
          clickedPiece.setRotation((clickedPiece.getRotation() + 90) % 360);
        }
      }
    }
  }

  private Vec2 getSize() {
    return new Vec2(pieces.getWidth(), pieces.getHeight());
  }

}
