package uk.co.petertribble.sphaero2.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;

public class JigsawResumeJigsawPanel extends JPanel {
  public static final int LOAD_ACTION_ID = 1;
  public static final int DELETE_ACTION_ID = 2;
  private Path folder;
  private ImagePanel thumbPanel;
  private ImagePanel statePanel;
  private ActionListener actionListener;

  public JigsawResumeJigsawPanel() {
    initComponents();
  }

  public JigsawResumeJigsawPanel(Path folder) {
    this();
    setFolder(folder);
  }

  public void setFolder(Path folder) {
    this.folder = folder;
    try {
      thumbPanel.setImage(ImageIO.read(folder.resolve("thumb.png").toFile()));
      statePanel.setImage(ImageIO.read(folder.resolve("state.png").toFile()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Path getFolder() {
    return folder;
  }

  private void initComponents() {
    this.thumbPanel = new ImagePanel();
    thumbPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    add(thumbPanel);
    this.statePanel = new ImagePanel();
    statePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    add(statePanel);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(2, 1));
    JButton loadButton = new JButton("load");
    loadButton.addActionListener(event -> {
      if (actionListener != null) {
        actionListener.actionPerformed(new ActionEvent(JigsawResumeJigsawPanel.this, LOAD_ACTION_ID, folder.toAbsolutePath().toString()));
      }
    });
    buttonPanel.add(loadButton);
    JButton deleteButton = new JButton("delete");
    buttonPanel.add(deleteButton);
    add(buttonPanel);
  }

  public void addActionListener(ActionListener actionListener) {
    this.actionListener = actionListener;
  }
}
