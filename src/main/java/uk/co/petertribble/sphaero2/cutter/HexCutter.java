package uk.co.petertribble.sphaero2.cutter;

import com.berray.math.Vec2;
import uk.co.petertribble.sphaero2.JigUtil;
import uk.co.petertribble.sphaero2.model.Piece;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HexCutter extends JigsawCutter {
  private float widthToHeightRatio = 1/1.154f;

  @Override
  public String getName() {
    return "Hexagon";
  }

  @Override
  public String getDescription() {
    return "Pieces are cut to hexagons. The borders are <b>not</b> straight but are cut to have the hex pattern. There are no knobs.";
  }

  @Override
  public Piece[] cut(BufferedImage image) {
    JigUtil.ensureLoaded(image);
    int width = image.getWidth(null);
    int height = image.getHeight(null);

    /*
     * First compute the number of rows and columns.  If N = total number
     * of pieces, R = rows, C = columns, H = height, W = width, and K =
     * width/height ratio, then
     * R * C = N
     * (W/C) / (H/R) = K
     * and therefore
     * C = N/R
     * (WRR/NH) = K
     * R = sqrt (NHK/W)
     */
    int rows = (int) Math.round(
        Math.sqrt(widthToHeightRatio * prefPieces * height / width));
    int columns = Math.round(prefPieces / rows);
    float size = 1.0f * width / columns;

    startProgress(rows * columns);

    List<List<Vec2>> points = createPoints(size, width, height);
    List<List<Hex>> hexGrid = createHexTiles(points);
    updateHexNeighbours(hexGrid);
    Map<Integer, Hex> hexMap = toMap(hexGrid);

    // cut pieces
    Map<Integer, Piece> pieces = cutPieces(image, hexMap, width, height);

    updatePieceNeighbours(pieces, hexMap);

    return pieces.values().toArray(new Piece[0]);
  }

  private void updatePieceNeighbours(Map<Integer, Piece> pieces, Map<Integer, Hex> hexMap) {
    for (Piece piece : pieces.values()) {
      Hex hex = hexMap.get(piece.getId());
      for (Hex neighbour : hex.neighbours) {
        piece.addNeighbor(pieces.get(neighbour.id));
      }
    }
  }

  private Map<Integer, Piece> cutPieces(BufferedImage image, Map<Integer, Hex> hexMap, int width, int height) {

    Map<Integer, Piece> pieces = new HashMap<>();
    for (Hex hex : hexMap.values()) {
      Piece piece = makePiece(image, hex, width, height);
      pieces.put(piece.getId(), piece);
    }

    return pieces;
  }

  private Piece makePiece(BufferedImage image, Hex hex, int tWidth, int tHeight) {
    // Roundoff (I'm guessing) will sometimes cause the path bounds to be
    // outside of the image bounds, even though that edge is a straight
    // line.  This would cause the edge pieces to appear not to line up
    // while they're being put together.  When the puzzle is finished, the
    // dissolve trick would cause the image to appear blurry due to its
    // finished version being one pixel off from the other.  Clamp all
    // sides to the image bounds.  The old PixelGrabber code didn't care,
    // but bufferedImages.getRGB() will exception if you try and read
    // outside the image.
    Rectangle box = hex.polygon.getBounds();
    if (box.x < 0) {
      box.x = 0;
    }
    if (box.y < 0) {
      box.y = 0;
    }

    int width = box.width;
    int height = box.height;

    if (box.x + width > tWidth) {
      width = tWidth - box.x;
    }
    if (box.y + height > tHeight) {
      height = tHeight - box.y;
    }

    int[] data = new int[width * height];
    data = image.getRGB(box.x, box.y, width, height, data, 0, width);

    int minX = box.x;
    int minY = box.y;
    mask(data, hex.polygon, minX, minY, width, height);

    //
    // int rotation = ((int) (Math.random() * 6)) * 60;
    int rotation = 0;

    return new Piece(hex.id, data, minX, minY, width, height,
        tWidth, tHeight, rotation);
  }

  private void mask(int[] data, Polygon polygon, int minX, int minY, int width, int height) {
    for (int j = 0; j < height; j++) {
      int pathY = minY + j;
      for (int i = 0; i < width; i++) {
        if (!polygon.contains(minX + i, pathY)) {
          data[j * width + i] = 0;
        }
      }
    }
  }

  private Map<Integer, Hex> toMap(List<List<Hex>> hexGrid) {
    Map<Integer, Hex> hexMap = new HashMap<>();
    for (List<Hex> hexRow : hexGrid) {
      for (Hex hex : hexRow) {
        hexMap.put(hex.id, hex);
      }
    }
    return hexMap;
  }

  private static void updateHexNeighbours(List<List<Hex>> hexGrid) {
    // go through all rows and find the neighbours
    for (int row = 0; row < hexGrid.size(); row++) {
      int xOffset = row % 2 == 0 ? 1 : 0;
      for (int col = 0; col < hexGrid.get(row).size(); col++) {
        List<Hex> centerRow = hexGrid.get(row);
        var hexTile = centerRow.get(col);
        // top row
        if (row - 1 >= 0) {
          List<Hex> topRow = hexGrid.get(row - 1);
          // top left
          if (col - xOffset >= 0) {
            hexTile.neighbours.add(topRow.get(col - xOffset));
          }
          // top right
          if ((col - xOffset + 1) <= topRow.size() - 1) {
            hexTile.neighbours.add(topRow.get(col - xOffset + 1));
          }
        }
        // center left
        if (col - 1 >= 0) {
          hexTile.neighbours.add(centerRow.get(col - 1));
        }
        // center right
        if (col + 1 <= centerRow.size() - 1) {
          hexTile.neighbours.add(centerRow.get(col + 1));
        }
        // bottom row
        if (row + 1 <= hexGrid.size() - 1) {
          List<Hex> bottom = hexGrid.get(row + 1);
          // bottom left
          if (col - xOffset >= 0) {
            hexTile.neighbours.add(bottom.get(col - xOffset));
          }
          // top right
          if ((col - xOffset + 1) <= bottom.size() - 1) {
            hexTile.neighbours.add(bottom.get(col - xOffset + 1));
          }
        }
      }
    }
  }

  private static List<List<Hex>> createHexTiles(List<List<Vec2>> points) {
    int hexIndex = 0;
    List<List<Hex>> hexGrid = new ArrayList<>();
    // there are 1 rows of tiles less than rows of points (as each row has an upper and a lower row of points)
    for (int row = 0; row < points.size() - 1; row++) {
      List<Vec2> upperRow = points.get(row);
      List<Vec2> lowerRow = points.get(row + 1);
      List<Hex> hexRow = new ArrayList<>();
      // start the odd rows at the 2st point (index 1 instead of 0)
      for (int x = row % 2; x < upperRow.size() - 2; x += 2) {
        // get 3 points from the upper row and 3 points from the lower row
        List<Vec2> hexPoints = new ArrayList<>();
        hexPoints.add(upperRow.get(x));
        hexPoints.add(upperRow.get(x + 1));
        hexPoints.add(upperRow.get(x + 2));
        hexPoints.add(lowerRow.get(x + 2));
        hexPoints.add(lowerRow.get(x + 1));
        hexPoints.add(lowerRow.get(x));
        hexRow.add(new Hex(hexIndex++, hexPoints));
      }
      hexGrid.add(hexRow);
    }
    return hexGrid;
  }

  private static List<List<Vec2>> createPoints(float size, int width, int height) {
    List<List<Vec2>> points = new ArrayList<>();

    float deltaX = (float) (Math.cos(Math.toRadians(30)) * size);
    float deltaY = (float) (Math.cos(Math.toRadians(60)) * size);

    float[] yRowDelta = new float[]{deltaY, 0};

    for (int yRow = 0; yRow * (size + deltaY) < height; yRow++) {
      List<Vec2> row = new ArrayList<>();
      float y = yRow * (size + deltaY);
      int yOffsetIndex = yRow % 2;
      row.add(new Vec2(0, y + yRowDelta[yOffsetIndex++ % 2]));
      for (int x = 0; x < width - deltaX; x += deltaX) {
        row.add(new Vec2(x + deltaX, y + yRowDelta[yOffsetIndex++ % 2]));
      }
      points.add(row);
    }
    return points;
  }

  private static class Hex {
    public final int id;
    public final List<Vec2> points;
    public final List<Hex> neighbours = new ArrayList<>();
    private final Polygon polygon;

    public Hex(int id, List<Vec2> points) {
      this.id = id;
      this.points = points;
      this.polygon = new Polygon();
      for (Vec2 point : points) {
        polygon.addPoint((int) point.getX(), (int) point.getY());
      }
    }
  }

}
