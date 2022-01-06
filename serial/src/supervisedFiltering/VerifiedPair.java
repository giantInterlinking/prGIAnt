package supervisedFiltering;

/**
 *
 * @author Georgios
 */
public class VerifiedPair {

    private final int sourceId;
    private final int targetId;

    public VerifiedPair(int sId, int tId) {
        sourceId = sId;
        targetId = tId;
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
        final VerifiedPair other = (VerifiedPair) obj;
        if (this.getSourceId() != other.getSourceId()) {
            return false;
        }
        if (this.getTargetId() != other.getTargetId()) {
            return false;
        }
        return true;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getTargetId() {
        return targetId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + this.getSourceId();
        hash = 71 * hash + this.getTargetId();
        return hash;
    }
}
