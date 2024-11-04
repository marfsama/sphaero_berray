package uk.co.petertribble.sphaero2.gui;

import com.berray.BerrayApplication;
import com.berray.GameObject;
import com.berray.assets.CoreAssetShortcuts;
import com.berray.components.CoreComponentShortcuts;
import com.berray.components.core.AnchorType;
import com.berray.event.CoreEvents;
import com.berray.math.Color;
import com.berray.math.Rect;
import com.berray.math.Vec2;
import com.berray.math.Vec3;
import com.raylib.Raylib;
import org.bytedeco.javacpp.FloatPointer;
import uk.co.petertribble.sphaero2.JigUtil;
import uk.co.petertribble.sphaero2.model.Jigsaw;
import uk.co.petertribble.sphaero2.model.JigsawParam;
import uk.co.petertribble.sphaero2.model.Piece;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.berray.objects.core.Label.label;
import static com.berray.objects.gui.panel.PanelBuilder.makePanel;
import static com.berray.objects.gui.panel.RowBuilder.makeRow;
import static com.raylib.Raylib.*;


public class JigsawApplication extends BerrayApplication implements CoreAssetShortcuts, CoreComponentShortcuts {

  private HashMap<Integer, PieceDescription> pieceDescriptions;

  @Override
  public void game() {
    String imagePath = "<full-path-to-image-jpg-or-png>";

    BufferedImage sourceImage = getImage(imagePath);
    JigsawParam params = new JigsawParam();
    params.setFilename(new File(imagePath));
    params.setPieces(20);
    Jigsaw jigsaw = new Jigsaw(params, sourceImage);
    System.out.println("cutting...");
    jigsaw.reset(true, width(), height());

    System.out.println("creating pieces textures...");
    List<BufferedImage> textures = createTextures(jigsaw.getPieces().getPieces());

    BufferedImage previewImage = JigUtil.resizeImage(sourceImage, 150, sourceImage.getHeight());
    System.out.println("preview image " + previewImage.getWidth() + " x " + previewImage.getHeight());

    System.out.println("uploading pieces textures...");
    for (int index = 0; index < textures.size(); index++) {
      loadSprite("pieces_" + index, textures.get(index));
    }

    loadSprite("preview", previewImage);

    Raylib.Shader shaderOutline = LoadShader(null, "/home/mato/project/games/sphaero2/src/main/resources/outline2.fs");
    float outlineSize[] = {1.0f};
    float outlineColor[] = {1.0f, 0.8f, 0.0f, 1.0f};     // Normalized RED color
    float textureSize[] = {1024, 1024};

    // Get shader locations
    int outlineSizeLoc = GetShaderLocation(shaderOutline, "outlineSize");
    int outlineColorLoc = GetShaderLocation(shaderOutline, "outlineColor");
    int textureSizeLoc = GetShaderLocation(shaderOutline, "textureSize");

    // Set shader values (they can be changed later)
    SetShaderValue(shaderOutline, outlineSizeLoc, new FloatPointer(FloatBuffer.wrap(outlineSize)), SHADER_UNIFORM_FLOAT);
    SetShaderValue(shaderOutline, outlineColorLoc, new FloatPointer(FloatBuffer.wrap(outlineColor)), SHADER_UNIFORM_VEC4);
    SetShaderValue(shaderOutline, textureSizeLoc, new FloatPointer(FloatBuffer.wrap(textureSize)), SHADER_UNIFORM_VEC2);


    var root = add(
        pos(0, 0),
        anchor(AnchorType.TOP_LEFT)
    );

    ShaderNode shaderNode = root.add(
        new ShaderNode(shaderOutline),
        pos(0, 0),
        anchor(AnchorType.TOP_LEFT)
    );

    GameObject piecesNode = shaderNode.add(
        new PiecesComponent(jigsaw.getPieces(), pieceDescriptions),
        pos(0, 0),
        scale(1.0f),
        area(),
        anchor(AnchorType.TOP_LEFT),
        mouse()
    );
    float initialScale = Math.min((float) width() / sourceImage.getWidth(), (float) height() / sourceImage.getHeight());
    if (initialScale > 1.0f) {
      initialScale = 1.0f;
    }
    piecesNode.set("scaleFactor", (int) (initialScale * 10));
    piecesNode.add(
        new PiecesDrawComponent(jigsaw.getPieces(), pieceDescriptions)
    );
    GameObject selectionRectangle = piecesNode.add(
        rect(0f ,0),
        pos(0,0),
        anchor(AnchorType.TOP_LEFT),
        color(new Color(173, 216, 230, 128)), // light blue
        "selectionRectangle"
    );
    selectionRectangle.setPaused(true);


    root.add(
        label(() ->
            "# Pieces: " + jigsaw.getPieces().getPieces().size() + "\n" +
                "# Textures: " + textures.size() + "\n" +
                "# mouse pos: " + piecesNode.get("mousePos", null) + "\n" +
                "# scale: " + piecesNode.get("scaleFactor", null) + "\n"+
                "# pos: " + piecesNode.get("pos", null) + "\n"+
                "# rect: " + jigsaw.getPieces().getRect() + "\n"
        ),
        pos(0, 0),
        anchor(AnchorType.TOP_LEFT),
        color(Color.GOLD)
    );

    GameObject preview = add(
        makePanel()
            .title("Preview")
            .fontSize(20)
            .movable(true)
            .minimizable(true)
            .columnWidths(150.0f)
            .color(Color.BLACK, Color.GOLD)
            .frame(5, Color.WHITE)
            .row(makeRow(previewImage.getHeight())
                .align(AnchorType.TOP_LEFT)
                .add(
                    sprite("preview"),
                    pos(0, 0)
                )
            )
            .buildGameObject()
    );
    preview.set("pos", new Vec2(100, 100));
    preview.set("anchor", AnchorType.TOP_LEFT);

    GameObject overview = make(
        new PiecesDrawComponent(jigsaw.getPieces(), pieceDescriptions),
        scale(1.0f, 1.0f, 1.0f)
    );

    overview.add(
        rect(width(), height()).fill(false).lineThickness(10),
        pos(0,0),
        color(Color.WHITE),
        anchor(AnchorType.TOP_LEFT),
        "marker"
    );

    GameObject overviewPanel = add(
        makePanel()
            .title("Overview")
            .fontSize(20)
            .movable(true)
            .minimizable(true)
            .columnWidths(150.0f)
            .color(Color.BLACK, Color.GOLD)
            .frame(5, Color.WHITE)
            .row(makeRow(150.0f)
                .align(AnchorType.TOP_LEFT)
                .add(overview)
            )
            .buildGameObject()
    );
    overviewPanel.set("pos", new Vec2(500, 100));
    overviewPanel.set("anchor", AnchorType.TOP_LEFT);
    overviewPanel.on(CoreEvents.UPDATE, e -> {
      var rect = jigsaw.getPieces().getRect();
      float scaleX = 150.0f / rect.getWidth();
      float scaleY = 150.0f / rect.getHeight();
      float scale = Math.min(scaleX, scaleY);
      overview.set("scale", new Vec3(scale, scale, scale));
      overview.set("pos", new Vec2(-rect.getX() * scale, -rect.getY() * scaleX));

      GameObject marker = overview.getChildren("marker").get(0);
      Vec3 piecesScale = piecesNode.get("scale");
      Vec2 piecesPos = piecesNode.get("pos");
      marker.set("lineThickness", 1.0f / scale);
      marker.set("pos", new Vec2(-piecesPos.getX() / piecesScale.getX(), -piecesPos.getY() / piecesScale.getY()));
      marker.set("size", new Vec2(width() / piecesScale.getX(), height() / piecesScale.getY()));
    });
  }

