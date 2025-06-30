package uk.co.petertribble.sphaero2.components;

import uk.co.petertribble.sphaero2.components.select.SelectImageState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class JigsawSwingMain extends JFrame {

    private GameStateContext gameStateContext;

    public JigsawSwingMain() throws HeadlessException {
        super("Jigsaw Puzzle");
        initGameStateContext();
        gameStateContext.changeState(new SelectImageState());
    }

    private void initGameStateContext() {
        this.gameStateContext = new GameStateContext();
        this.gameStateContext.addGameStateListener((gameState) -> {
            SwingUtilities.invokeLater(() -> {
                getContentPane().removeAll();
                getContentPane().add(gameState.getPanel());
                pack();
            });
        });
        getContentPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                gameStateContext.setContentPaneSize(e.getComponent().getSize());
            }
        });
    }

    public static void main(String[] args) {
        JigsawSwingMain frame = new JigsawSwingMain();
        frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

}
