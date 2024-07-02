package uk.co.petertribble.sphaero2;

import uk.co.petertribble.sphaero2.components.JigsawPanel;
import uk.co.petertribble.sphaero2.components.JigsawPiecesPanel;
import uk.co.petertribble.sphaero2.components.SelectImagePanel;
import uk.co.petertribble.sphaero2.components.TimeLabel;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

/**
 * JFrame that runs a JigsawPuzzle. This is the front end for
 * JigsawPuzzle, the main class of the jigsaw application.
 * <p>
 * "Sphaero" is short for "Sphaerodactylinea". This is the name of one
 * of the subfamilies of geckos, including house geckos, tokay geckos,
 * striped leaf geckos, and several other common varieties. It reminded me
 * of Escher's depiction of a 2D surface tiled by lizards, which vaguely
 * resemble jigsaw puzzle pieces. Hence the name. ("Gecko" was already
 * taken.)
 * <p>
 * Known Bugs
 * <p>
 * You can rotate a piece or pieces while dragging them; that's a
 * feature.  However, it doesn't rotate around the mouse cursor in that
 * case. It uses the center of mass of the piece (I think); the upshot is
 * that a piece may appear to jump out from under the cursor, yet still
 * respond to dragging.
 * <p>
 * The program may report one or more NullPointerExceptions when
 * loading the image. This seems to be in the native image-loading code,
 * possibly due to some image data being accessed before it has loaded.
 * I've never seen any missing pieces or image data as a result of this,
 * however.
 * <p>
 * The most serious bug is an OutOfMemoryError while the puzzle is
 * being solved. This occurs particularly on large images and large
 * numbers of pieces (200+), and even then only if picture is solved by
 * forming one large set of fitted pieces, and adding pieces singly to
 * that. If it's solved instead by forming medium-sized sections first,
 * and then fitting those sections together at the end, no problems arise.
 * <p>
 * This program uses a fair bit of memory. I use a max heap size of
 * 256Mb for large (1024x768 pixels) images, 200 pieces, and occasionally
 * will still get an OutOfMemoryError as above.
 */
public class JigsawFrame extends JFrame implements ActionListener {

  public static final int THUMB_HEIGHT = 150;
  public static final int THUMB_WIDTH = 150;
  private JMenuBar jmb;
  private JMenu jmh;
  private JMenuItem newItem;
  private JMenuItem exitItem;
  private JMenuItem helpItem;
  private JMenuItem aboutItem;
  private JMenuItem pictureItem;
  private Image image;
  private Icon miniImage;
  private Jigsaw jigsaw;


  private int pHeight = 480;
  private int pWidth = 640;
  private SelectImagePanel selectImagePanel;
  private JButton save;

  /**
   * Creates and displays a simple JFrame containing a jigsaw puzzle in a
   * JScrollPane. The frame may be resized freely. If an image is supplied
   * on the command line, it will be used; otherwise the user will be
   * prompted.
   *
   * <h1>Command line arguments</h1>
   *
   * <pre>
   * -p &lt;<i>number</i>&gt; Cut the picture into roughly this number of
   * pieces.
   * &lt;<i>filename</i>&gt; If this denotes an image file, it will be
   * the target picture.  If it denotes a folder, it will be searched
   * for a random file, which is subject to the rules above.
   * Potentially any image file in any subfolder could be used. If an
   * image file cannot be found this way after ten tries, the program
   * halts.
   * </pre>
   *
   * <p>100 pieces are created by default. If no filename is given, the
   * current folder is used.
   *
   * <h1>Puzzle commands</h1>
   *
   * <p> Pieces can be dragged around with the mouse. The piece (or group
   * of pieces) most recently dragged or clicked on is the active piece.
   * Press R to rotate the active piece (or group) 90 degrees clockwise.
   * Press E to rotate it 90 degrees counter-clockwise. (Case doesn't
   * matter.) Press S to shuffle all the pieces around the panel randomly,
   * keeping fitted pieces together. Pieces are fitted automatically if
   * they are placed close enough, and are rotated the same way.
   *
   * @param image  the BufferedImage to use as the picture
   * @param pieces the number of pieces to create
   * @param cutter the JigsawCutter to be used to cut the image into pieces
   */
  public JigsawFrame(BufferedImage image, int pieces, JigsawCutter cutter) {
    super("Jigsaw Puzzle");
    initFrameWork();
    JigsawParam params = new JigsawParam();
    params.setCutter(cutter);
    params.setPieces(pieces);
    Jigsaw jigsaw = new Jigsaw(params, image);

    init(jigsaw, true);
  }

  /**
   * Prompt for an image to solve, with the given number of pieces
   * and piece style.
   */
  public JigsawFrame() {
    super("Jigsaw Puzzle");
    initFrameWork();
    initSelectImagePrompt();
  }

