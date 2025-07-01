package uk.co.petertribble.sphaero2.components.play;

import uk.co.petertribble.sphaero2.JigUtil;
import uk.co.petertribble.sphaero2.JigsawFrame;
import uk.co.petertribble.sphaero2.model.Jigsaw;
import uk.co.petertribble.sphaero2.model.MultiPiece;
import uk.co.petertribble.sphaero2.model.Piece;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class SaveAction extends AbstractAction {


    private final Jigsaw jigsaw;

    public SaveAction(Jigsaw jigsaw) {
        super("Save");
        this.jigsaw = jigsaw;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (jigsaw != null && !jigsaw.isFinished()) {
            String directory = jigsaw.getParams().getFilename().getName();
            int pos = directory.lastIndexOf(".");
            if (pos > 0 && pos < (directory.length() - 1)) { // If '.' is not the first or last character.
                directory = directory.substring(0, pos);
            }
            Path outPath = Path.of(System.getProperty("user.home"), ".sphaero", directory);
            try {
                Files.createDirectories(outPath);
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(outPath.resolve("save.txt"), WRITE, TRUNCATE_EXISTING, CREATE)));
                     ImageOutputStream piecesData = new MemoryCacheImageOutputStream(Files.newOutputStream(outPath.resolve("pieces.bin"), WRITE, TRUNCATE_EXISTING, CREATE))
                ) {
                    writer.println("file: " + jigsaw.getParams().getFilename().getAbsolutePath());
                    writer.println("pieces: " + jigsaw.getParams().getPieces());
                    writer.println("cutter: " + jigsaw.getParams().getCutter().getName());
                    writer.println("# piece: id, imageX, imageY, imageWidth, imageHeight, puzzleX, puzzleY, rotation, multipieceid, neighbours (list of ids)");
                    writer.println("# multipiece: id, imageX, imageY, imageWidth, imageHeight, puzzleX, puzzleY, rotation");
                    for (Piece piece : jigsaw.getPieces().getPieces()) {
                        for (Piece subPiece : piece.getSubs()) {
                            writer.println("piece: " + subPiece.getId() + ", "
                                    + subPiece.getImageX() + ", " + subPiece.getImageY() + ", "
                                    + subPiece.getImageWidth() + ", " + subPiece.getImageHeight() + ", "
                                    + subPiece.getPuzzleX() + ", " + subPiece.getPuzzleY() + ", "
                                    + subPiece.getRotation() + ", "
                                    + (piece instanceof MultiPiece ? piece.getId() : -1) + ", "
                                    + subPiece.getNeighbors().stream().map(Piece::getId).map(String::valueOf).collect(Collectors.joining(","))
                            );

                            int[] data = subPiece.getData();
                            piecesData.writeInts(subPiece.getData(), 0, data.length);
                        }
                        if (piece instanceof MultiPiece) {
                            MultiPiece subPiece = (MultiPiece) piece;
                            writer.println("multipiece: " + subPiece.getId() + ", "
                                    + subPiece.getImageX() + ", " + subPiece.getImageY() + ", "
                                    + subPiece.getImageWidth() + ", " + subPiece.getImageHeight() + ", "
                                    + subPiece.getPuzzleX() + ", " + subPiece.getPuzzleY() + ", "
                                    + subPiece.getRotation() + ", "
                                    + subPiece.getNeighbors().stream().map(Piece::getId).map(String::valueOf).collect(Collectors.joining(","))
                            );
                        }
                    }
                }
                // write original image
                ImageIO.write(jigsaw.getImage(), "png", outPath.resolve("source.png").toFile());
                BufferedImage thumbnail = JigUtil.resizeImage(jigsaw.getImage(), JigsawFrame.THUMB_WIDTH, JigsawFrame.THUMB_HEIGHT);
                ImageIO.write(thumbnail, "png", outPath.resolve("thumb.png").toFile());

                // write current solve state
                BufferedImage currentState = new BufferedImage(jigsaw.getWidth(), jigsaw.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = currentState.getGraphics();
                for (Piece piece : jigsaw.getPieces().getPieces()) {
                    piece.draw(graphics);
                }
                graphics.dispose();
                BufferedImage currentStateThumbnail = JigUtil.resizeImage(currentState, JigsawFrame.THUMB_WIDTH, JigsawFrame.THUMB_HEIGHT);
                ImageIO.write(currentStateThumbnail, "png", outPath.resolve("state.png").toFile());
                thumbnail.flush();
                currentState.flush();
                currentStateThumbnail.flush();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }
}
