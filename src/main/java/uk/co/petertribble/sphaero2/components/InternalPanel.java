package uk.co.petertribble.sphaero2.components;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseEvent;

public class InternalPanel extends JPanel {
  private final String title;


  public InternalPanel(String title) {
    this.title = title;
    setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 4)
        )
    );

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

  private enum Corner {
    NORTH(Cursor.N_RESIZE_CURSOR),
    NORTH_EAST(Cursor.NE_RESIZE_CURSOR),
    EAST(Cursor.E_RESIZE_CURSOR),
    SOUTH_EAST(Cursor.SE_RESIZE_CURSOR),
    SOUTH(Cursor.S_RESIZE_CURSOR),
    SOUTH_WEST(Cursor.SW_RESIZE_CURSOR),
    WEST(Cursor.W_RESIZE_CURSOR),
    NORTH_WEST(Cursor.NW_RESIZE_CURSOR),
    NONE(-1);;

    private final int cursorId;

    Corner(int cursorId) {
      this.cursorId = cursorId;
    }

    public int getCursorId() {
      return cursorId;
    }

    public boolean isNorth() {
      return this == NORTH || this == NORTH_EAST || this == NORTH_WEST;
    }

    public boolean isSouth() {
      return this == SOUTH || this == SOUTH_EAST || this == SOUTH_WEST;
    }

    public boolean isEast() {
      return this == EAST || this == NORTH_EAST || this == SOUTH_EAST;
    }

    public boolean isWest() {
      return this == WEST || this == NORTH_WEST || this == SOUTH_WEST;
    }

  }

  private enum DragMode {
    NONE,
    RESIZE,
    MOVE
  }

  private static class MouseAdapter extends java.awt.event.MouseAdapter {
    private final JPanel panel;
    private Point dragStartPanelLocation;
    private Dimension dragStartPanelSize;
    private Corner resizeCorner = Corner.NONE;
    private Point dragStartMouse;
    private DragMode dragMode = DragMode.NONE;
    private int MIN_WIDTH = 100;
    private int MIN_HEIGHT = 20;

    public MouseAdapter(JPanel panel) {
      this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      dragStartMouse = e.getLocationOnScreen();
      dragStartPanelLocation = panel.getLocation();
      dragStartPanelSize = panel.getSize();
      System.out.println("pressed");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      dragStartMouse = null;
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      dragMode = DragMode.NONE;
      System.out.println("release");
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      var p = e.getLocationOnScreen();

      Corner windowCorner = getWindowCorner(e.getPoint());
      if (dragMode == DragMode.NONE) {
        // drag started. check mode
        if (windowCorner != Corner.NONE) {
          panel.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
          dragMode = DragMode.RESIZE;
          resizeCorner = windowCorner;
        } else {
          dragMode = DragMode.MOVE;
          panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          resizeCorner = Corner.NONE;
        }
      }
      System.out.println("dragged " + dragMode + " " + e.getPoint() + " " + panel.getSize() + " " + resizeCorner+" "+panel.getInsets());

      int deltax = p.x - dragStartMouse.x;
      int deltay = p.y - dragStartMouse.y;

      if (dragMode == DragMode.MOVE) {
        panel.setLocation(dragStartPanelLocation.x + deltax, dragStartPanelLocation.y + deltay);
      } else {
        Point location = panel.getLocation();
        Dimension size = panel.getSize();
        if (resizeCorner.isNorth()) {
          location.y = dragStartPanelLocation.y + deltay;
          size.height = dragStartPanelSize.height - deltay;
        }
        if (resizeCorner.isWest()) {
          location.x = dragStartPanelLocation.x + deltax;
          size.width = dragStartPanelSize.width - deltax;
        }
        if (resizeCorner.isEast()) {
          size.width = dragStartPanelSize.width + deltax;
        }
        if (resizeCorner.isSouth()) {
          size.height = dragStartPanelSize.height + deltay;
        }
        size.width = Math.max(size.width, MIN_WIDTH);
        size.height = Math.max(size.height, MIN_HEIGHT);
        panel.setLocation(location);
        panel.setSize(size);
      }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
      if (dragMode == DragMode.NONE) {
        Corner windowCorner = getWindowCorner(e.getPoint());
        if (windowCorner != Corner.NONE) {
          panel.setCursor(Cursor.getPredefinedCursor(windowCorner.getCursorId()));
        } else {
          panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
      }
      dragMode = DragMode.NONE;
    }

    private Corner getWindowCorner(Point e) {
      int width = panel.getWidth();
      int height = panel.getHeight();
      Insets insets = panel.getInsets();

      if (e.getX() <= insets.left && e.getY() < insets.top) {
        return Corner.NORTH_WEST;
      }
      if (e.getX() >= width - insets.right && e.getY() < insets.top) {
        return Corner.NORTH_EAST;
      }
      if (e.getX() >= width - insets.right && e.getY() > height - insets.bottom) {
        return Corner.SOUTH_EAST;
      }
      if (e.getX() <= insets.left && e.getY() > height - insets.bottom) {
        return Corner.SOUTH_WEST;
      }

      if (e.getY() <= insets.top) {
        return Corner.NORTH;
      }
      if (e.getX() >= width - insets.right) {
        return Corner.EAST;
      }
      if (e.getY() >= height - insets.bottom) {
        return Corner.SOUTH;
      }
      if (e.getX() <= insets.left) {
        return Corner.WEST;
      }
      return Corner.NONE;
    }
  }
}
