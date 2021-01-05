# Parallel-Cloud-Classification

This program classifies cloud type based on simulated weather data using the Java Fork/Join framework.


## Use

If the program has not been compiled:
- navigate to the root directory and use make to generate the necessary class files. 

From the root directory:
- javadocs can be generated and removed using the make docs and make cleandocs commands respectively
- class files can also be removed using make clean. 

Before running program:
- The desired input data file and correct output file should be placed in the bin directory. 
- A output file out_example.txt has been included in the bin folder for reference.

Running the program:
In order to run the program with the basic functionality, the following command is run from within the bin folder:

         java CloudData [input data file] [output data file] [correct output data file]


In order to run the program in benchmarking mode, which tests both sequential and parallel, as well as varying the data size and sequential cut-off parameters, the command is adjusted by adding the -t flag:

       java CloudData [input data file] [output data file] [correct output data file] -t
       
## Analysis
A full analysis of the parallel speed-up is included in the accompanying report. 
