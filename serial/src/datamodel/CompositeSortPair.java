package datamodel;

import java.io.Serializable;
import org.locationtech.jts.geom.Geometry;

public class CompositeSortPair implements Serializable, Comparable<CompositeSortPair> {

    private static final long serialVersionUID = 723425435776147L;

    private final int id;
    private final int entityId1;
    private final int entityId2;
    private int relatedMatches;

    private final float mainWeight;
    private final float secondaryWeight;

    private final Geometry targetGeometry;

    public CompositeSortPair(int id, int id1, int id2, float w1, float w2, Geometry tG) {
        this.id = id;
        entityId1 = id1;
        entityId2 = id2;
        mainWeight = w1;
        secondaryWeight = w2;
        relatedMatches = 0;
        targetGeometry = tG;
    }

    @Override
    public int compareTo(CompositeSortPair o) {
        if (o.getId() == id) {
            return 0;
        }

        double test1 = o.getMainWeight() - this.getMainWeight();
        if (0 < test1) {
            return -1;
        }

        if (test1 < 0) {
            return 1;
        }

        double test2 = o.getSecondaryWeight() - this.getSecondaryWeight();
        if (0 < test2) {
            return -1;
        }

        if (test2 < 0) {
            return 1;
        }
        
        return o.getId() - id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompositeSortPair other = (CompositeSortPair) obj;
        return this.id == other.id;
    }

    public int getEntityId1() {
        return entityId1;
    }

    public int getEntityId2() {
        return entityId2;
    }

    public int getId() {
        return id;
    }

    public float getMainWeight() {
        return mainWeight * (1 + relatedMatches);
    }

    public float getSecondaryWeight() {
        return secondaryWeight * (1 + relatedMatches);
    }

    public Geometry getTargetGeometry() {
        return targetGeometry;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + this.id;
        return hash;
    }

    public void incrementRelatedMatches() {
        relatedMatches++;
    }

    @Override
    public String toString() {
        return "E1 : " + entityId1 + ", E2 : " + entityId2 + ", main weight : " + getMainWeight() + ", secondary weight : " + getSecondaryWeight();
    }
}
