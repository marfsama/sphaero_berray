package uk.co.petertribble.sphaero2.gui;

import com.berray.GameObject;
import com.berray.components.core.Component;
import com.berray.math.Color;
import com.berray.math.Rect;
import com.berray.math.Vec2;
import com.raylib.Raylib;
import org.bytedeco.javacpp.FloatPointer;
import uk.co.petertribble.sphaero2.model.MultiPiece;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import java.nio.FloatBuffer;
import java.util.Map;

import static com.raylib.Raylib.*;

public class PiecesDrawComponent extends Component {
  private final PiecesBin pieces;
  private final Map<Integer, PieceDescription> pieceDescriptions;
  private final Shader shader;
  private final int highlightMask;

  public PiecesDrawComponent(PiecesBin pieces, Map<Integer, PieceDescription> pieceDescriptions, Shader shader) {
    super("piecesDraw");
    this.pieces = pieces;
    this.pieceDescriptions = pieceDescriptions;
    this.shader = shader;

    this.highlightMask = GetShaderLocation(shader, "highlightMask");
    SetShaderValue(shader, highlightMask, new FloatPointer(FloatBuffer.wrap(new float[] {1.0f, 0.0f, 1.0f, 0.0f})), SHADER_UNIFORM_VEC4);
  }

  @Override
  public void add(GameObject gameObject) {
    super.add(gameObject);
    registerGetter("render", () -> true);
  }

  @Override
  public void draw() {
    BeginShaderMode(shader);
    for (Piece piece : pieces.getPieces()) {
      boolean  selected = pieces.getSelected().contains(piece);
      if (piece instanceof MultiPiece) {
        MultiPiece multiPiece = (MultiPiece) piece;
        for (Piece subPiece : multiPiece.getSubs()) {
          int rotX = piece.getRotatedX();
          int rotY = piece.getRotatedY();
          int pieceX = subPiece.getRotatedX();
          int pieceY = subPiece.getRotatedY();

          int deltaX = pieceX - rotX;
          int deltaY = pieceY - rotY;

          drawPiece(subPiece, piece.getPuzzleX() + deltaX, piece.getPuzzleY() + deltaY, false);
        }
        if (selected) {
          DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Color.GOLD.toRaylibColor());
        }
      } else {
        drawPiece(piece, piece.getPuzzleX(), piece.getPuzzleY(), selected);
      }
    }
    EndShaderMode();
  }

  private void drawPiece(Piece piece, int x, int y, boolean selected) {
    PieceDescription pieceDescription = pieceDescriptions.get(piece.getId());
    if (pieceDescription != null) {
      Raylib.Texture texture = getAssetManager().getAsset("pieces_" + pieceDescription.getTexture()).getAsset();
      Color color = selected ? Color.GOLD : Color.WHITE;

      Rect texturePosition = pieceDescription.getTexturePosition();
      Vec2 center = new Vec2(piece.getImageWidth() / 2.0f, piece.getImageHeight() / 2.0f);
      Vec2 centerRotated = new Vec2(piece.getCurrentWidth() / 2.0f, piece.getCurrentHeight() / 2.0f);

      DrawTexturePro(texture,
          texturePosition.toRectangle(),
          new Rect(x + centerRotated.getX(), y + centerRotated.getY(), piece.getImageWidth(), piece.getImageHeight()).toRectangle(),
          center.toVector2(),
          piece.getRotation(),
          color.toRaylibColor());

      if (selected) {
        DrawRectangleLines(piece.getPuzzleX(), piece.getPuzzleY(), piece.getCurrentWidth(), piece.getCurrentHeight(), Color.GOLD.toRaylibColor());
      }
    }

  }

}
