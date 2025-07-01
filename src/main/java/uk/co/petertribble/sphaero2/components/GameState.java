package uk.co.petertribble.sphaero2.components;

import javax.swing.*;

public interface GameState {
    void enterState(GameStateContext context);
    void exitState();
    /** returns the content pane for the state */
    JPanel getPanel();
}
