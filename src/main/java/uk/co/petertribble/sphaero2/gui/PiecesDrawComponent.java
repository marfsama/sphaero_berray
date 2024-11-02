package uk.co.petertribble.sphaero2.gui;

import com.berray.GameObject;
import com.berray.components.core.Component;
import com.berray.math.Color;
import com.berray.math.Rect;
import com.berray.math.Vec2;
import com.raylib.Jaylib;
import com.raylib.Raylib;
import uk.co.petertribble.sphaero2.model.MultiPiece;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import java.util.Map;

import static com.raylib.Raylib.*;

public class PiecesDrawComponent extends Component {
  private final PiecesBin pieces;
  private final Map<Integer, PieceDescription> pieceDescriptions;

  public PiecesDrawComponent(PiecesBin pieces, Map<Integer, PieceDescription> pieceDescriptions) {
    super("piecesDraw");
    this.pieces = pieces;
    this.pieceDescriptions = pieceDescriptions;
  }

  @Override
  public void add(GameObject gameObject) {
    super.add(gameObject);
    registerGetter("render", () -> true);
  }

  @Override
  public void draw() {
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
        //DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Jaylib.GOLD);
      } else {
        drawPiece(piece, piece.getPuzzleX(), piece.getPuzzleY(), pieces.isSelected(piece), false);
      }
    }
    // draw bounding box
    //Rect boundingBox = gameObject.getBoundingBox();
    //DrawRectangleLines((int) boundingBox.getX(), (int) boundingBox.getY(), (int) boundingBox.getWidth(), (int) boundingBox.getHeight(), Jaylib.BLACK);
  }

  private void drawPiece(Piece piece, int x, int y, boolean selected, boolean highlight) {
    PieceDescription pieceDescription = pieceDescriptions.get(piece.getId());
    if (pieceDescription != null) {
      Raylib.Texture texture = getAssetManager().getAsset("pieces_" + pieceDescription.getTexture()).getAsset();
      Color color = Color.WHITE;
      if (selected) {
        color = Color.GOLD;
      }

      Rect texturePosition = pieceDescription.getTexturePosition();
      Vec2 center = new Vec2(piece.getImageWidth() / 2.0f, piece.getImageHeight() / 2.0f);
      Vec2 centerRotated = new Vec2(piece.getCurrentWidth() / 2.0f, piece.getCurrentHeight() / 2.0f);

      DrawTexturePro(texture,
          texturePosition.toRectangle(),
          new Rect(x + centerRotated.getX(), y + centerRotated.getY(), piece.getImageWidth(), piece.getImageHeight()).toRectangle(),
          center.toVector2(),
          piece.getRotation(),
          color.toRaylibColor());

      if (highlight) {
        DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Jaylib.GOLD);
        DrawLine(piece.getPuzzleX(), piece.getPuzzleY(), piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY() + piece.getCurrentHeight(), Jaylib.GOLD);
        DrawLine(piece.getPuzzleX() + piece.getCurrentWidth(), piece.getPuzzleY(), piece.getPuzzleX(), piece.getPuzzleY() + piece.getCurrentHeight(), Jaylib.GOLD);

        DrawCircle((int) (piece.getPuzzleX() + centerRotated.getX()), (int) (piece.getPuzzleY() + centerRotated.getY()), 5.0f, Jaylib.GOLD);
      }
    }

  }

}
