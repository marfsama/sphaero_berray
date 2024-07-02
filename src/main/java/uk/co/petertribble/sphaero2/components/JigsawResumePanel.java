package uk.co.petertribble.sphaero2.components;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JigsawResumePanel extends JPanel implements ActionListener {
    private Path folder;
    private ActionListener actionListener;

    public JigsawResumePanel(Path folder) {
        this.folder = folder;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        try {
            Files.list(folder)
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            JigsawResumeJigsawPanel comp = new JigsawResumeJigsawPanel(path);
                            comp.addActionListener(JigsawResumePanel.this);
                            add(comp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, e.getID(), e.getActionCommand()));
        }
    }
}
