/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package supervisedFiltering;

import org.locationtech.jts.geom.Geometry;

/**
 *
 * @author Georgios
 */
public class SamplePair {
    
    private final int sourceId;
    private final int targetId;
    private final Geometry geometry;
    
    SamplePair(int sId, int tId, Geometry g) {
        sourceId = sId;
        targetId = tId;
        geometry = g;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getTargetId() {
        return targetId;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
