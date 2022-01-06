# Additional progressive methods

## Dynamic Progressive GIA.nt

We have extended the original approach of Progressive GIA.nt in two ways.

**Dynamic Progressive GIA.nt** updates the weights of the top-BU candidate pairs during the Verification step, whenever a new qualifying pair is detected. To get its performance, execute the class [experiments/DynamicProgressiveGIAnt.java](serial/src/experiments/DynamicProgressiveGIAnt.java).

**Composite Dynamic Progressive GIA.nt** extends Dynamic Progressive GIA.nt using a composite weighting scheme that consists of a primary and a secondary one. The former defines the processing order of the top-weighted pairs, while the latter (usually MBR Overlap) breaks the ties. To get its performance, execute the class [experiments/CompositeDynamicProgressiveGIAnt.java](serial/src/experiments/CompositeDynamicProgressiveGIAnt.java).
