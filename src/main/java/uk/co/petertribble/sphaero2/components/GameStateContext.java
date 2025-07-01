package uk.co.petertribble.sphaero2.components;

import uk.co.petertribble.sphaero2.cutter.JigsawCutter;
import uk.co.petertribble.sphaero2.model.JigsawParam;
import uk.co.petertribble.sphaero2.model.Piece;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared state and state management.
 */
public class GameStateContext {
    private JigsawParam jigsawParam;
    /** Listener which will be informed when a new GameState is set. */
    private Consumer<GameState> gameStateListener;
    private GameState currentGameState;
    private BufferedImage image;
    private List<Piece> pieces;

    private Dimension contentPaneSize;

    public GameStateContext() {
        this.jigsawParam = new JigsawParam();
        jigsawParam.setCutter(JigsawCutter.cutters[0]);
        jigsawParam.setPieces(JigsawCutter.DEFAULT_PIECES);
    }

    public JigsawParam getJigsawParam() {
        return jigsawParam;
    }

    public void setJigsawParam(JigsawParam jigsawParam) {
        this.jigsawParam = jigsawParam;
    }

    /**
     * changes the game state to the requested state.
     */
    public void changeState(GameState gameState) {
        if (currentGameState != null) {
            currentGameState.exitState();
        }
        gameState.enterState(this);
        this.currentGameState = gameState;
        if (gameStateListener != null) {
            gameStateListener.accept(gameState);
        }
    }

    public void addGameStateListener(Consumer<GameState> gameStateListener) {
        this.gameStateListener = gameStateListener;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setPieces(List<Piece> pieces) {
        this.pieces = pieces;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public Dimension getContentPaneSize() {
        return contentPaneSize;
    }

    public void setContentPaneSize(Dimension contentPaneSize) {
        this.contentPaneSize = contentPaneSize;
    }
}
