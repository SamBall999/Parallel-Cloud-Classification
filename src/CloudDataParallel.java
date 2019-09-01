



import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.ForkJoinPool;


/**
 * Using given data on wind and lift, computes prevailing wind direction and cloud type per gridpoint
 *
 *<p>
 *Uses thread classes in order to parallelize the operations. Incorporates timing mechanisms used in benchmarking.
 *</p>
 *@author Samantha Ball
 *@version 1.0
 *@since 1
 */
public class CloudDataParallel {

 Vector<Float> [][][] advection; // in-plane regular grid of wind vectors, that evolve over time
 float [][][] convection; // vertical air movement strength, that evolves over time
 int [][][] classification; // cloud type per grid point, evolving over time
 int dimx, dimy, dimt; // data dimensions

 //used to time sequential program - must test across a range of input sizes
 static long startTime = 0;
 static final ForkJoinPool fjPool = new ForkJoinPool();


 private static void tick(){
	 startTime = System.currentTimeMillis();
 }
 private static float tock(){
	 return (System.currentTimeMillis() - startTime) / 1000.0f;
 }



 public Vector<Double> sum(Vector<Float>[][][] values){
 int numElements = dim();
 return fjPool.invoke(new SumArray(values, 0, numElements)); //watch out for length function
 }

 // overall number of elements in the timeline grids
 public int dim(){
	 return dimt*dimx*dimy;
 }

 /**
  * Converts linear position into 3D location in simulation grid
  *
  *@param pos Linear position to be converted to a grid point
  *@param ind Integer array to hold the corresponding grid indices
  */
 public void locate(int pos, int [] ind)
 {
	 ind[0] = (int) pos / (dimx*dimy); // t
	 ind[1] = (pos % (dimx*dimy)) / dimy; // x
	 ind[2] = pos % (dimy); // y
 }

 /**
  * Reads in text file and inserts data into corresponding 3D arrays
  *
  *@param filename Name of file to read data from
  */
 public void readData(String fileName){
	 try{
		 Scanner sc = new Scanner(new File(fileName), "UTF-8");

		 // input grid dimensions and simulation duration in timesteps
		 dimt = sc.nextInt();
		 dimx = sc.nextInt();
		 dimy = sc.nextInt();

		 // initialize and load advection (wind direction and strength) and convection
		 advection = new Vector[dimt][dimx][dimy];
		 convection = new float[dimt][dimx][dimy];
		 for(int t = 0; t < dimt; t++)
			 for(int x = 0; x < dimx; x++)
				 for(int y = 0; y < dimy; y++){
					 advection[t][x][y] = new Vector<Float>();
					 advection[t][x][y].add(sc.nextFloat());
					 advection[t][x][y].add(sc.nextFloat());
					 convection[t][x][y] = sc.nextFloat();
				 }

		 classification = new int[dimt][dimx][dimy];
		 sc.close();
	 }
	 catch (IOException e){
		 System.out.println("Unable to open input file "+fileName);
		 e.printStackTrace();
	 }
	 catch (java.util.InputMismatchException e){
		 System.out.println("Malformed input file "+fileName);
		 e.printStackTrace();
	 }
 }

 /**
  * Writes output data to text file
  *
  *@param filename Name of file to write data to
  *@param wind Vector containing average wind values
  */
 public void writeData(String fileName, Vector wind){
		try{
			FileWriter fileWriter = new FileWriter(fileName);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.printf("%d %d %d\n", dimt, dimx, dimy);
			printWriter.printf("%f %f\n", wind.get(0), wind.get(1));

			for(int t = 0; t < dimt; t++){
				for(int x = 0; x < dimx; x++){
				 for(int y = 0; y < dimy; y++){
					 printWriter.printf("%d ", classification[t][x][y]);
				 }
				}
				printWriter.printf("\n");
				}

			printWriter.close();
		}
		catch (IOException e){
			System.out.println("Unable to open output file "+fileName);
			 e.printStackTrace();
		}

 }



 public Vector<Double> findAverage()
 {
	 //calculate  average wind vector for all air layer elements and time steps
	 //return vector with average x and y values
	 Vector<Double> wind = new Vector();
   Vector<Double> sums = sum(advection);
	 double xsum = sums.get(0);
	 double ysum = sums.get(1);
	 //xsum = sum(advection);
	 //sum all y wind values
	 //ysum = sum(advection);
	 //divide by number of entries/grid points
	 int numPoints = dim();
	 double xav = xsum/numPoints;
	 double yav = ysum/numPoints;
	 //OR make vector inside summing function??
	 //add values to wind vector
	 wind.add(xav);
	 wind.add(yav);
	 return wind;

 }




 void getClouds()
 {
			 int numElements = dim();
			 fjPool.invoke(new WriteClouds(classification, advection, convection, 0, numElements)); //put return back if necessary
 }


 public static void main(String[] args)
 {
	 CloudData cd = new CloudData(); //create CloudData object
	 cd.readData(args[0]); //read in data from input file
	 tick();
	 Vector<Double> wind = cd.findAverage(); //find prevailing wind vector
	 //loop through all points and call findCloud for each one
	 cd.getClouds();
	 float time = tock();
	 System.out.println("Run took "+ time +" seconds");
   tick();
   wind = cd.findAverage(); //find prevailing wind vector
  //loop through all points and call findCloud for each one
   cd.getClouds();
   time = tock();
   System.out.println("Second run took "+ time +" seconds");
	 cd.writeData(args[1], wind); // write data to output file
 }
}
