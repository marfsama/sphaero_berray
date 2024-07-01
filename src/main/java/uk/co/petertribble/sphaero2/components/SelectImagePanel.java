package uk.co.petertribble.sphaero2.components;


import uk.co.petertribble.sphaero2.JigsawFrame;
import uk.co.petertribble.sphaero2.cutter.*;
import uk.co.petertribble.sphaero2.model.Jigsaw;
import uk.co.petertribble.sphaero2.model.JigsawParam;
import uk.co.petertribble.sphaero2.model.MultiPiece;
import uk.co.petertribble.sphaero2.model.Piece;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.READ;

/**
 * Panel to select the image to cut together with some configurations.
 */
public class SelectImagePanel extends JPanel implements ActionListener {
    // for the interactive prompt
    private static final Color HELP_COLOR = new Color(100, 100, 150);
    private static final int DEFAULT_PIECES = JigsawCutter.DEFAULT_PIECES;

    public static final String JIGSAW_PARAMS = "jigsawParams";
    public static final String JIGSAW = "jigsaw";
    private JigsawParam jigsawParams;

    private JTextField imageField;
    private JButton browseButton;
    private JComboBox<JigsawCutter> cutterCBox;
    private JSpinner pieceSpinner;
    private JLabel cutterDescLabel;
    private JButton okButton;


