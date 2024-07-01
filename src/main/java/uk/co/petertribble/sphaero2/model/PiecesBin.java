package uk.co.petertribble.sphaero2.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class PiecesBin {
    private List<Piece> pieces;
    private AtomicInteger idProvider = new AtomicInteger();

    public PiecesBin() {
        this.pieces = new ArrayList<>();
    }

    public PiecesBin(List<Piece> pieces) {
        this.pieces = Objects.requireNonNull(pieces);
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public Supplier<Integer> getIdProvider() {
        return idProvider::getAndIncrement;
    }

    public void setPieces(List<Piece> pieces) {
        this.pieces = pieces;
        // find last piece id and set idProvider to the next id
        int maxId = pieces.stream().mapToInt(Piece::getId).max().orElse(0);
        idProvider.set(maxId);
    }

    public void shuffle(int width, int height) {
        List<Piece> pieces = this.pieces;
        this.pieces = new ArrayList<>();
        Random random = new Random();
        for (Piece piece : pieces) {
            piece.setPuzzlePosition(
                    random.nextInt(width - piece.getCurrentWidth()),
                    random.nextInt(height - piece.getCurrentHeight()));
            this.pieces.add(piece);
        }
        Collections.shuffle(this.pieces);
    }

    /**
     * Push the top piece (at the front) to the bottom (the back).
     */
    public void push() {
        Piece p = pieces.remove(pieces.size() - 1);
        pieces.add(0, p);
    }

    public int getWidth() {
        return pieces.stream().mapToInt(p -> p.getRotatedX() + p.getCurrentWidth()).max().orElse(100);
    }

    public int getHeight() {
        return pieces.stream().mapToInt(p -> p.getRotatedY() + p.getCurrentHeight()).max().orElse(100);
    }
}
