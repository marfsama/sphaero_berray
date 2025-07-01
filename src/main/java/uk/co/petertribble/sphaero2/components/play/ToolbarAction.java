package uk.co.petertribble.sphaero2.components.play;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ToolbarAction extends AbstractAction {
    private Runnable actionMethod;

    public ToolbarAction(String name) {
        super(name);
    }

    public ToolbarAction(String name, Runnable actionMethod) {
        super(name);
        this.actionMethod = actionMethod;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (actionMethod != null) {
            actionMethod.run();
        }
    }
}
