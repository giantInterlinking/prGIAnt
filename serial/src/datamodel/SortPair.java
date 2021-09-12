package datamodel;

import java.io.Serializable;
import org.locationtech.jts.geom.Geometry;

public class SortPair implements Serializable, Comparable<SortPair> {

    private static final long serialVersionUID = 723425435776147L;

    private final int id;
    private final int entityId1;
    private final int entityId2;
    private int relatedMatches;
    
    private float weight;

    private final Geometry targetGeometry;
    
    public SortPair (int id, int id1, int id2, float w, Geometry tG) {
        weight = w;
        this.id = id;
        entityId1 = id1;
        entityId2 = id2;
        relatedMatches = 0;
        targetGeometry = tG;
    }
    
    @Override
    public int compareTo(SortPair o) {
        if (o.getId() == id) {
            return 0;
        }

        double test = o.getWeight() - this.getWeight();
        if (0 < test) {
            return -1;
        }

        if (test < 0) {
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
        final SortPair other = (SortPair) obj;
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

    /**
     * @return the targetGeometry
     */
    public Geometry getTargetGeometry() {
        return targetGeometry;
    }
    
    /**
     * Returns the weight between two geometries.
     * Higher weights correspond to stronger likelihood of related entities.
     * @return 
     */
    public float getWeight() {
        if (relatedMatches == 0) {
            return weight;
        }
        return weight * (1 + relatedMatches);
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
    
    public void setWeight(float w) {
        weight = w;
    }
    
    @Override
    public String toString() {
        return "DPG E1 : " + entityId1 + ", E2 : " + entityId2 + ", weight : " + getWeight();
    }
}