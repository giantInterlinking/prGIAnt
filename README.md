# (Progressive) GIAnt for Geospatial Interlinking

The code consists of two parts: one for the serial and one for the parallel execution. 

Below, we explain how to repeat the experiments of our paper on ["Progressive Geospatial Interlinking"](http://cgi.di.uoa.gr/~koubarak/publications/2021/GeospatialInterlinking.pdf).

The datasets we used for evaluationg of both execution modes are available [here](http://spatialhadoop.cs.umn.edu/datasets.html).

# Serial Processing 

## (batch) GIA.nt

To run get the performance reported in **Table 3** and **Figure 6**, execute the class [experiments/BatchGIAnt.java](serial/src/experiments/BatchGIAnt.java).

To get the statistics reported in **Table 4**, execute the class [experiments/Table4.java](serial/src/experiments/Table4.java).

## RADON

To reproduce the performance of RADON, please use the original implementation available as part of the **LIMES** tool that is available [here](https://github.com/dice-group/LIMES).

## Progressive RADON

To get the performance reported in **Figure 6**, execute the class [experiments/ProgressiveRADON.java](serial/src/experiments/ProgressiveRADON.java).

## Progressive GIA.nt

To get the performance reported in **Figures 6** and **7**, execute the class [experiments/ProgressiveGIAnt.java](/serial/src/experiments/ProgressiveGIAnt.java).

## Dynamic Progressive GIA.nt

We have extended the original approach of Progressive GIA.nt in two ways.

**Dynamic Progressive GIA.nt** updates the weights of the top-BU candidate pairs during the Verification step, whenever a new qualifying pair is detected. To get its performance, execute the class [experiments/DynamicProgressiveGIAnt.java](serial/src/experiments/DynamicProgressiveGIAnt.java).

**Composite Dynamic Progressive GIA.nt** extends Dynamic Progressive GIA.nt using a composite weighting scheme that consists of a primary and a secondary one. The former defines the processing order of the top-weighted pairs, while the latter (usually MBR Overlap) breaks the ties. To get its performance, execute the class [experiments/CompositeDynamicProgressiveGIAnt.java](serial/src/experiments/CompositeDynamicProgressiveGIAnt.java).

# Parallel Processing

The latest code along with more detailed instructions for Parallel Processing is available [here](https://github.com/GiorgosMandi/DS-JedAI/tree/master/TSAS-Experiments).

## Build

	sbt assembly

## Run 

In order to run, it requires a `yaml` configuration file specifying the properties of the execution, that looks like this:

	source:
	  path: "/path_to/dataset1"
	  realIdField: "id_field"
	  geometryField: "geometry_fiels"

	target:
	  path: "/path_to/dataset2"
	  realIdField: "id_field"
	  geometryField: "geometry_fiels"

	relation: "intersects" #ignore

	configurations:
	  theta_measure: "avg" # specifying lat/long granularity, it can be min, max, avg and avg2
	  matchingAlg: "GIANT" # GIANT or PROGRESSIVE_GIANT
	  gridType: "QUADTREE" # QUADTREE or KDBTREE
	  weighting_strategy: "JS" # can be CBS, JS, PEARSON_X2


To run execute:

	spark-submit --master <spark-master> --class experiments.De9ImExp  target/path_to/DS-JedAI-assembly-0.1.jar -conf /path_to/configuration.yaml


