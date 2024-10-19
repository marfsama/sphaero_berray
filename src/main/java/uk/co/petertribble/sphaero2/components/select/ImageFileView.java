package uk.co.petertribble.sphaero2.components.select;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageFileView extends FileView {

  private Map<File, ImageIcon> cache = new HashMap<>();

  public String getName(File f) {
    return "";
  }

  public String getDescription(File f) {
    return null; //let the L&F FileView figure this out
  }

  public Boolean isTraversable(File f) {
    return null; //let the L&F FileView figure this out
  }

  public String getTypeDescription(File f) {
    return null;
  }

  public Icon getIcon(File f) {
    if (f.isDirectory()) {
      return null;
    }

    if (!f.getName().endsWith("jpg")) {
      return null;
    }

    if (cache.containsKey(f)) {
      return cache.get(f);
    }

    ImageIcon thumbnail;
    // Don't use createImageIcon (which is a wrapper for getResource)
    // because the image we're trying to load is probably not one
    // of this program's own resources.
    ImageIcon tmpIcon = new ImageIcon(f.getPath());
    if (tmpIcon.getIconWidth() > 90) {
      thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(90, -1, Image.SCALE_DEFAULT));
    } else { // no need to miniaturize
      thumbnail = tmpIcon;
    }

    cache.put(f, thumbnail);

    return thumbnail;
  }
}
