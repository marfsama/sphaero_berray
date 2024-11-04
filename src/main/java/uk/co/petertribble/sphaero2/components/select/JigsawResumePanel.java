package uk.co.petertribble.sphaero2.components.select;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JigsawResumePanel extends JPanel implements ActionListener {
  private Path folder;
  private ActionListener actionListener;

  public JigsawResumePanel(Path folder) {
    this.folder = folder;
    initComponents();
  }

  private void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    try {
      Files.list(folder)
          .filter(Files::isDirectory)
          .sorted((path1, path2) -> {
                try {
                  return -Files.getLastModifiedTime(path1).compareTo(Files.getLastModifiedTime(path2));
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
          )
          .forEach(path -> {
            try {
              JigsawResumeJigsawPanel comp = new JigsawResumeJigsawPanel(path);
              comp.addActionListener(JigsawResumePanel.this);
              add(comp);
            } catch (Exception e) {
              e.printStackTrace();
            }
          });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setActionListener(ActionListener actionListener) {
    this.actionListener = actionListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (actionListener != null) {
      actionListener.actionPerformed(new ActionEvent(this, e.getID(), e.getActionCommand()));
    }
  }

  public void refresh() {
    removeAll();
    initComponents();
  }
}
