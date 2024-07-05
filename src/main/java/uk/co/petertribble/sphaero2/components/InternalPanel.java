package uk.co.petertribble.sphaero2.components;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class InternalPanel extends JPanel {
  private static final int TITLEBAR_HEIGHT = 20;
  private final String title;


  public InternalPanel(String title) {
    this.title = title;
    setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

    MouseAdapter mouseAdapter = new MouseAdapter(this);
    addMouseListener(mouseAdapter);
    addMouseMotionListener(mouseAdapter);

    setLayout(new BorderLayout());
    JLabel titleLabel = new JLabel(title, JLabel.CENTER);
    titleLabel.setBackground(Color.BLUE);
    titleLabel.setOpaque(true);
    titleLabel.setForeground(Color.LIGHT_GRAY);
    add(titleLabel, BorderLayout.NORTH);
  }

  public void setContentPane(JComponent contentPane) {
    Component centerComponent = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
    if (centerComponent != null) {
      remove(centerComponent);
    }
    add(contentPane, BorderLayout.CENTER);
    validate();
  }

  private static class MouseAdapter extends java.awt.event.MouseAdapter {
    private final JPanel panel;
    private Point dragStartPanel;
    private Point dragStartMouse;

    public MouseAdapter(JPanel panel) {
      this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      dragStartMouse = e.getLocationOnScreen();
      dragStartPanel = panel.getLocation();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      dragStartMouse = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      var p = e.getLocationOnScreen();
      if (dragStartMouse != null) {
        int deltax = p.x - dragStartMouse.x;
        int deltay = p.y - dragStartMouse.y;
        panel.setLocation(dragStartPanel.x + deltax, dragStartPanel.y + deltay);
      }
    }
  }
}
