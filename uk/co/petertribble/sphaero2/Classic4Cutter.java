package uk.co.petertribble.sphaero2;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.image.PixelGrabber;

// ### Pieces are a bit "prickly" in appearance, particularly if they're
//   small.

/**
 * Cuts pieces with each side being equally likely of having the knob
 * facing north, south, east or west.  Each piece subsequently may have
 * 0-4 holes, and will mostly likely have 2. Pieces are all roughly square,
 * not counting the knobs and holes.
 */
public class Classic4Cutter extends JigsawCutter {

    @Override
    public String getName() {
	return "Classic-4";
    }

    @Override
    public String getDescription() {
	return
	    "Creates pieces where each side is equally likely to have a hole or"
	    +" a knob; each piece may have 0-4 holes, and will tend to have 2. "
	    +" Each piece is roughly square, not counting holes and knobs."; }

    /**
     * Creates a Classic4Cutter that will try to produce a default number of
     * pieces.
     */
    public Classic4Cutter() {
	this(DEFAULT_PIECES);
    }

    /**
     * Creates a Classic4Cutter.
     * @param prefPieces the preferred number of pieces; the cutter will try
     * to produce close to this many
     * @throws IllegalArgumentException if prefPieces is less than 2
     */
    public Classic4Cutter(int prefPieces) {
	setPreferredPieceCount(prefPieces);
    }

    @Override
    public Piece[] cut(Image image) {
	JigUtil.ensureLoaded(image);
	int width = image.getWidth(null);
	int height = image.getHeight(null);

	/*
	 * First compute the number of rows and columns.  If N = total number
	 * of pieces, R = rows, C = columns, H = height, W = width
	 * R * C = N
	 * (W/C) = (H/R)
	 * and therefore
	 * C = N/R
	 * (WR/N) = (H/R)
	 * R = sqrt (NH/W)
	 */

	int rows = (int) Math.round(Math.sqrt(prefPieces * height / width));
	int columns = (int) Math.round(prefPieces / rows);

	jp.setMaximum(rows*columns);
	jp.setValue(iprogress);

	// Make a matrix of points representing the corners of the pieces.
	// Each point is based on a grid of equal rectangles, and can then
	// drift by up to 1/20th the height or width of an average piece.
	// Points on the north and south edges are fixed vertically, of
	// course, and east/west edge points are fixed horizontally.
	int hVary = height / (rows * 20);
	int wVary = width / (columns * 20);
	Point[][] points = new Point[columns+1][rows+1];
	// i varies horizontally; j varies vertically
	for (int j = 0; j <= rows; j++) {
	    int baseY = j*height / rows;
	    for (int i = 0; i <= columns; i++) {
		// int baseX = i*width / columns;
		int x = i*width / columns;
		int y = baseY;
		if ((i > 0) && (i < columns)) {
		    x += Math.random()*(2*wVary+1) - wVary;
		}
		if ((j > 0) && (j < rows)) {
		    y += Math.random()*(2*hVary+1) - hVary;
		}
		points[i][j] = new Point(x, y);
	    }
	}

	// Make a knob for each edge.  Two matrices, one for vertical edges,
	// one for horizontal.  Remember to alternate knob directions.
	// boolean flip1 = true;
	Knob[][] vKnobs = new Knob[columns-1][rows];
	for (int j = 0; j < rows; j++) {
	    // boolean flip = flip1;
	    for (int i = 0; i < columns-1; i++) {
		Point p1 = points[i+1][j];
		Point p2 = points[i+1][j+1];
		boolean flip = (Math.random() >= 0.5);
		if (flip) { Point temp = p1; p1 = p2; p2 = temp; }
		vKnobs[i][j] = new Knob(p1.x, p1.y, p2.x, p2.y);
		// flip = !flip;
	    }
	    // flip1 = !flip1;
	}

	// flip1 = true;
	Knob[][] hKnobs = new Knob[columns][rows-1];
	for (int j = 0; j < rows-1; j++) {
	    // boolean flip = flip1;
	    for (int i = 0; i < columns; i++) {
		Point p1 = points[i][j+1];
		Point p2 = points[i+1][j+1];
		boolean flip = (Math.random() >= 0.5);
		if (flip) { Point temp = p1; p1 = p2; p2 = temp; }
		hKnobs[i][j] = new Knob(p1.x, p1.y, p2.x, p2.y);
		// flip = !flip;
	    }
	    // flip1 = !flip1;
	}

	// Create the pieces.
	Piece[][] pieces = new Piece[columns][rows];
	for (int j = 0; j < rows; j++) {
	    for (int i = 0; i < columns; i++) {
		Knob knobN = (j > 0) ? hKnobs[i][j-1] : null;
		Knob knobS = (j < rows-1) ? hKnobs[i][j] : null;
		Knob knobW = (i > 0) ? vKnobs[i-1][j] : null;
		Knob knobE = (i < columns-1) ? vKnobs[i][j] : null;
		pieces[i][j] = makePiece(image,
					points[i][j],
					points[i][j+1],
					points[i+1][j],
					points[i+1][j+1],
					knobN, knobE, knobS, knobW,
					width, height);
		iprogress++;
		jp.setValue(iprogress);
	    }
	}

	// Set each piece's neighbors, and build the final array.
	return finalBuild(pieces, rows, columns);
    }

