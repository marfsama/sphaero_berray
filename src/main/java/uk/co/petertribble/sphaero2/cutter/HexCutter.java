package uk.co.petertribble.sphaero2.cutter;

import com.berray.math.Vec2;
import uk.co.petertribble.sphaero2.JigUtil;
import uk.co.petertribble.sphaero2.model.Knob;
import uk.co.petertribble.sphaero2.model.Piece;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class HexCutter extends JigsawCutter {
  private float widthToHeightRatio = 1.154f;

  @Override
  public String getName() {
    return "Hexagon";
  }

  @Override
  public String getDescription() {
    return "Pieces are cut to hexagons. The borders are <b>not</b> straight but are cut to have the hex pattern.";
  }

  @Override
  public Piece[] cut(BufferedImage image) {
    JigUtil.ensureLoaded(image);
    int width = image.getWidth(null);
    int height = image.getHeight(null);

    /*
     * First compute the number of rows and columns.  If N = total number
     * of pieces, R = rows, C = columns, H = height, W = width, and K =
     * width/height ratio of piece, then
     * R * C = N
     * (W/C) / (H/R) = K
     * (W/C) * (R/H) = K
     * (WR/HC) = K
     * and therefore
     * C = N/R
     * (WRR/NH) = K
     * R = sqrt (NHK/W)
     */
    int rows = (int) Math.round(
        Math.sqrt((widthToHeightRatio * prefPieces * height) / width)/widthToHeightRatio);
    int columns = Math.round(prefPieces / rows);
    float size = 1.0f * width / columns;

    int s = 10;
    int p = 0;
    do {
      s++;
      int c = (int) (width / (Math.cos(Math.toRadians(30)) * s * 2));
      int r = (int) (height / (Math.cos(Math.toRadians(60)) * s + s));
      p = c * r;
    } while ( p > prefPieces);
    size = s;


    List<List<Point>> points = createPoints(size, width, height);
    Map<EdgeKey, Edge> edges = createEdges(points);

    List<List<Hex>> hexGrid = createHexTiles(points);
    updateHexNeighbours(hexGrid);
    Map<Integer, Hex> hexMap = toMap(hexGrid);
    // cut pieces
    startProgress("cutting", rows * columns);
    Map<Integer, Piece> pieces = cutPieces(image, hexMap, edges, width, height);

    updatePieceNeighbours(pieces, hexMap);

    return pieces.values().toArray(new Piece[0]);
  }

  private void updatePieceNeighbours(Map<Integer, Piece> pieces, Map<Integer, Hex> hexMap) {
    for (Piece piece : pieces.values()) {
      Hex hex = hexMap.get(piece.getId());
      for (Hex neighbour : hex.neighbours) {
        if (neighbour != null) {
          piece.addNeighbor(pieces.get(neighbour.id));
        }
      }
    }
  }

  private Map<Integer, Piece> cutPieces(BufferedImage image, Map<Integer, Hex> hexMap, Map<EdgeKey, Edge> edges, int width, int height) {

    Map<Integer, Piece> pieces = new HashMap<>();
    for (Hex hex : hexMap.values()) {

      Path2D path = new Path2D.Float(Path2D.WIND_NON_ZERO);
      path.moveTo(hex.points.get(0).pos.getX(), hex.points.get(0).pos.getY());
      for (int i = 0; i < hex.points.size(); i++) {
        Point p1 = hex.points.get(i);
        Point p2 = hex.points.get((i+1) % hex.points.size());
        int startIndex = p1.index < p2.index ? p1.index : p2.index;
        int endIndex = p1.index < p2.index ? p2.index : p1.index;
        Edge edge = edges.get(new EdgeKey(startIndex, endIndex));
        if (edge == null) {
          throw new IllegalStateException("missing edge for points "+p1+" and "+p2);
        }
        if (edge.knob != null) {
          path.append(edge.knob.getCurvePath((int) p1.pos.getX(), (int) p1.pos.getY()), true);
        }
        else {
          path.lineTo(p2.pos.getX(), p2.pos.getY());
        }
      }
      //path.closePath();

      Piece piece = makePiece(image, hex, path, width, height);
      pieces.put(piece.getId(), piece);
      updateProgress();
      statusListener.ejectPiece(piece);
    }

    return pieces;
  }

  private Piece makePiece(BufferedImage image, Hex hex, Path2D path, int tWidth, int tHeight) {
    // Roundoff (I'm guessing) will sometimes cause the path bounds to be
    // outside of the image bounds, even though that edge is a straight
    // line.  This would cause the edge pieces to appear not to line up
    // while they're being put together.  When the puzzle is finished, the
    // dissolve trick would cause the image to appear blurry due to its
    // finished version being one pixel off from the other.  Clamp all
    // sides to the image bounds.  The old PixelGrabber code didn't care,
    // but bufferedImages.getRGB() will exception if you try and read
    // outside the image.
    Rectangle box = path.getBounds();
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
    mask(data, path, minX, minY, width, height);

    //
    // int rotation = ((int) (Math.random() * 6)) * 60;
    int rotation = 0;

    return new Piece(hex.id, data, minX, minY, width, height,
        tWidth, tHeight, rotation);
  }

  private void mask(int[] data, Path2D path, int minX, int minY, int width, int height) {
    for (int j = 0; j < height; j++) {
      int pathY = minY + j;
      for (int i = 0; i < width; i++) {
        if (!path.contains(minX + i, pathY)) {
          data[j * width + i] = 0;
        }
      }
    }
  }

  private Map<EdgeKey, Edge> createEdges(List<List<Point>> points) {
    Map<EdgeKey, Edge> edges = new HashMap<>();

    for (int rowIndex = 0; rowIndex < points.size(); rowIndex++) {
      List<Point> pointRow = points.get(rowIndex);
      int horizontalOffset = rowIndex % 2;
      // don't add knob to first or last rows
      boolean firstOrLastRow = rowIndex == 0 || rowIndex > points.size()-2;
      for (int col = 0; col < pointRow.size() - 1; col++) {
        boolean firstOrLastColumn = col == 0 || col >= pointRow.size()-2;
        // create horizontal edge
        Edge edge = createEdge(pointRow.get(col), pointRow.get(col + 1), !(firstOrLastRow || firstOrLastColumn));
        edges.put(edge.getKey(), edge);
        // do we need to add a horizontal edge?
        if (rowIndex + 1 < points.size() && col % 2 == horizontalOffset) {
          boolean firstHorizontalEdge = col / 2 == 0;
          boolean lastHorizontalEdge = col >= pointRow.size()-2;
          Edge horizontalEdge = createEdge(pointRow.get(col), points.get(rowIndex + 1).get(col), !(firstHorizontalEdge || lastHorizontalEdge));
          edges.put(horizontalEdge.getKey(), horizontalEdge);
        }
      }
      // add horizontal edge to piece if needed to.
      if (rowIndex + 1 < points.size() && (pointRow.size()-1) % 2 == horizontalOffset) {
        Edge horizontalEdge = createEdge(pointRow.get(pointRow.size()-1), points.get(rowIndex + 1).get(pointRow.size()-1), false);
        edges.put(horizontalEdge.getKey(), horizontalEdge);
      }

    }
    return edges;
  }

  private static Edge createEdge(Point start, Point end, boolean addKnob) {
    Edge edge = new Edge(start, end);

    if (addKnob) {
      boolean flip = Math.random() >= 0.5;
      Vec2 startVec = flip ? end.pos : start.pos;
      Vec2 endVec = flip ? start.pos : end.pos;

      edge.knob = new Knob((int) startVec.getX(), (int) startVec.getY(), (int) endVec.getX(), (int) endVec.getY());
    }

    return edge;
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
            hexTile.neighbours[0] = topRow.get(col - xOffset);
          }
          // top right
          if ((col - xOffset + 1) <= topRow.size() - 1) {
            hexTile.neighbours[1] = topRow.get(col - xOffset + 1);
          }
        }
        // center left
        if (col - 1 >= 0) {
          hexTile.neighbours[2] = centerRow.get(col - 1);
        }
        // center right
        if (col + 1 <= centerRow.size() - 1) {
          hexTile.neighbours[3] = centerRow.get(col + 1);
        }
        // bottom row
        if (row + 1 <= hexGrid.size() - 1) {
          List<Hex> bottom = hexGrid.get(row + 1);
          // bottom left
          if (col - xOffset >= 0) {
            hexTile.neighbours[4] = bottom.get(col - xOffset);
          }
          // top right
          if ((col - xOffset + 1) <= bottom.size() - 1) {
            hexTile.neighbours[5] = bottom.get(col - xOffset + 1);
          }
        }
      }
    }
  }

  private static List<List<Hex>> createHexTiles(List<List<Point>> points) {
    int hexIndex = 0;
    List<List<Hex>> hexGrid = new ArrayList<>();
    // there are 1 rows of tiles less than rows of points (as each row has an upper and a lower row of points)
    for (int row = 0; row < points.size() - 1; row++) {
      List<Point> upperRow = points.get(row);
      List<Point> lowerRow = points.get(row + 1);
      List<Hex> hexRow = new ArrayList<>();
      // start the odd rows at the 2nd point (index 1 instead of 0)
      for (int x = row % 2; x < upperRow.size() - 2; x += 2) {
        // get 3 points from the upper row and 3 points from the lower row, in clockwise order
        List<Point> hexPoints = new ArrayList<>();
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


  private static List<List<Point>> createPoints(float size, int width, int height) {
    List<List<Point>> points = new ArrayList<>();

    float deltaX = (float) (Math.cos(Math.toRadians(30)) * size);
    float deltaY = (float) (Math.cos(Math.toRadians(60)) * size);
    int pointIndex = 0;

    float[] yRowDelta = new float[]{deltaY, 0};

    for (int yRow = 0; yRow * (size + deltaY) < height; yRow++) {
      List<Point> row = new ArrayList<>();
      float y = yRow * (size + deltaY);
      int yOffsetIndex = yRow % 2;
      row.add(new Point(pointIndex++, new Vec2(0, y + yRowDelta[yOffsetIndex++ % 2])));
      for (int x = 0; x < width - deltaX; x += deltaX) {
        row.add(new Point(pointIndex++, new Vec2(x + deltaX, y + yRowDelta[yOffsetIndex++ % 2])));
      }
      points.add(row);
    }
    return points;
  }

  private static class Point {
    int index;
    Vec2 pos;

    public Point(int index, Vec2 pos) {
      this.index = index;
      this.pos = pos;
    }

    @Override
    public String toString() {
      return "Point{" +
          "index=" + index +
          ", pos=" + pos +
          '}';
    }
  }

  private static class Hex {
    public final int id;
    public final List<Point> points;
    public final Hex[] neighbours = new Hex[6];
    private final Polygon polygon;

    public Hex(int id, List<Point> points) {
      this.id = id;
      this.points = points;
      this.polygon = new Polygon();
      for (Point point : points) {
        polygon.addPoint((int) point.pos.getX(), (int) point.pos.getY());
      }
    }
  }

  public static class EdgeKey {
    int startPoint;
    int endPoint;

    public EdgeKey(int startPoint, int endPoint) {
      this.startPoint = startPoint;
      this.endPoint = endPoint;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      EdgeKey edgeKey = (EdgeKey) o;
      return startPoint == edgeKey.startPoint && endPoint == edgeKey.endPoint;
    }

    @Override
    public int hashCode() {
      return Objects.hash(startPoint, endPoint);
    }
  }

  public static class Edge {
    Point start;
    Point end;

    Knob knob;

    public Edge(Point start, Point end) {
      // points are sorted by index
      if (start.index < end.index) {
        this.start = start;
        this.end = end;
      } else {
        this.start = end;
        this.end = start;
      }
    }

    public EdgeKey getKey() {
      return new EdgeKey(start.index, end.index);
    }
  }

}
