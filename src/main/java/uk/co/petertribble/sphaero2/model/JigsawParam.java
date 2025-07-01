package uk.co.petertribble.sphaero2.model;

import uk.co.petertribble.sphaero2.cutter.JigsawCutter;

import java.awt.*;
import java.io.File;

public class JigsawParam {
  private JigsawCutter cutter;
  private int pieces;
  private File filename;
  private Rectangle rectangle;

  public JigsawParam() {
    // default parameter
    cutter = JigsawCutter.cutters[0];
    pieces = 100;
  }

  public JigsawParam(JigsawParam other) {
    this.cutter = other.cutter;
    this.pieces = other.pieces;
    this.filename = other.filename;
    this.rectangle = other.rectangle;
  }

  public JigsawCutter getCutter() {
    cutter.setPreferredPieceCount(pieces);
    return cutter;
  }

  public void setCutter(JigsawCutter cutter) {
    this.cutter = cutter;
  }

  public int getPieces() {
    return pieces;
  }

  public void setPieces(int pieces) {
    this.pieces = pieces;
  }

  public File getFilename() {
    return filename;
  }

  public void setFilename(File filename) {
    this.filename = filename;
  }

  public void setRectangle(Rectangle rectangle) {
    this.rectangle = rectangle;
  }

  public Rectangle getRectangle() {
    return rectangle;
  }
}
