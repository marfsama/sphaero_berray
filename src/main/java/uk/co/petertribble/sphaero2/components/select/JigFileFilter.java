package uk.co.petertribble.sphaero2.components.select;

import uk.co.petertribble.sphaero2.JigUtil;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * A file filter that just accepts images.
 *
 * @author Peter Tribble
 */
public class JigFileFilter extends FileFilter {
  @Override
  public boolean accept(File f) {
    return f.isDirectory() || JigUtil.isImage(f);
  }

  @Override
  public String getDescription() {
    return "Image files.";
  }
}
