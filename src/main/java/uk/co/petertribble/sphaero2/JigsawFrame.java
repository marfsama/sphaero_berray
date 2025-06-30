package uk.co.petertribble.sphaero2;

import uk.co.petertribble.sphaero2.components.*;
import uk.co.petertribble.sphaero2.components.play.*;
import uk.co.petertribble.sphaero2.components.select.SelectImagePanel;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

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
  private JMenuBar manuBar;
  private JMenu jmh;
  private JMenuItem newItem;
  private JMenuItem exitItem;
  private JMenuItem helpItem;
  private JMenuItem aboutItem;
  private Jigsaw jigsaw;


  private int pHeight = 480;
  private int pWidth = 640;
  private SelectImagePanel selectImageFrame;
  private JButton save;
  private JLabel progressLabel;
  private TimeLabel tlabel;
  private InputManager inputManager = new InputManager();
  private JigsawPanel puzzle;

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

    this.selectImageFrame = new SelectImagePanel();
    selectImageFrame.addPropertyChangeListener(event -> {
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

    manuBar = new JMenuBar();
    manuBar.add(jmf);
    setJMenuBar(manuBar);

    setIconImage(new ImageIcon(this.getClass().getClassLoader().getResource("pixmaps/sphaero2.png")).getImage());

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
  }

  private void init(Jigsaw jigsaw, boolean cut) {
    this.jigsaw = jigsaw;
    inputManager.clear();

    JLayeredPane layeredPane = getLayeredPane();
    Component[] palettes = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
    Arrays.stream(palettes).forEach(layeredPane::remove);

    layeredPane.add(inputManager, JLayeredPane.DRAG_LAYER);
    inputManager.setSize(layeredPane.getSize());
    layeredPane.addComponentListener(inputManager);

    InternalPanel previewPanel = new InternalPanel("Preview");
    previewPanel.setBounds(100, 100, 200, 200);
    ImagePanel previewImagePanel = new ImagePanel();
    previewImagePanel.setImage(jigsaw.getImage());
    previewImagePanel.setScale(true);
    previewImagePanel.setBackground(Color.BLACK);
    previewPanel.setContentPane(previewImagePanel);
    layeredPane.add(previewPanel, JLayeredPane.PALETTE_LAYER);

    this.puzzle = new JigsawPanel(jigsaw);
    JPanel oldJigsawPane = new JPanel(new BorderLayout());
    oldJigsawPane.add(new JScrollPane(puzzle));
    createStatusBar(oldJigsawPane);
    createToolBar(oldJigsawPane);
    inputManager.addPiecesPanel(puzzle);
    setContentPane(oldJigsawPane);

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
      //jigsaw.getParams().getCutter().setJProgressBar(jp);
      jigsaw.reset();
      dialog.setVisible(false);
    }
    manuBar.add(jmh);
    repaint();
    tlabel.start();
    puzzle.requestFocus();
    puzzle.setTimeLabel(tlabel);
    puzzle.setProgressLabel(progressLabel);
  }

  private void createStatusBar(JPanel panel) {
    JPanel statusbar = new JPanel();
    statusbar.setLayout(new FlowLayout(FlowLayout.RIGHT));
    this.tlabel = new TimeLabel();
    this.progressLabel = new JLabel("Progress: 0% (5/5)");
    statusbar.add(progressLabel);
    statusbar.add(Box.createHorizontalStrut(2));
    statusbar.add(tlabel);
    this.save = new JButton(new SaveAction(jigsaw));
    statusbar.add(Box.createHorizontalStrut(2));
    statusbar.add(save);

    panel.add(statusbar, BorderLayout.SOUTH);
  }

  private void createToolBar(JPanel panel) {
    JToolBar toolbar = new JToolBar();

    toolbar.add(new JToggleButton(new ToolbarAction("toggleSelection")));
    toolbar.add(new JButton(new ToolbarAction("stack", () -> puzzle.stack())));
    toolbar.add(new JButton(new ToolbarAction("disperse")));
    toolbar.add(new JButton(new ToolbarAction("clear")));
    toolbar.add(new JButton(new ToolbarAction("shuffle")));
    toolbar.add(new JButton(new ToolbarAction("arrange")));

    panel.add(toolbar, BorderLayout.NORTH);
  }


  private void initSelectImagePrompt() {
    selectImageFrame.refresh();
    setContentPane(selectImageFrame);
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
    manuBar.remove(jmh);
    manuBar.revalidate();
    System.gc();
    initSelectImagePrompt();
  }

  private void setupPuzzle(JigsawParam params) {
    // Get the image.
    File file = params.getFilename();

    if (!file.exists()) {
      JOptionPane.showMessageDialog(this, "File does not exist.",
          "Nonexistent file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (!JigUtil.isImage(file)) {
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
      Rectangle rectangle = params.getRectangle();
      if (rectangle != null) {
        image = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
      }
      init(new Jigsaw(params, JigUtil.resizeImage(image)), true);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Image file cannot be read.", "Invalid Image", JOptionPane.ERROR_MESSAGE);
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
    }
  }


}