  private void initFrameWork() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    this.selectImagePanel = new SelectImagePanel();
    selectImagePanel.addPropertyChangeListener(event -> {
      if (SelectImagePanel.JIGSAW_PARAMS.equals(event.getPropertyName())) {
        JigsawParam params = (JigsawParam) event.getNewValue();
        setupPuzzle(params);
      } else if (SelectImagePanel.JIGSAW.equals(event.getPropertyName())) {
        Jigsaw jigsaw = (Jigsaw) event.getNewValue();
        init(jigsaw, false);
      }
    });


    JMenu jmf = new JMenu("File");
    jmf.setMnemonic(KeyEvent.VK_F);

    newItem = new JMenuItem("New Image", KeyEvent.VK_N);
    newItem.addActionListener(this);
    jmf.add(newItem);

    jmf.addSeparator();

    exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
    exitItem.addActionListener(this);
    jmf.add(exitItem);

    jmb = new JMenuBar();
    jmb.add(jmf);
    setJMenuBar(jmb);

    setIconImage(new ImageIcon(this.getClass().getClassLoader()
        .getResource("pixmaps/sphaero2.png")).getImage());

    /*
     * Create the help menu for the puzzle here, even though it's only
     * visible in puzzle mode.
     */
    jmh = new JMenu("Help");
    jmh.setMnemonic(KeyEvent.VK_H);
    helpItem = new JMenuItem("Instructions", KeyEvent.VK_I);
    helpItem.addActionListener(this);
    jmh.add(helpItem);
    aboutItem = new JMenuItem("About", KeyEvent.VK_A);
    aboutItem.addActionListener(this);
    jmh.add(aboutItem);
    pictureItem = new JMenuItem("Show Picture", KeyEvent.VK_P);
    pictureItem.addActionListener(this);
    jmh.add(pictureItem);
  }

  private void init(Jigsaw jigsaw, boolean cut) {
    this.jigsaw = jigsaw;


    JigsawPanel puzzle = new JigsawPanel(jigsaw);
    JPanel oldJigsawPane = new JPanel(new BorderLayout());
    oldJigsawPane.add(new JScrollPane(puzzle));
    TimeLabel tlabel = createStatusBar(oldJigsawPane);

    JTabbedPane contentPane = new JTabbedPane();
    contentPane.addTab("Old Puzzle", oldJigsawPane);

    JigsawPiecesPanel newJigsawPane = new JigsawPiecesPanel();
    contentPane.addTab("New Puzzle", new JScrollPane(newJigsawPane));

    setContentPane(contentPane);
    pack();

    setSize(1024, 740);
    setVisible(true);

    if (cut) {
      // This doesn't quite work; I would prefer a modal dialog, but that
      // completely blocks the app
      JProgressBar jp = new JProgressBar();
      jp.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
      JDialog dialog = new JDialog(this, "Processing image.");
      dialog.setContentPane(jp);
      dialog.pack();
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
      jigsaw.getParams().getCutter().setJProgressBar(jp);
      jigsaw.reset();
      dialog.setVisible(false);
    }
    newJigsawPane.setPiecesBin(new PiecesBin(jigsaw.getPieces().getPieces()));
    jmb.add(jmh);
    repaint();
    tlabel.start();
    puzzle.setTimeLabel(tlabel);
  }

  private TimeLabel createStatusBar(JPanel ppanel) {
    JPanel statusbar = new JPanel();
    statusbar.setLayout(new FlowLayout(FlowLayout.RIGHT));
    TimeLabel tlabel = new TimeLabel();
    statusbar.add(new JLabel("Progress: 0% (5/5)"));
    statusbar.add(Box.createHorizontalStrut(2));
    statusbar.add(tlabel);
    this.save = new JButton(new SaveAction());
    statusbar.add(Box.createHorizontalStrut(2));
    statusbar.add(save);


    ppanel.add(statusbar, BorderLayout.SOUTH);
    return tlabel;
  }

  private void initSelectImagePrompt() {
    setContentPane(selectImagePanel);
    setSize(pWidth, pHeight);
    setVisible(true);
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      new JigsawFrame();
    }
  }

  private void showPrompt() {
    getContentPane().removeAll();
    jmb.remove(jmh);
    jmb.revalidate();
    miniImage = null;
    System.gc();
    initSelectImagePrompt();
  }

  private void showPicture() {
    if (miniImage == null) {
      miniImage = new ImageIcon(image.getScaledInstance(200, -1,
          Image.SCALE_FAST));
    }
    JOptionPane.showMessageDialog(this, miniImage,
        "Quick view", JOptionPane.PLAIN_MESSAGE);
  }


  private void setupPuzzle(JigsawParam params) {
    // Get the image.
    File file = params.getFilename();

    if (!file.exists()) {
      JOptionPane.showMessageDialog(this, "File does not exist.",
          "Nonexistent file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (file.isDirectory()) {
      try {
        file = JigUtil.getRandomImageFile(file);
      } catch (FileNotFoundException ex) {
        JOptionPane.showMessageDialog(this,
            "This folder contains no images.",
            "Empty folder", JOptionPane.ERROR_MESSAGE);
        return;
      }
    } else if (!JigUtil.isImage(file)) {
      JOptionPane.showMessageDialog(this, "This is not an image file.",
          "Invalid Image", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Get the cutter and set its piece count
    JigsawCutter cutter = params.getCutter();
    int pieces = params.getPieces();
    cutter.setPreferredPieceCount(pieces);

    try {
      BufferedImage image = ImageIO.read(file);
      // FIXME this doesn't actually show the window properly until
      // after the pieces have been cut???
      // So the progress bar doesn't work either
      init(new Jigsaw(params, JigUtil.resizeImage(image)), true);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Image file cannot be read.",
          "Invalid Image", JOptionPane.ERROR_MESSAGE);
    }
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == exitItem) {
      System.exit(0);
    } else if (e.getSource() == newItem) {
      showPrompt();
    } else if (e.getSource() == helpItem) {
      JOptionPane.showMessageDialog(this, JigUtil.helpMsg(),
          "Sphaero2 help", JOptionPane.PLAIN_MESSAGE);
    } else if (e.getSource() == aboutItem) {
      JOptionPane.showMessageDialog(this, JigUtil.aboutMsg(),
          "About Sphaero2", JOptionPane.PLAIN_MESSAGE);
    } else if (e.getSource() == pictureItem) {
      showPicture();
    }
  }


  public class SaveAction extends AbstractAction {


    public SaveAction() {
      super("Save");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (jigsaw != null && !jigsaw.isFinished()) {
        String directory = jigsaw.getParams().getFilename().getName();
        int pos = directory.lastIndexOf(".");
        if (pos > 0 && pos < (directory.length() - 1)) { // If '.' is not the first or last character.
          directory = directory.substring(0, pos);
        }
        Path outPath = Path.of(System.getProperty("user.home"), ".sphaero", directory);
        try {
          Files.createDirectories(outPath);
          try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(outPath.resolve("save.txt"), WRITE, TRUNCATE_EXISTING, CREATE)));
               ImageOutputStream piecesData = new MemoryCacheImageOutputStream(Files.newOutputStream(outPath.resolve("pieces.bin"), WRITE, TRUNCATE_EXISTING, CREATE))
          ) {
            writer.println("file: " + jigsaw.getParams().getFilename().getAbsolutePath());
            writer.println("pieces: " + jigsaw.getParams().getPieces());
            writer.println("cutter: " + jigsaw.getParams().getCutter().getName());
            writer.println("# piece: id, imageX, imageY, imageWidth, imageHeight, puzzleX, puzzleY, rotation, multipieceid, neighbours (list of ids)");
            writer.println("# multipiece: id, imageX, imageY, imageWidth, imageHeight, puzzleX, puzzleY, rotation");
            for (Piece piece : jigsaw.getPieces().getPieces()) {
              for (Piece subPiece : piece.getSubs()) {
                writer.println("piece: " + subPiece.getId() + ", "
                    + subPiece.getImageX() + ", " + subPiece.getImageY() + ", "
                    + subPiece.getImageWidth() + ", " + subPiece.getImageHeight() + ", "
                    + subPiece.getPuzzleX() + ", " + subPiece.getPuzzleY() + ", "
                    + subPiece.getRotation() + ", "
                    + (piece instanceof MultiPiece ? piece.getId() : -1) + ", "
                    + subPiece.getNeighbors().stream().map(Piece::getId).map(String::valueOf).collect(Collectors.joining(","))
                );

                int[] data = subPiece.getData();
                piecesData.writeInts(subPiece.getData(), 0, data.length);
              }
              if (piece instanceof MultiPiece) {
                MultiPiece subPiece = (MultiPiece) piece;
                writer.println("multipiece: " + subPiece.getId() + ", "
                    + subPiece.getImageX() + ", " + subPiece.getImageY() + ", "
                    + subPiece.getImageWidth() + ", " + subPiece.getImageHeight() + ", "
                    + subPiece.getPuzzleX() + ", " + subPiece.getPuzzleY() + ", "
                    + subPiece.getRotation() + ", "
                    + subPiece.getNeighbors().stream().map(Piece::getId).map(String::valueOf).collect(Collectors.joining(","))
                );
              }
            }
          }
          // write original image
          ImageIO.write(jigsaw.getImage(), "png", outPath.resolve("source.png").toFile());
          BufferedImage thumbnail = JigUtil.resizeImage(jigsaw.getImage(), THUMB_WIDTH, THUMB_HEIGHT);
          ImageIO.write(thumbnail, "png", outPath.resolve("thumb.png").toFile());

          // write current solve state
          BufferedImage currentState = new BufferedImage(jigsaw.getWidth(), jigsaw.getHeight(), BufferedImage.TYPE_INT_ARGB);
          Graphics graphics = currentState.getGraphics();
          for (Piece piece : jigsaw.getPieces().getPieces()) {
            piece.draw(graphics);
          }
          graphics.dispose();
          BufferedImage currentStateThumbnail = JigUtil.resizeImage(currentState, THUMB_WIDTH, THUMB_HEIGHT);
          ImageIO.write(currentStateThumbnail, "png", outPath.resolve("state.png").toFile());
          thumbnail.flush();
          currentState.flush();
          currentStateThumbnail.flush();

        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }

      }
    }
  }


}
