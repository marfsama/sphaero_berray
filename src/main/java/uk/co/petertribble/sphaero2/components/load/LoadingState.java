package uk.co.petertribble.sphaero2.components.load;

import uk.co.petertribble.sphaero2.components.GameState;
import uk.co.petertribble.sphaero2.components.GameStateContext;
import uk.co.petertribble.sphaero2.components.play.PlayState;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.READ;

public class LoadingState implements GameState {
    private LoadingPanel panel;

    private final Path savedStatePath;

    public LoadingState(Path savedStatePath) {
        this.savedStatePath = savedStatePath;
    }

    @Override
    public void enterState(GameStateContext context) {
        this.panel = new LoadingPanel();
        Deque<Piece> pieces = new ConcurrentLinkedDeque<>();
        panel.setPieces(pieces);

        new Thread(() -> {
            Jigsaw jigsaw = loadSavedState(savedStatePath);
            context.setJigsawParam(jigsaw.getParams());
            context.setPieces(jigsaw.getPieces());
            context.setImage(jigsaw.getImage());
            context.changeState(new PlayState());
        }).start();
    }

    @Override
    public void exitState() {

    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    private Jigsaw loadSavedState(Path outPath) {
        JigsawParam params = new JigsawParam();
        try {
            long currentDataDuration = 0;
            long bevelDuration = 0;
            long highlightDuration = 0;
            long imageDuration = 0;
            long highlightImageDuration = 0;

            BufferedImage originalImage = ImageIO.read(outPath.resolve("source.png").toFile());
            Map<Integer, Piece> pieces = new LinkedHashMap<>();
            Map<Integer, List<Integer>> neighbours = new HashMap<>();
            Map<Integer, List<Integer>> multipieces = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(outPath.resolve("save.txt"), READ)));
                 ImageInputStream piecesData = new MemoryCacheImageInputStream(Files.newInputStream(outPath.resolve("pieces.bin"), READ))
            ) {
                List<String> lines = reader.lines()
                        .filter(line -> line.trim().length() > 0)
                        .filter(line -> !line.startsWith("#")).collect(Collectors.toList());
                for (String line : lines) {
                    if (line.startsWith("file: ")) {
                        params.setFilename(new File(line.substring("file: ".length())));
                    } else if (line.startsWith("pieces: ")) {
                        params.setPieces(Integer.parseInt(line.substring("pieces: ".length())));
                        panel.getProgressBar().setMaximum(params.getPieces());
                    } else if (line.startsWith("cutter: ")) {
                        String cutterName = line.substring("cutter: ".length());
                        for (var cutter : JigsawCutter.cutters) {
                            if (cutterName.equals(cutter.getName())) {
                                params.setCutter(cutter);
                            }
                        }
                    } else if (line.startsWith("piece: ")) {
                        panel.getProgressBar().setValue(pieces.size());
                        panel.getProgressBar().setString(pieces.size()+" / "+params.getPieces());
                        String[] stringValues = line.substring("piece: ".length()).split(", *");
                        List<Integer> integers = Arrays.stream(stringValues).map(Integer::parseInt).collect(Collectors.toList());
                        if (integers.size() < 9) {
                            System.out.println("illegal piece line: " + line);
                            return null;
                        }
                        int id = integers.get(0);
                        int imageX = integers.get(1);
                        int imageY = integers.get(2);
                        int imageWidth = integers.get(3);
                        int imageHeight = integers.get(4);
                        int puzzleX = integers.get(5);
                        int puzzleY = integers.get(6);
                        int rotation = integers.get(7);
                        int multipieceId = integers.get(8);
                        List<Integer> neighbourIds = integers.subList(9, integers.size());
                        int[] pieceData = new int[imageWidth * imageHeight];
                        piecesData.readFully(pieceData, 0, pieceData.length);

                        Piece piece = new Piece(id, pieceData, imageX, imageY, imageWidth, imageHeight, originalImage.getWidth(), originalImage.getHeight(), rotation);
                        currentDataDuration += piece.currentDataDuration;
                        bevelDuration += piece.bevelDuration;
                        highlightDuration += piece.highlightDuration;
                        imageDuration += piece.imageDuration;
                        highlightImageDuration += piece.highlightImageDuration;
                        piece.setPuzzlePosition(puzzleX, puzzleY);
                        piece.setCurrentPosition(puzzleX, puzzleY);
                        pieces.put(id, piece);
                        neighbours.put(id, neighbourIds);
                        if (multipieceId > -1) {
                            multipieces.computeIfAbsent(multipieceId, k -> new ArrayList<>()).add(id);
                        }
                    } else if (line.startsWith("multipiece: ")) {
                        String[] stringValues = line.substring("multipiece: ".length()).split(", *");
                        List<Integer> integers = Arrays.stream(stringValues).map(Integer::parseInt).collect(Collectors.toList());
                        if (integers.size() < 8) {
                            System.out.println("illegal multipiece line: " + line);
                            return null;
                        }
                        int id = integers.get(0);
                        int imageX = integers.get(1);
                        int imageY = integers.get(2);
                        int imageWidth = integers.get(3);
                        int imageHeight = integers.get(4);
                        int puzzleX = integers.get(5);
                        int puzzleY = integers.get(6);
                        int rotation = integers.get(7);
                        List<Integer> neighbourIds = integers.subList(8, integers.size());
                        neighbours.put(id, neighbourIds);

                        // the sub pieces should already be read.
                        Set<Piece> subPieces = new HashSet<>();
                        List<Integer> subPieceIds = multipieces.get(id);
                        if (subPieceIds == null) {
                            System.out.println("multipiece " + id + " does not have subpieces");
                            return null;
                        }
                        // add sub piece to multipiece
                        for (Integer subPieceId : subPieceIds) {
                            subPieces.add(pieces.get(subPieceId));
                        }

                        MultiPiece multiPiece = new MultiPiece(subPieces, imageX, imageY, imageWidth, imageHeight, originalImage.getWidth(), originalImage.getHeight(), rotation);
                        multiPiece.setPuzzlePosition(puzzleX, puzzleY);
                        multiPiece.setId(id);
                        pieces.put(id, multiPiece);
                    }
                }
                // post processing: add neighbours to pieces
                for (int pieceId : neighbours.keySet()) {
                    Piece piece = pieces.get(pieceId);
                    for (int neighbourId : neighbours.get(pieceId)) {
                        Piece e = pieces.get(neighbourId);
                        if (e == null) {
                            System.out.println("cannot find neighbour " + neighbourId + " of piece " + pieceId);
                        } else {
                            piece.getNeighbors().add(e);
                        }
                    }
                }
                // post processing: remove pieces which are already part of a multipiece
                for (int multipieceId : multipieces.keySet()) {
                    for (int subpieceId : multipieces.get(multipieceId)) {
                        pieces.remove(subpieceId);
                    }
                }
                List<Piece> finalPieces = new ArrayList<>(pieces.values());
                int lastId = finalPieces.stream().mapToInt(Piece::getId).max().orElse(1);
                Jigsaw jigsaw = new Jigsaw(params, originalImage, new PiecesBin(new AtomicInteger(lastId+1), "main", finalPieces));

                System.out.println("Statistics: ");
                System.out.println(" * currentDataDuration: "+currentDataDuration );
                System.out.println(" * bevelDuration: " + bevelDuration);
                System.out.println(" * highlightDuration: " + highlightDuration);
                System.out.println(" * imageDuration: " + imageDuration);
                System.out.println(" * highlightImageDuration: " + highlightImageDuration);


                return jigsaw;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
