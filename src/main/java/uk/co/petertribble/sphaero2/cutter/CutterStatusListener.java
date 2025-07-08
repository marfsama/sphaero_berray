package uk.co.petertribble.sphaero2.cutter;

import uk.co.petertribble.sphaero2.model.Piece;
import uk.co.petertribble.sphaero2.model.PiecesBin;

/** Callback listener so interested parties can watch the progress of the cutter. */
public interface CutterStatusListener {
    /** Starts a cutting step. */
    void startStep(String step, int maxValue);
    /** Progresses the current cutting step. */
    void progress(int progress);
    /** Eject a piece which was cutted. This piece does not need to be fully configured, just the image and the image position must be set. */
    void ejectPiece(Piece piece);

    /** Called when the cutting is finished. */
    void done(PiecesBin pieces);

}
