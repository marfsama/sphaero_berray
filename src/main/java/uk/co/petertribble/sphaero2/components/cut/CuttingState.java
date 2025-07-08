package uk.co.petertribble.sphaero2.components.cut;

import uk.co.petertribble.sphaero2.components.GameState;
import uk.co.petertribble.sphaero2.components.GameStateContext;
import uk.co.petertribble.sphaero2.components.play.PlayState;
import uk.co.petertribble.sphaero2.cutter.CutterStatusListener;
import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.JigsawParam;
import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class CuttingState implements GameState {
    private CuttingPanel panel;

    @Override
    public void enterState(GameStateContext context) {
        panel = new CuttingPanel();
        JigsawParam params = context.getJigsawParam();
        Deque<Piece> pieces = new ConcurrentLinkedDeque<>();
        panel.setPieces(pieces);

        File file = params.getFilename();

        try {
            BufferedImage image = ImageIO.read(file);
            Rectangle rectangle = params.getRectangle();
            if (rectangle != null) {
                image = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
            //BufferedImage resizedImage = JigUtil.resizeImage(image);
            BufferedImage resizedImage = image;
            context.setImage(resizedImage);
            System.out.println("ImageSize: "+getImageSize(resizedImage));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Image file cannot be read.", "Invalid Image", JOptionPane.ERROR_MESSAGE);
        }


        JigsawCutter cutter = params.getCutter();
        cutter.setPreferredPieceCount(params.getPieces());
        CutterStatusListenerImpl statusListener = new CutterStatusListenerImpl(pieces, context);
        cutter.setStatusListener(statusListener);

        new Thread(() -> {
            List<Piece> pieces1 = new ArrayList<>(Arrays.asList(cutter.cut(context.getImage())));

            PiecesBin piecesBin = new PiecesBin(new AtomicInteger(1), "");
            piecesBin.setPieces(pieces1);
            piecesBin.shuffle(new Rectangle(0, 0, piecesBin.getWidth(), piecesBin.getHeight()), true);

            statusListener.done(piecesBin);
        }).start();
    }

    private int getImageSize(BufferedImage image) {
        DataBuffer buff = image.getRaster().getDataBuffer();
        return buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
    }

    @Override
    public void exitState() {
        panel = null;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    private class CutterStatusListenerImpl implements CutterStatusListener {
        private final Deque<Piece> pieces;
        private final GameStateContext context;
        private String step;
        private int max;
        private volatile boolean done = false;

        public CutterStatusListenerImpl(Deque<Piece> pieces, GameStateContext context) {
            this.pieces = pieces;
            this.context = context;
        }

        @Override
        public void startStep(String step, int maxValue) {
            SwingUtilities.invokeLater(() -> {
                this.step = step;
                this.max = maxValue;
                JProgressBar progressBar = panel.getProgressBar();
                progressBar.setString(step + (max > 0 ? " (0/" + maxValue + ")" : ""));
                progressBar.setValue(0);
                progressBar.setMaximum(maxValue);
            });
        }

        @Override
        public void progress(int progress) {
            SwingUtilities.invokeLater(() -> {
                if (!done) {
                panel.getProgressBar().setString(step + (max > 0 ? " (" + progress + "/" + max + ")" : ""));
                panel.getProgressBar().setValue(progress);
                }
            });
        }

        @Override
        public void ejectPiece(Piece piece) {
            pieces.add(piece);
            SwingUtilities.invokeLater(() -> {
                        if (!done) {
                            panel.repaint();
                        }
                    }
            );
        }

        @Override
        public void done(PiecesBin pieces) {
            done = true;
            context.setPieces(pieces);
            context.changeState(new PlayState());

            long imageSize = 0;
            long highlightSize = 0;
            long currentSize = 0;
            // count memory usage of pieces
            for (Piece piece : pieces.getPieces()) {
                imageSize += piece.getOrigData().length*4L;
                currentSize += piece.getCurData().length*4L;
                highlightSize += piece.getHighlightData().length*4L;
            }
            System.out.println("memory usages:");
            System.out.println("image size: "+imageSize);
            System.out.println("highlightSize: "+highlightSize);
            System.out.println("currentSize: "+currentSize);
            System.out.println("total memory: "+(imageSize+highlightSize+currentSize));
        }
    }
}
