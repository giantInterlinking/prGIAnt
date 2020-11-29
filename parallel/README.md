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