    public SelectImagePanel() {
        this.jigsawParams = new JigsawParam();
        jigsawParams.setCutter(JigsawCutter.cutters[0]);
        jigsawParams.setPieces(DEFAULT_PIECES);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPane = this;
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));

        imageField = new JTextField();
        imageField.setText(getCurrentPath());
        imageField.selectAll();

        browseButton = new JButton("Browse...");
        browseButton.setMnemonic(KeyEvent.VK_B);
        browseButton.addActionListener(this);

        JLabel imageLabel = createHelpLabel("<html>"
                + "If this is an image file, it is used to create the puzzle. "
                + "If it is a folder, an image file is selected from it "
                + "(including any subfolders) at random.");

        JPanel imageBPane = new JPanel();
        imageBPane.setLayout(new BoxLayout(imageBPane, BoxLayout.LINE_AXIS));
        imageBPane.add(imageField);
        imageBPane.add(Box.createRigidArea(new Dimension(10, 0)));
        imageBPane.add(browseButton);

        JPanel imagePane = new JPanel(new BorderLayout());
        imagePane.setBorder(createTitledBorder("Find an image"));
        imagePane.add(imageBPane, BorderLayout.NORTH);
        imagePane.add(imageLabel, BorderLayout.CENTER);

        cutterCBox = new JComboBox<>(JigsawCutter.cutters);
        cutterCBox.setSelectedItem(jigsawParams.getCutter());
        cutterCBox.addActionListener(this);

        cutterDescLabel = createHelpLabel(null);
        JPanel cutterPane = new JPanel(new BorderLayout());
        cutterPane.add(cutterCBox, BorderLayout.NORTH);
        cutterPane.add(cutterDescLabel, BorderLayout.CENTER);
        cutterPane.setBorder(createTitledBorder("Piece Style"));
        fireCutterChanged();

        pieceSpinner = new JSpinner(new SpinnerNumberModel(
                DEFAULT_PIECES, JigsawCutter.MIN_PIECES,
                JigsawCutter.MAX_PIECES, 1));
        JLabel pieceLabel = createHelpLabel("<html>"
                + " The puzzle will have roughly this many pieces.");
        JPanel piecePane = new JPanel(new BorderLayout());
        piecePane.add(pieceSpinner, BorderLayout.NORTH);
        piecePane.add(pieceLabel, BorderLayout.CENTER);
        piecePane.setBorder(createTitledBorder("Piece Count"));
        pieceSpinner.addChangeListener(event -> jigsawParams.setPieces((Integer) pieceSpinner.getValue()));

        JPanel okPanel = new JPanel();
        okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.LINE_AXIS));
        okPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        okPanel.add(Box.createHorizontalGlue());
        okButton = new JButton("Start Puzzling");
        okButton.setMnemonic(KeyEvent.VK_K);
        okPanel.add(okButton);
        okButton.addActionListener(this);

        SamplePanel sPanel = new SamplePanel(imageField);
        if (sPanel.samplesValid()) {
            JPanel samplePane = new JPanel(new BorderLayout());
            samplePane.setBorder(createTitledBorder("Select an image"));
            samplePane.add(new JScrollPane(sPanel));
            mainPane.add(samplePane);
        }

        JPanel resumePane = new JPanel(new BorderLayout());
        resumePane.add(new JButton(new LoadAction()), BorderLayout.NORTH);
        resumePane.setBorder(createTitledBorder("Resume"));


        mainPane.add(imagePane);
        mainPane.add(piecePane);
        mainPane.add(cutterPane);
        mainPane.add(resumePane);
        mainPane.add(okPanel);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    /**
     * Returns the folder corresponding to whatever's currently displayed in
     * imageField.
     */
    private File getCurrentFolder() {
        String text = imageField.getText().trim();
        return new File((text.length() == 0) ? "." : text);
    }

    /**
     * Lame way of getting the current path in a way that guarantees an
     * answer returned, and no exception thrown.  Could still throw a
     * SecurityException, but I don't care.
     */
    private String getCurrentPath() {
        File folder = getCurrentFolder();
        try {
            return folder.getCanonicalPath();
        } catch (IOException ex) {
            return folder.getAbsolutePath();
        }
    }

    private JLabel createHelpLabel(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 1));
        label.setForeground(HELP_COLOR);
        return label;
    }

    private Border createTitledBorder(String title) {
        Border outer = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title);
        Border inner = BorderFactory.createEmptyBorder(2, 5, 5, 5);
        return BorderFactory.createCompoundBorder(outer, inner);
    }

    private void fireCutterChanged() {
        JigsawCutter cutter = (JigsawCutter) cutterCBox.getSelectedItem();
        cutterDescLabel.setText("<html>" + cutter.getDescription());
        jigsawParams.setCutter(cutter);
    }

    private void fireBrowseAction() {
        JFileChooser chooser = new JFileChooser(getCurrentFolder());
        chooser.setFileFilter(new JigFileFilter());
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAccessory(new ImagePreview(chooser));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imageField.setText(chooser.getSelectedFile().getAbsolutePath());
            jigsawParams.setFilename(chooser.getSelectedFile());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browseButton) {
            fireBrowseAction();
        } else if (e.getSource() == cutterCBox) {
            fireCutterChanged();
        } else if (e.getSource() == okButton) {
            firePropertyChange(JIGSAW_PARAMS, jigsawParams, new JigsawParam(jigsawParams));
        }
    }

    public class LoadAction extends AbstractAction {

        public LoadAction() {
            super("load last jigsaw");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            Path outPath = Path.of(System.getProperty("user.home"), ".sphaero");
            if (!Files.exists(outPath)) {
                System.out.println("no current save state");
                return;
            }
            JigsawParam params = new JigsawParam();
            try {
                BufferedImage originalImage = ImageIO.read(outPath.resolve("source.png").toFile());
                Map<Integer, Piece> pieces = new LinkedHashMap<>();
                Map<Integer, java.util.List<Integer>> neighbours = new HashMap<>();
                Map<Integer, java.util.List<Integer>> multipieces = new HashMap<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(outPath.resolve("save.txt"), READ)));
                     ImageInputStream piecesData = new MemoryCacheImageInputStream(Files.newInputStream(outPath.resolve("pieces.bin"), READ))
                ) {
                    java.util.List<String> lines = reader.lines()
                            .filter(line -> line.trim().length() > 0)
                            .filter(line -> !line.startsWith("#")).collect(Collectors.toList());
                    for (String line : lines) {
                        if (line.startsWith("file: ")) {
                            params.setFilename(new File(line.substring("file: ".length())));
                        } else if (line.startsWith("pieces: ")) {
                            params.setPieces(Integer.parseInt(line.substring("pieces: ".length())));
                        } else if (line.startsWith("cutter: ")) {
                            String cutterName = line.substring("cutter: ".length());
                            for (var cutter : JigsawCutter.cutters) {
                                if (cutterName.equals(cutter.getName())) {
                                    params.setCutter(cutter);
                                }
                            }
                        } else if (line.startsWith("piece: ")) {
                            String[] stringValues = line.substring("piece: ".length()).split(", *");
                            java.util.List<Integer> integers = Arrays.stream(stringValues).map(Integer::parseInt).collect(Collectors.toList());
                            if (integers.size() < 9) {
                                System.out.println("illegal piece line: " + line);
                                return;
                            }
                            int id = integers.get(0);
                            int imageX = integers.get(1);
                            int imageY = integers.get(2);
                            int imageWidth = integers.get(3);
                            int imageHeight = integers.get(4);
                            int puzzleX = integers.get(5);
                            int puzzleY = integers.get(6);
                            int rotation = integers.get(7);
                            int multipieceid = integers.get(8);
                            java.util.List<Integer> neighbourIds = integers.subList(9, integers.size());
                            int[] pieceData = new int[imageWidth * imageHeight];
                            piecesData.readFully(pieceData, 0, pieceData.length);

                            Piece piece = new Piece(id, pieceData, imageX, imageY, imageWidth, imageHeight, originalImage.getWidth(), originalImage.getHeight(), rotation);
                            piece.setPuzzlePosition(puzzleX, puzzleY);
                            pieces.put(id, piece);
                            neighbours.put(id, neighbourIds);
                            if (multipieceid > -1) {
                                multipieces.computeIfAbsent(multipieceid, k -> new ArrayList<>()).add(id);
                            }
                        } else if (line.startsWith("multipiece: ")) {
                            String[] stringValues = line.substring("multipiece: ".length()).split(", *");
                            java.util.List<Integer> integers = Arrays.stream(stringValues).map(Integer::parseInt).collect(Collectors.toList());
                            if (integers.size() < 8) {
                                System.out.println("illegal multipiece line: " + line);
                                return;
                            }
                            int id = integers.get(0);
                            int imageX = integers.get(1);
                            int imageY = integers.get(2);
                            int imageWidth = integers.get(3);
                            int imageHeight = integers.get(4);
                            int puzzleX = integers.get(5);
                            int puzzleY = integers.get(6);
                            int rotation = integers.get(7);
                            java.util.List<Integer> neighbourIds = integers.subList(8, integers.size());
                            neighbours.put(id, neighbourIds);

                            // the sub pieces should already be read.
                            Set<Piece> subPieces = new HashSet<Piece>();
                            java.util.List<Integer> subPieceIds = multipieces.get(id);
                            if (subPieceIds == null) {
                                System.out.println("multipiece " + id + " does not have subpieces");
                                return;
                            }
                            // add sub piece to multipiece
                            for (Integer subPieceId : subPieceIds) {
                                subPieces.add(pieces.get(subPieceId));
                            }

                            MultiPiece multiPiece = new MultiPiece(subPieces, imageX, imageY, imageWidth, imageHeight, originalImage.getWidth(), originalImage.getHeight(), rotation);
                            multiPiece.setPuzzlePosition(puzzleX, puzzleY);
                            pieces.put(id, multiPiece);
                        }
                    }
                    // post processing: add neighbours to pieces
                    for (int pieceId : neighbours.keySet()) {
                        Piece piece = pieces.get(pieceId);
                        for (int neighbourId : neighbours.get(pieceId)) {
                            piece.getNeighbors().add(pieces.get(neighbourId));
                        }
                    }
                    // post processing: remove pieces which are already part of a multipiece
                    for (int multipieceId : multipieces.keySet()) {
                        for (int subpieceId : multipieces.get(multipieceId)) {
                            pieces.remove(subpieceId);
                        }
                    }
                    List<Piece> finalPieces = new ArrayList<>(pieces.values());
                    Jigsaw jigsaw = new Jigsaw(params, originalImage);
                    jigsaw.getPieces().setPieces(finalPieces);
                    SelectImagePanel.this.firePropertyChange(JIGSAW, null, jigsaw);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
