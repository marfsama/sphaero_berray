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
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    // paint title bar
    var insets = getInsets();
    var width = getWidth() - insets.left - insets.right;
    g.setColor(Color.BLUE);
    g.fillRect(0, 0, width, TITLEBAR_HEIGHT);
    g.setFont(g.getFont().deriveFont(14f).deriveFont(Font.BOLD));
    g.setColor(Color.LIGHT_GRAY);
    Rectangle2D titleTextBounds = g.getFontMetrics().getStringBounds(title, g);
    int x = (int) ((width - titleTextBounds.getWidth()) / 2);
    g.drawString(title, x, TITLEBAR_HEIGHT - (int) (TITLEBAR_HEIGHT - 14f) / 2);

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
