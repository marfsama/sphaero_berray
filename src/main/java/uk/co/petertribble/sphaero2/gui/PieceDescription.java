package uk.co.petertribble.sphaero2.gui;

import com.berray.math.Rect;

public class PieceDescription {
  private int id;
  private int texture;
  private Rect texturePosition;

  public PieceDescription(int id, int texture, Rect texturePosition) {
    this.id = id;
    this.texture = texture;
    this.texturePosition = texturePosition;
  }

  public int getId() {
    return id;
  }

  public int getTexture() {
    return texture;
  }

  public Rect getTexturePosition() {
    return texturePosition;
  }
}
