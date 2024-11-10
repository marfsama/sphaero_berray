package uk.co.petertribble.sphaero2.gui;

import com.berray.BerrayApplication;
import com.berray.GameObject;
import com.berray.assets.CoreAssetShortcuts;
import com.berray.components.CoreComponentShortcuts;
import com.berray.components.core.AnchorType;
import com.berray.components.core.Component;
import com.berray.event.CoreEvents;
import com.berray.event.MouseEvent;
import com.berray.math.Color;
import com.berray.math.Vec2;
import com.raylib.Raylib;
import uk.co.petertribble.sphaero2.model.Knob;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.List;
import java.util.*;

public class HexTest extends BerrayApplication implements CoreComponentShortcuts, CoreAssetShortcuts {
  private Map<EdgeKey, Edge> edges;

  @Override
  public void game() {
    String imagePath = "jigsaw/portrait_of_aloy_by_gordon87_dgtr6jh.png";

    loadSprite("image", imagePath);

    var image = add(
        sprite("image"),
        pos(center()),
        anchor(AnchorType.CENTER)
    );

    Map<Integer, Hex> pieces = cut(50, 1024, 1024);
    image.add(
        pos(0, 0),
        new HexComponent(pieces, edges, 1024, 1024),
        area(),
        mouse()
    );
  }

  public Map<Integer, Hex> cut(int size, int width, int height) {
    List<List<Point>> points = createPoints(size, width, height);
    Map<EdgeKey, Edge> edges = createEdges(points);

    this.edges = edges;

    List<List<Hex>> hexGrid = createHexTiles(points);
    updateHexNeighbours(hexGrid);
    Map<Integer, Hex> hexMap = toMap(hexGrid);


    return hexMap;
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

      Knob knob = new Knob((int) startVec.getX(), (int) startVec.getY(), (int) endVec.getX(), (int) endVec.getY());

      edge.path = (Path2D.Float) knob.getCurvePath((int) startVec.getX(), (int) startVec.getY());
    }
    else {
      // no knob, straight edge
      edge.path.moveTo(start.pos.getX(), start.pos.getY());
      edge.path.lineTo(end.pos.getX(),end.pos.getY());
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
        // get 3 points from the upper row and 3 points from the lower row
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


  @Override
  public void initWindow() {
    width(2000);
    height(1200);
    background(Color.GRAY);
    title("Hexcutting Test");
    targetFps = -1;
  }

  public static void main(String[] args) {
    new HexTest().runGame();
  }

  public static class HexComponent extends Component {

    private final Map<Integer, Hex> hexGrid;
    private final int width;
    private final int height;

    private Map<EdgeKey, Edge> edges;

    private Hex selectedHex = null;

    public HexComponent(Map<Integer, Hex> hexGrid, Map<EdgeKey, Edge> edges, int width, int height) {
      super("hex");
      this.hexGrid = hexGrid;
      this.width = width;
      this.height = height;
      this.edges = edges;
    }

    @Override
    public void add(GameObject gameObject) {
      super.add(gameObject);
      registerGetter("render", () -> true);
      registerGetter("size", () -> new Vec2(width, height));

      on(CoreEvents.HOVER, this::onMouseMove);
    }

    private void onMouseMove(MouseEvent e) {
      // get the polygon under the mouse
      Vec2 pos = e.getGameObjectPos();
      for (Hex hex : hexGrid.values()) {
        if (hex.polygon.contains(pos.getX(), pos.getY())) {
          this.selectedHex = hex;
          e.setProcessed();
          return;
        }
      }
    }

    @Override
    public void draw() {

      for (Hex hex : hexGrid.values()) {
        drawPoints(hex.points, Color.GOLD, true);
      }

      for (Edge edge : this.edges.values()) {
        drawEdge(edge);
      }

      var hexTile = selectedHex;

      if (hexTile != null) {
        for (Hex neighbour : hexTile.neighbours) {
          if (neighbour != null) {
            drawPoints(neighbour.points, Color.GREEN, true);
          }
        }
        drawPoints(hexTile.points, Color.RED, true);
      }

    }

    private void drawEdge(Edge edge) {
      PathIterator iterator = edge.path.getPathIterator(null);
      float[] values = new float[6];
      Vec2 lastPoint = null;
      while (!iterator.isDone()) {
        var type = iterator.currentSegment(values);
        switch (type) {
          case PathIterator.SEG_MOVETO: {
            lastPoint = new Vec2(values[0], values[1]);
            break;
          }
          case PathIterator.SEG_LINETO: {
            Vec2 newPoint = new Vec2(values[0], values[1]);
            if (lastPoint != null) {
              Raylib.DrawLineEx(lastPoint.toVector2(), newPoint.toVector2(), 2.0f, Color.BLACK.toRaylibColor());
            }
            lastPoint = newPoint;
            break;
          }
          case PathIterator.SEG_QUADTO: {
            Vec2 p1 = new Vec2(values[0], values[1]);
            Vec2 p2 = new Vec2(values[2], values[3]);
            if (lastPoint != null) {
              Raylib.DrawSplineSegmentBezierQuadratic(lastPoint.toVector2(), p1.toVector2(), p2.toVector2(), 2.0f, Color.BLACK.toRaylibColor());
            }
            lastPoint = p2;
            break;
          }
          case PathIterator.SEG_CUBICTO: {
            Vec2 p1 = new Vec2(values[0], values[1]);
            Vec2 p2 = new Vec2(values[2], values[3]);
            Vec2 p3 = new Vec2(values[4], values[5]);
            if (lastPoint != null) {
              Raylib.DrawSplineSegmentBezierCubic(lastPoint.toVector2(), p1.toVector2(), p2.toVector2(), p3.toVector2(), 2.0f, Color.BLACK.toRaylibColor());
            }
            lastPoint = p3;
            break;
          }
          case PathIterator.SEG_CLOSE:
            // nothing
            break;
        }

        iterator.next();
      }
    }

    private static void drawPoints(List<Point> row, Color color, boolean join) {
      for (int i = 0; i < row.size() - (join ? 0 : 1); i++) {
        var p = row.get(i);
        var p2 = row.get((i + 1) % row.size());
        Raylib.DrawLineEx(p.pos.toVector2(), p2.pos.toVector2(), 3.0f, color.toRaylibColor());
      }
    }
  }

  private static class Point {
    int index;
    Vec2 pos;

    public Point(int index, Vec2 pos) {
      this.index = index;
      this.pos = pos;
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

    Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);

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