  private List<BufferedImage> createTextures(List<Piece> originalPieces) {
    if (originalPieces.isEmpty()) {
      throw new IllegalStateException("Jigsaw has no pieces. Did you forget to cut it?");
    }
    // sort pieces by width
    List<Piece> pieces = new ArrayList<>(originalPieces);
    pieces.sort(Comparator.comparingInt(Piece::getImageWidth)
        .thenComparing(Piece::getImageHeight));

    int textureSize = 1024;

    List<BufferedImage> textures = new ArrayList<>();
    this.pieceDescriptions = new HashMap<>();

    int columnWidth = 0;
    int currentY = 5;
    int currentX = 5;
    BufferedImage texture = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
    Graphics graphics = texture.createGraphics();

    for (Piece piece : pieces) {
      int width = piece.getImageWidth();
      int height = piece.getImageHeight();
      // check if the current column is full.
      if (currentY + height > textureSize-5) {
        // yes. start next column
        currentY = 5;
        currentX += columnWidth+5;
        columnWidth = width;
      }
      // check if current image is full
      if (currentX + width > textureSize-5) {
        // yes. start new image
        graphics.dispose();
        textures.add(texture);
        texture = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
        graphics = texture.createGraphics();
        currentX = 0;
      }

      graphics.drawImage(piece.getOriginalImage(), currentX, currentY, null);
      pieceDescriptions.put(piece.getId(), new PieceDescription(piece.getId(), textures.size(), new Rect(currentX, currentY, width, height)));
      currentY += height;
      columnWidth = Math.max(columnWidth, width);
    }
    // close last texture
    graphics.dispose();
    textures.add(texture);

    return textures;
  }

  private static BufferedImage getImage(String filename) {
    try {
      return ImageIO.read(new File(filename));
    } catch (IOException e) {
      throw new IllegalStateException("cannot load image " + filename, e);
    }
  }

  public static BufferedImage toBufferedImage(Image img) {
    if (img instanceof BufferedImage) {
      return (BufferedImage) img;
    }

    // Create a buffered image with transparency
    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

    // Draw the image on to the buffered image
    Graphics2D bGr = bimage.createGraphics();
    bGr.drawImage(img, 0, 0, null);
    bGr.dispose();

    // Return the buffered image
    return bimage;
  }


  @Override
  public void initWindow() {
    width(2000);
    height(1200);
    background(Color.GRAY);
    title("Sphaero Test");
  }

  public static void main(String[] args) {
    new JigsawApplication().runGame();
  }


}
