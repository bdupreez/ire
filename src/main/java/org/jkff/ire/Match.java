package org.jkff.ire;

/**
 * Created on: 22.07.2010 23:25:29
 */
public class Match {
    private final int whichPattern;
    private final int startPos;
    private final int length;

    public Match(final int whichPattern, final int startPos, final int length) {
        this.whichPattern = whichPattern;
        this.startPos = startPos;
        this.length = length;
    }

    public int whichPattern() {
        return whichPattern;
    }

    public int startPos() {
        return startPos;
    }

    public int length() {
        return length;
    }

    public String toString() {
        return "" + whichPattern + "@("+startPos+","+length+")";
    }
}
