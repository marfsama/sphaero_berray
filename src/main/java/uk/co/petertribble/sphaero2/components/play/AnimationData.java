package uk.co.petertribble.sphaero2.components.play;

public class AnimationData {
    private final int startX, startY;
    private final int endX, endY;
    private final long startTime;
    private final long duration;

    public AnimationData(int startX, int startY, int endX, int endY, long duration) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }
}
