package uk.co.petertribble.sphaero2.components.select;

import uk.co.petertribble.sphaero2.components.GameState;
import uk.co.petertribble.sphaero2.components.GameStateContext;
import uk.co.petertribble.sphaero2.components.cut.CuttingState;
import uk.co.petertribble.sphaero2.components.load.LoadingState;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class SelectImageState implements GameState {
    private SelectImagePanel panel;
    private JPanel centeredBox;

    @Override
    public void enterState(GameStateContext context) {
        panel = new SelectImagePanel();
        panel.setJigsawParams(context.getJigsawParam());
        panel.setStartListener(e -> {
            if (e.getID() == 1) {
                context.setJigsawParam(panel.getJigsawParams());
                context.changeState(new CuttingState());
            } else if (e.getID() == 2) {
                context.changeState(new LoadingState(Path.of(e.getActionCommand())));
            }
        });
        panel.setMaximumSize(new Dimension(1000, 1000));
        panel.setMinimumSize(new Dimension(1000, 1000));
        panel.setPreferredSize(new Dimension(1000, 1000));
        panel.setBorder(BorderFactory.createRaisedBevelBorder());

        this.centeredBox = new JPanel();
        centeredBox.setLayout(new BoxLayout(centeredBox, BoxLayout.Y_AXIS));
        centeredBox.setBackground(Color.BLACK);
        centeredBox.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        centeredBox.add(Box.createVerticalGlue());
        centeredBox.add(panel);
        centeredBox.add(Box.createVerticalGlue());
    }

    @Override
    public void exitState() {
        // dispose panel
        panel = null;
    }

    @Override
    public JPanel getPanel() {
        return centeredBox;
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("Select Image Game State Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        SelectImageState state = new SelectImageState();
        GameStateContext context = new GameStateContext();
        context.addGameStateListener(System.out::println);
        state.enterState(context);

        frame.setContentPane(state.getPanel());
        frame.pack();

        frame.revalidate();
    }
}
