package uk.co.petertribble.sphaero2.gui;

import com.berray.GameObject;
import com.berray.components.core.Component;
import com.berray.event.Event;
import com.berray.math.Color;
import com.berray.math.Rect;
import com.berray.math.Vec2;
import com.raylib.Jaylib;
import uk.co.petertribble.sphaero2.model.MultiPiece;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import java.util.Map;

import static com.raylib.Raylib.*;

public class PiecesComponent extends Component {

  private final PiecesBin pieces;
  private final Map<Integer, PieceDescription> pieceDescriptions;
  private DragMode dragMode;
  /**
   * Position, where the mouse button was initially pressed down.
   */
  private Vec2 mouseDownPosition;
  private Vec2 pieceStart;
  private Piece clickedPiece;

  public PiecesComponent(PiecesBin pieces, Map<Integer, PieceDescription> pieceDescriptions) {
    super("pieceBoard", "area");
    this.pieces = pieces;
    this.pieceDescriptions = pieceDescriptions;
  }

  @Override
  public void add(GameObject gameObject) {
    super.add(gameObject);
    registerGetter("render", () -> true);
    registerGetter("size", this::getSize);

    on("mouseClick", this::onMouseClick);
    on("mousePress", this::onMousePress);
    on("dragStart", this::onDragStart);
    on("dragFinish", this::onDragFinish);
    on("dragging", this::onDragging);
  }

  private void onMousePress(Event event) {
    mouseDownPosition = event.getParameter(1);
  }

  private void onDragging(Event event) {
    Vec2 clickPosition = event.getParameter(1);
    Vec2 delta = clickPosition.sub(mouseDownPosition);

    switch (dragMode) {
      case TABLE:
        gameObject.doAction("moveBy", delta);
        break;
      case SELECTED_PIECES:
        Vec2 anchor = pieces.getSelectedAnchor();
        // calculate vector by which we moved the pieces already
        var alreadyMoved = anchor.sub(pieceStart);
        // calculate delta to total drag vector
        // this is the amount we need to move the selection
        var selectedDelta = delta.sub(alreadyMoved);
        if (selectedDelta.lengthSquared() > 1.0f) {
          pieces.moveSelected(selectedDelta);
        }
        break;
      case SINGLE_PIECE:
        float x = pieceStart.getX() + delta.getX();
        float y = pieceStart.getY() + delta.getY();
        pieces.movePieceTo(clickedPiece, (int) x, (int) y);
        break;
    }
  }

  private void onDragFinish(Event event) {
    switch (dragMode) {
      case SINGLE_PIECE:
        pieces.join(clickedPiece);
        break;
      case SELECTED_PIECES:
        for (Piece piece : pieces.getSelected()) {
          pieces.join(piece);
        }
        break;
    }
    this.dragMode = DragMode.NONE;
  }

  private void onDragStart(Event event) {
    Vec2 clickPosition = mouseDownPosition;
    this.clickedPiece = pieces.getPieceAt((int) clickPosition.getX(), (int) clickPosition.getY());
    if (clickedPiece == null) {
      this.dragMode = DragMode.TABLE;
    } else {
      // a piece was clicked.
      if (pieces.isSelected(clickedPiece)) {
        dragMode = DragMode.SELECTED_PIECES;
        pieceStart = pieces.getSelectedAnchor();
      } else {
        pieces.clearSelection();
        pieces.moveToTop(clickedPiece);
        dragMode = DragMode.SINGLE_PIECE;
        pieceStart = new Vec2(clickedPiece.getPuzzleX(), clickedPiece.getPuzzleY());
      }
    }
  }

  private void onMouseClick(Event event) {
    Vec2 clickPosition = event.getParameter(1);
    Vec2 mouseMoved = mouseDownPosition.sub(clickPosition);

    // only accept click when the mouse was not moved (else it is a drag)
    if (mouseMoved.lengthSquared() < 0.1f) {
      Piece clickedPiece = pieces.getPieceAt((int) clickPosition.getX(), (int) clickPosition.getY());
      if (clickedPiece != null) {
        this.clickedPiece = clickedPiece;
        pieces.moveToTop(clickedPiece);
      }
    }
  }

  private Vec2 getSize() {
    return new Vec2(pieces.getWidth(), pieces.getHeight());
  }

  @Override
  public void draw() {
    rlPushMatrix();
    {
      rlMultMatrixf(gameObject.getWorldTransform().toFloatTransposed());

      for (Piece piece : pieces.getPieces()) {
        if (piece instanceof MultiPiece) {
          MultiPiece multiPiece = (MultiPiece) piece;
          for (Piece subPiece : multiPiece.getSubs()) {
            int rotX = piece.getRotatedX();
            int rotY = piece.getRotatedY();
            int pieceX = subPiece.getRotatedX();
            int pieceY = subPiece.getRotatedY();

            int deltaX = pieceX - rotX;
            int deltaY = pieceY - rotY;

            drawPiece(subPiece, piece.getPuzzleX() + deltaX, piece.getPuzzleY() + deltaY, pieces.isSelected(piece), false);
          }
          // DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Jaylib.GOLD);
        } else {
          drawPiece(piece, piece.getPuzzleX(), piece.getPuzzleY(), pieces.isSelected(piece), false);
        }
      }


    }
    rlPopMatrix();

  }

  private void drawPiece(Piece piece, int x, int y, boolean selected, boolean highlight) {
    PieceDescription pieceDescription = pieceDescriptions.get(piece.getId());
    if (pieceDescription != null) {
      Texture texture = getAssetManager().getAsset("pieces_" + pieceDescription.getTexture()).getAsset();
      Color color = Color.WHITE;
      if (selected) {
        color = Color.GOLD;
      }

      Rect texturePosition = pieceDescription.getTexturePosition();
      switch (piece.getRotation()) {
        case 0:
          // everything is ok
          break;
        case 90:
          x += piece.getImageHeight();
          break;
        case 180:
          x += piece.getImageWidth();
          y += piece.getImageHeight();
          break;
        case 270:
          y += piece.getImageWidth();
          break;
      }

      if (highlight) {
        DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Jaylib.GOLD);
      }

      DrawTexturePro(texture,
          texturePosition.toRectangle(),
          new Rect(x, y, piece.getImageWidth(), piece.getImageHeight()).toRectangle(),
          Vec2.origin().toVector2(),
          piece.getRotation(),
          color.toRaylibColor());
    }
  }
}
