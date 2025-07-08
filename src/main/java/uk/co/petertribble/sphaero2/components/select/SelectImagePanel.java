package uk.co.petertribble.sphaero2.components.select;


import uk.co.petertribble.sphaero2.components.select.filechooser.ImageFileView;
import uk.co.petertribble.sphaero2.components.select.filechooser.ImagePreview;
import uk.co.petertribble.sphaero2.components.select.filechooser.JigFileFilter;
import uk.co.petertribble.sphaero2.components.select.resume.JigsawResumeItemPanel;
import uk.co.petertribble.sphaero2.components.select.resume.JigsawResumeListPanel;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.JigsawParam;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Panel to select the image to cut together with some configurations.
 */
public class SelectImagePanel extends JPanel implements ActionListener {
  // for the interactive prompt
  private static final Color HELP_COLOR = new Color(100, 100, 150);
  private static final int DEFAULT_PIECES = JigsawCutter.DEFAULT_PIECES;

  public static final String JIGSAW_PARAMS = "jigsawParams";
  public static final String JIGSAW = "jigsaw";
  private JigsawParam jigsawParams;

  private JTextField imageFileNameField;
  private JButton browseButton;
  private JButton okButton;
  private JigsawResumeListPanel jigsawResumePanel;
  private ImagePropertiesPanel propertiesPanel;
  private ActionListener startListener;

  public SelectImagePanel() {
    this.jigsawParams = new JigsawParam();
    jigsawParams.setCutter(JigsawCutter.cutters[0]);
    jigsawParams.setPieces(DEFAULT_PIECES);
    initComponents();
  }

  public void setStartListener(ActionListener startListener) {
    this.startListener = startListener;
  }

  public JigsawParam getJigsawParams() {
    return jigsawParams;
  }

  public void setJigsawParams(JigsawParam jigsawParams) {
    this.jigsawParams = jigsawParams;
  }

  private void initComponents() {
    JPanel mainPane = this;
    mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));

    imageFileNameField = new JTextField();
    imageFileNameField.setText(getCurrentPath());
    imageFileNameField.selectAll();

    browseButton = new JButton("Browse...");
    browseButton.setMnemonic(KeyEvent.VK_B);
    browseButton.addActionListener(this);

    JPanel imageBPane = new JPanel();
    imageBPane.setLayout(new BoxLayout(imageBPane, BoxLayout.LINE_AXIS));
    imageBPane.setBorder(createTitledBorder("Find an image"));
    imageBPane.add(imageFileNameField);
    imageBPane.add(Box.createRigidArea(new Dimension(10, 0)));
    imageBPane.add(browseButton);

    JPanel okPanel = new JPanel();
    okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.LINE_AXIS));
    okPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    okPanel.add(Box.createHorizontalGlue());
    okButton = new JButton("Start Puzzling");
    okButton.setMnemonic(KeyEvent.VK_K);
    okButton.addActionListener(this);
    okPanel.add(okButton);

    JPanel resumePane = new JPanel();
    resumePane.setLayout(new BorderLayout());
    this.jigsawResumePanel = new JigsawResumeListPanel(Path.of(System.getProperty("user.home")).resolve(".sphaero"));
    jigsawResumePanel.setActionListener(this);
    resumePane.add(new JScrollPane(jigsawResumePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.CENTER);
    resumePane.setBorder(createTitledBorder("Resume"));

    propertiesPanel = new ImagePropertiesPanel();
    propertiesPanel.setBorder(createTitledBorder("Preview"));

    mainPane.add(imageBPane);
    mainPane.add(propertiesPanel);
    mainPane.add(resumePane);
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
    String text = imageFileNameField.getText().trim();
    return new File(text.isEmpty() ? "." : text);
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

  private void fireBrowseAction() {
    JFileChooser chooser = new JFileChooser(getCurrentFolder());
    chooser.setFileFilter(new JigFileFilter());
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setAccessory(new ImagePreview(chooser));
    chooser.setFileView(new ImageFileView());
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      imageFileNameField.setText(chooser.getSelectedFile().getAbsolutePath());
      jigsawParams.setFilename(chooser.getSelectedFile());
      propertiesPanel.setImage(chooser.getSelectedFile());
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == browseButton) {
      fireBrowseAction();
    } else if (e.getSource() == okButton) {
      jigsawParams.setRectangle(propertiesPanel.getImageSelection());
      jigsawParams.setPieces(propertiesPanel.getPieceCount());
      jigsawParams.setCutter(propertiesPanel.getSelectedCutter());
      if (startListener != null) {
        startListener.actionPerformed(new ActionEvent(this, 1, "start"));
      }
    } else if (e.getSource() == jigsawResumePanel) {
      if (e.getID() == JigsawResumeItemPanel.LOAD_ACTION_ID) {
        if (startListener != null) {
          startListener.actionPerformed(new ActionEvent(this, 2, e.getActionCommand()));
        }
     }
    }
  }

  public void refresh() {
    jigsawResumePanel.refresh();
  }


}
