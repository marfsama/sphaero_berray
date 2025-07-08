package uk.co.petertribble.sphaero2.components.play;

import uk.co.petertribble.sphaero2.model.Jigsaw;
import uk.co.petertribble.sphaero2.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

// ### I think I need a quicker way to detect which piece is clicked on.
//   Mouse-down lags when there are lots of pieces.

/**
 * Jigsaw puzzle.
 */
public class JigsawPanel extends JigsawPiecesPanel {

  // this is the timer label
  private TimeLabel tlabel;
  private JLabel progressLabel;
  private Jigsaw jigsaw;

  /**
   * Creates a new JigsawPuzzle.
   *
   * @param jigsaw the jigsaw
   */
  public JigsawPanel(Jigsaw jigsaw) {
    this.jigsaw = jigsaw;
    super.setPiecesBin(jigsaw.getPieces());
  }

  @Override
  protected void pieceJoined(Piece focusPiece) {
    finish();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (jigsaw == null) {
      return;
    }


    if (jigsaw.isFinished() && jigsaw.getFinishedImage() != null) {
      Piece lastPiece = jigsaw.getPieces().getPieces().get(0);
      int x = lastPiece.getPuzzleX();
      int y = lastPiece.getPuzzleY();
      g.drawImage(jigsaw.getFinishedImage(), x, y, null);
    }
  }

  public void setTimeLabel(TimeLabel tlabel) {
    this.tlabel = tlabel;
  }

  public void setProgressLabel(JLabel progressLabel) {
    this.progressLabel = progressLabel;
  }

  // ### Should this be public?
  private void finish() {
    progressLabel.setText(jigsaw.getPiecesInMultipieces() + "/" + jigsaw.getParams().getPieces());
    if (!jigsaw.calculateFinished()) {
      return;
    }
    jigsaw.setFinished();
    setScale(1.0f);
    Piece lastPiece = jigsaw.getPieces().getPieces().get(0);

    // Auto-rotate the puzzle to its correct position.
    lastPiece.setRotation(0);

    // stop the time label to show the solution time
    if (tlabel != null) {
      tlabel.finished();
    }

    // Center the last piece in the middle of the panel.
    int prevX = lastPiece.getPuzzleX();
    int prevY = lastPiece.getPuzzleY();
    final int width = lastPiece.getImageWidth();
    final int height = lastPiece.getImageHeight();
    int curW = getWidth();
    int curH = getHeight();
    final int centerX = (curW - width) / 2;
    final int centerY = (curH - height) / 2;
    lastPiece.setPuzzlePosition(centerX, centerY);
    lastPiece.setCurrentPosition(centerX, centerY);
    repaint(0, prevX, prevY, width, height);
    repaint(0, centerX, centerY, width, height);

    // Draw the original image on top of the last piece in increasing
    // opaqueness.  This should make the pieces appear to fade into the
    // original image.
    final int[] data = new int[width * height];
    try {
      new PixelGrabber(jigsaw.getImage(), 0, 0, width, height, data, 0, width)
          .grabPixels();
    } catch (InterruptedException ex) {
    }
    for (int i = 0; i < data.length; i++) {
      data[i] = data[i] & 0x00ffffff;
    }

    ActionListener fader = new ActionListener() {
      int trans = 0x00;

      @Override
      public void actionPerformed(ActionEvent evt) {
        for (int i = 0; i < data.length; i++) {
          data[i] = (data[i] & 0x00ffffff) | (trans << 24);
        }
        if (jigsaw.getFinishedImage() != null) {
          jigsaw.getFinishedImage().flush();
        }
        jigsaw.setFinishedImage(Toolkit.getDefaultToolkit().createImage(
            new MemoryImageSource(width, height, data, 0, width)));
        repaint(0, centerX, centerY, width, height);
        if (trans < 0xff) {
          trans += 0x11;
          if (trans >= 0xff) {
            trans = 0xff;
          }
          Timer timer = new Timer(200, this);
          timer.setRepeats(false);
          timer.start();
        }
      }
    };

    Timer timer = new Timer(200, fader);
    timer.setRepeats(false);
    timer.start();
  }


  // Mouse event handling -------------------------------------------------

  protected void mousePressed0(MouseEvent e) {
    if (jigsaw.isFinished()) {
      return;
    }
    super.mousePressed0(e);
  }

  protected void mouseDragged0(MouseEvent e) {
    if (jigsaw.isFinished()) {
      return;
    }
    super.mouseDragged0(e);
  }

  protected void mouseReleased0(MouseEvent e) {
    if (jigsaw.isFinished()) {
      return;
    }
    super.mouseReleased0(e);
  }

  @Override
  protected void keyTyped0(KeyEvent e) {
    if (jigsaw.isFinished()) {
      return;
    }
    super.keyTyped0(e);
  }

  public void stack() {
    if (jigsaw.isFinished()) {
      return;
    }

    super.stack();
  }
}
