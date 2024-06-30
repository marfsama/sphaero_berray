package uk.co.petertribble.sphaero2.components;


import uk.co.petertribble.sphaero2.*;
import uk.co.petertribble.sphaero2.model.JigsawParam;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * Panel to select the image to cut together with some configurations.
 */
public class SelectImagePanel extends JPanel implements ActionListener {
  // for the interactive prompt
  private static final Color HELP_COLOR = new Color(100, 100, 150);
  private static final int DEFAULT_PIECES = JigsawCutter.DEFAULT_PIECES;

  private static final JigsawCutter[] cutters = {
      new Classic4Cutter(),
      new ClassicCutter(),
      new SquareCutter(),
      new RectCutter(),
      new QuadCutter(),
  };
  public static final String JIGSAW_PARAMS = "jigsawParams";
  private JigsawParam jigsawParams;

  private JTextField imageField;
  private JButton browseButton;
  private JComboBox<JigsawCutter> cutterCBox;
  private JSpinner pieceSpinner;
  private JLabel cutterDescLabel;
  private JButton okButton;


  public SelectImagePanel() {
    this.jigsawParams = new JigsawParam();
    jigsawParams.setCutter(cutters[0]);
    jigsawParams.setPieces(DEFAULT_PIECES);
    initComponents();
  }

  private void initComponents() {
    JPanel mainPane = this;
    mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));

    imageField = new JTextField();
    imageField.setText(getCurrentPath());
    imageField.selectAll();

    browseButton = new JButton("Browse...");
    browseButton.setMnemonic(KeyEvent.VK_B);
    browseButton.addActionListener(this);

    JLabel imageLabel = createHelpLabel("<html>"
        + "If this is an image file, it is used to create the puzzle. "
        + "If it is a folder, an image file is selected from it "
        + "(including any subfolders) at random.");

    JPanel imageBPane = new JPanel();
    imageBPane.setLayout(new BoxLayout(imageBPane, BoxLayout.LINE_AXIS));
    imageBPane.add(imageField);
    imageBPane.add(Box.createRigidArea(new Dimension(10, 0)));
    imageBPane.add(browseButton);

    JPanel imagePane = new JPanel(new BorderLayout());
    imagePane.setBorder(createTitledBorder("Find an image"));
    imagePane.add(imageBPane, BorderLayout.NORTH);
    imagePane.add(imageLabel, BorderLayout.CENTER);

    cutterCBox = new JComboBox<>(cutters);
    cutterCBox.setSelectedItem(jigsawParams.getCutter());
    cutterCBox.addActionListener(this);

    cutterDescLabel = createHelpLabel(null);
    JPanel cutterPane = new JPanel(new BorderLayout());
    cutterPane.add(cutterCBox, BorderLayout.NORTH);
    cutterPane.add(cutterDescLabel, BorderLayout.CENTER);
    cutterPane.setBorder(createTitledBorder("Piece Style"));
    fireCutterChanged();

    pieceSpinner = new JSpinner(new SpinnerNumberModel(
        DEFAULT_PIECES, JigsawCutter.MIN_PIECES,
        JigsawCutter.MAX_PIECES, 1));
    JLabel pieceLabel = createHelpLabel("<html>"
        + " The puzzle will have roughly this many pieces.");
    JPanel piecePane = new JPanel(new BorderLayout());
    piecePane.add(pieceSpinner, BorderLayout.NORTH);
    piecePane.add(pieceLabel, BorderLayout.CENTER);
    piecePane.setBorder(createTitledBorder("Piece Count"));
    pieceSpinner.addChangeListener(event -> jigsawParams.setPieces((Integer) pieceSpinner.getValue()));

    JPanel okPanel = new JPanel();
    okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.LINE_AXIS));
    okPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    okPanel.add(Box.createHorizontalGlue());
    okButton = new JButton("Start Puzzling");
    okButton.setMnemonic(KeyEvent.VK_K);
    okPanel.add(okButton);
    okButton.addActionListener(this);

    SamplePanel sPanel = new SamplePanel(imageField);
    if (sPanel.samplesValid()) {
      JPanel samplePane = new JPanel(new BorderLayout());
      samplePane.setBorder(createTitledBorder("Select an image"));
      samplePane.add(new JScrollPane(sPanel));
      mainPane.add(samplePane);
    }

    mainPane.add(imagePane);
    mainPane.add(piecePane);
    mainPane.add(cutterPane);
    mainPane.add(okPanel);
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(640, 480);
  }

  /**
   * Returns the folder corresponding to whatever's currently displayed in
   * imageField.
   */
  private File getCurrentFolder() {
    String text = imageField.getText().trim();
    return new File((text.length() == 0) ? "." : text);
  }

  /**
   * Lame way of getting the current path in a way that guarantees an
   * answer returned, and no exception thrown.  Could still throw a
   * SecurityException, but I don't care.
   */
  private String getCurrentPath() {
    File folder = getCurrentFolder();
    try {
      return folder.getCanonicalPath();
    } catch (IOException ex) {
      return folder.getAbsolutePath();
    }
  }

  private JLabel createHelpLabel(String text) {
    JLabel label = new JLabel(text);
    label.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 1));
    label.setForeground(HELP_COLOR);
    return label;
  }

  private Border createTitledBorder(String title) {
    Border outer = BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), title);
    Border inner = BorderFactory.createEmptyBorder(2, 5, 5, 5);
    return BorderFactory.createCompoundBorder(outer, inner);
  }

  private void fireCutterChanged() {
    JigsawCutter cutter = (JigsawCutter) cutterCBox.getSelectedItem();
    cutterDescLabel.setText("<html>" + cutter.getDescription());
    jigsawParams.setCutter(cutter);
  }

  private void fireBrowseAction() {
    JFileChooser chooser = new JFileChooser(getCurrentFolder());
    chooser.setFileFilter(new JigFileFilter());
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setAccessory(new ImagePreview(chooser));
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      imageField.setText(chooser.getSelectedFile().getAbsolutePath());
      jigsawParams.setFilename(chooser.getSelectedFile());
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == browseButton) {
      fireBrowseAction();
    } else if (e.getSource() == cutterCBox) {
      fireCutterChanged();
    } else if (e.getSource() == okButton) {
      firePropertyChange(JIGSAW_PARAMS, jigsawParams, new JigsawParam(jigsawParams));
    }
  }

}
