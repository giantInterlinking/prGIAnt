package datamodel;

import java.util.Iterator;

public class PairIterator implements Iterator<Pair> {

    private float executedComparisons;
    private final float totalComparisons;

    private final int innerLimit;
    private final int outerLimit;
    
    private int innerLoop;
    private int outerLoop;

    private final Tile tile;

    PairIterator(Tile tile) {
        this.tile = tile;
        executedComparisons = 0;
        totalComparisons = tile.getNoOfPairs();

        innerLoop = -1; // so that counting in function next() starts from 0
        innerLimit = tile.getTargetGeometries().length - 1;
        outerLoop = 0;
        outerLimit = tile.getSourceGeometries().length - 1;
    }

    @Override
    public boolean hasNext() {
        return executedComparisons < totalComparisons;
    }

    @Override
    public Pair next() {
        if (totalComparisons <= executedComparisons) {
            return null;
        }

        executedComparisons++;
        innerLoop++;
        if (innerLimit < innerLoop) {
            innerLoop = 0;
            outerLoop++;
            if (outerLimit < outerLoop) {
                return null;
            }
        }

        return new Pair(tile.getSourceGeometries()[outerLoop], tile.getTargetGeometries()[innerLoop], 0, null);
    }
}
