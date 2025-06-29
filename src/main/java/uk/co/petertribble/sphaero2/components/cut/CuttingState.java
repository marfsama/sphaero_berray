package uk.co.petertribble.sphaero2.components.cut;

import uk.co.petertribble.sphaero2.components.GameState;
import uk.co.petertribble.sphaero2.components.GameStateContext;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;

import javax.swing.*;
import java.awt.*;

public class CuttingState implements GameState {
    private CuttingPanel panel;

    @Override
    public void enterState(GameStateContext context) {
        panel = new CuttingPanel();
        JigsawCutter cutter = context.getJigsawParam().getCutter();
        cutter.setProgressListener();
    }

    @Override
    public void exitState() {
        panel = null;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }
}
