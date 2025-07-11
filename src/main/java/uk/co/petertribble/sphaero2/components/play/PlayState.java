package uk.co.petertribble.sphaero2.components.play;

import uk.co.petertribble.sphaero2.components.GameState;
import uk.co.petertribble.sphaero2.components.GameStateContext;
import uk.co.petertribble.sphaero2.model.Jigsaw;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

public class PlayState implements GameState {
    private JigsawPanel jigsawPanel;
    private TimeLabel timeLabel;
    private JLabel progressLabel;
    private JPanel panel;

    @Override
    public void enterState(GameStateContext context) {
        Jigsaw jigsaw = new Jigsaw(context.getJigsawParam(), context.getImage(), context.getPieces());
        this.jigsawPanel = new JigsawPanel(jigsaw);

        this.panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(jigsawPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
        panel.add(createStatusBar(jigsaw), BorderLayout.SOUTH);
        panel.add(createToolBar(), BorderLayout.NORTH);

        jigsawPanel.setProgressLabel(progressLabel);
        jigsawPanel.setTimeLabel(timeLabel);

        SwingUtilities.invokeLater(() -> {
            this.jigsawPanel.animateAllToPuzzlePositions(500);
        });
        jigsawPanel.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                jigsawPanel.requestFocus();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {

            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }
        });
    }

    @Override
    public void exitState() {

    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    private JPanel createStatusBar(Jigsaw jigsaw) {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.timeLabel = new TimeLabel();
        this.progressLabel = new JLabel("Progress: 0% (5/5)");
        statusBar.add(progressLabel);
        statusBar.add(Box.createHorizontalStrut(2));
        statusBar.add(timeLabel);
        JButton save = new JButton(new SaveAction(jigsaw));
        statusBar.add(Box.createHorizontalStrut(2));
        statusBar.add(save);

        statusBar.add(new MemoryMonitorPanel());

        return statusBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();

        toolbar.add(new JToggleButton(new ToolbarAction("toggleSelection", e -> jigsawPanel.setSelectionMode(((JToggleButton)e.getSource()).getModel().isSelected()))));
        toolbar.add(new JButton(new ToolbarAction("stack", e -> jigsawPanel.stack())));
        toolbar.add(new JButton(new ToolbarAction("disperse", e -> jigsawPanel.shuffleSelection())));
        toolbar.add(new JButton(new ToolbarAction("clear", e -> jigsawPanel.clearSelection())));
        toolbar.add(new JButton(new ToolbarAction("arrange", e -> jigsawPanel.arrange())));
        toolbar.add(new JButton(new ToolbarAction("repaint", e -> jigsawPanel.repaintPieces())));

        return toolbar;
    }

}
