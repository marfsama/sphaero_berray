package uk.co.petertribble.sphaero2.gui;

import com.berray.Game;
import com.berray.GameObject;
import com.raylib.Raylib;

import java.util.function.BiConsumer;

import static com.raylib.Raylib.*;

public class ShaderNode extends GameObject {

  private final Shader shader;

  public ShaderNode(Shader shader) {
    this.shader = shader;
  }

  @Override
  public void visitDrawChildren(BiConsumer<String, Runnable> visitor) {
    visitor.accept(get("layer", Game.DEFAULT_LAYER), () -> BeginShaderMode(shader));
    super.visitDrawChildren(visitor);
    visitor.accept(get("layer", Game.DEFAULT_LAYER), Raylib::EndShaderMode);
  }

}
