package uk.co.petertribble.sphaero2.model;

import uk.co.petertribble.sphaero2.cutter.JigsawCutter;

import java.io.File;

public class JigsawParam {
    private JigsawCutter cutter;
    private int pieces;
    private File filename;

    public JigsawParam() {
    }

    public JigsawParam(JigsawParam other) {
        this.cutter = other.cutter;
        this.pieces = other.pieces;
        this.filename = other.filename;
    }

    public JigsawCutter getCutter() {
        return cutter;
    }

    public void setCutter(JigsawCutter cutter) {
        this.cutter = cutter;
    }

    public int getPieces() {
        return pieces;
    }

    public void setPieces(int pieces) {
        this.pieces = pieces;
    }

    public File getFilename() {
        return filename;
    }

    public void setFilename(File filename) {
        this.filename = filename;
    }
}