    private Piece makePiece(Image image,
			    Point nw, Point sw, Point ne, Point se,
			    Knob knobN, Knob knobE, Knob knobS, Knob knobW,
			    int tWidth, int tHeight) {
	// Build a path out of the knobs/puzzle edges.
	GeneralPath path = new GeneralPath();
	path.moveTo(nw.x, nw.y);
	if (knobN == null) {
	    path.lineTo(ne.x, ne.y);
	} else {
	    path.append(knobN.getCurvePath(nw.x, nw.y), true);
	}
	if (knobE == null) {
	    path.lineTo(se.x, se.y);
	} else {
	    path.append(knobE.getCurvePath(ne.x, ne.y), true);
	}
	if (knobS == null) {
	    path.lineTo(sw.x, sw.y);
	} else {
	    path.append(knobS.getCurvePath(se.x, se.y), true);
	}
	if (knobW == null) {
	    path.lineTo(nw.x, nw.y);
	} else {
	    path.append(knobW.getCurvePath(sw.x, sw.y), true);
	}

	// Roundoff (I'm guessing) will sometimes cause the path bounds to be
	// outside of the image bounds, even though that edge is a straight
	// line.  This would cause the edge pieces to appear not to line up
	// while they're being put together.  When the puzzle is finished, the
	// dissolve trick would cause the image to appear blurry due to its
	// finished version being one pixel off from the other.  I'm fixing the
	// roundoff problem for the top and left edge pieces, and hoping the
	// other pieces don't need any help.
	Rectangle box = path.getBounds();
	// if (box.x < 0) box.x = 0;
	// if (box.y < 0) box.y = 0;

	int width  = box.width;
	int height = box.height;

	int[] data = new int[width*height];
	PixelGrabber grabber =
	    new PixelGrabber(image, box.x, box.y,
			width, height, data, 0, width);
	try { grabber.grabPixels(); }
	catch (InterruptedException ex) {
	    System.out.println("interrupted while grabbing");
	}

	int minX = box.x;
	int minY = box.y;
	mask(data, path, minX, minY, width, height);

	int rotation = ((int)(Math.random()*4)) * 90;

	return new Piece(data, minX, minY, width, height,
			tWidth, tHeight, rotation);
    }

    private void mask(int[] data, GeneralPath path,
			int minX, int minY, int width, int height) {
	for (int j = 0; j < height; j++) {
	    int pathY = minY + j;
	    for (int i = 0; i < width; i++) {
		// int pathX = minX + i;
		if (!path.contains(minX + i, pathY)) {
		    data[j*width+i] = 0;
		}
	    }
	}
    }
}
