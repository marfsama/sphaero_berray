package uk.co.petertribble.sphaero2.components.play;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ToolbarAction extends AbstractAction {
    private Consumer<ActionEvent>  actionMethod;

    public ToolbarAction(String name) {
        super(name);
    }

    public ToolbarAction(String name, Consumer<ActionEvent> actionMethod) {
        super(name);
        this.actionMethod = actionMethod;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (actionMethod != null) {
            actionMethod.accept(e);
        }
    }
}
