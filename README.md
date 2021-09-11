# (Progressive) GIAnt for Geospatial Interlinking

The code consists of two parts: one for the serial and one for the parallel execution. 

# Serial Processing 

## (batch) GIA.nt

To run get the performance reported in **Table 3** and **Figure 6**, execute the class experiments/BatchGIAnt.java .

To get the statistics reported in **Table 4**, execute the class experiments/Table4.java.

## RADON

To reproduce the performance of RADON, please use the original implementation available as part of the **LIMES** tool that is available [here](https://github.com/dice-group/LIMES).

## Progressive GIA.nt

To get the performance reported in **Figures 6** and **7**, execute the class experiments/ProgressiveGIAnt.java.

## Progressive RADON

To get the performance reported in **Figure 6**, execute the class experiments/ProgressiveRADON.java.

# Parallel Processing

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


