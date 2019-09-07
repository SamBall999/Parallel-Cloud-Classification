
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;

import java.util.concurrent.ForkJoinPool;


/**
 * Using given data on wind and lift, computes prevailing wind direction and cloud type per gridpoint using parallelization
 *
 *<p>
 *Uses thread classes in order to parallelize the operations. Incorporates timing mechanisms used in benchmarking.
 *</p>
 *@author Samantha Ball
 *@version 1.0
 *@since 1
 */
public class CloudData {


  Vector<Double> [][][] advection; // in-plane regular grid of wind vectors, that evolve over time
  double [][][] convection; // vertical air movement strength, that evolves over time
  int [][][] classification; // cloud type per grid point, evolving over time
  int dimx, dimy, dimt; // data dimensions
  Vector<Double> pervailingWind;
  //below used to check against correct output
  Vector<Double> checkWind;
  int [][][] checkClassification; // cloud type per grid point, evolving over time
  int dx, dy, dt; // data dimensions of correct output
  //flag to check output is correct
  boolean isCorrect = true;
  static double EPSILON = 0.000001;
  static int datasize = 512;
  static float scalingFactor = 1;

  //used to time sequential program - must test across a range of input sizes
  static long startTime = 0;
  static final ForkJoinPool fjPool = new ForkJoinPool();

  private static void tick(){
    startTime = System.currentTimeMillis();
  }
  private static float tock(){
    return (System.currentTimeMillis() - startTime); // 1000.0f;
  }

  /**
   * Computes overall number of elements in the timeline grids
   *
   *@return Integer value representing the total number of elements in the timeline grids
   */
  public int dim(){
    //System.out.println("no elements" + (int)Math.ceil((dimt*scalingFactor)*(dimx)*(dimy)));
    return (int)Math.ceil((dimt*scalingFactor)*(dimx)*(dimy));
  }

  /**
   * Converts linear position into 3D location in simulation grid
   *
   *@param pos Linear position to be converted to a grid point
   *@param ind Integer array to hold the corresponding grid indices
   */
	void locate(int pos, int [] ind)
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
	void readData(String fileName){
		try{
			Scanner sc = new Scanner(new File(fileName), "UTF-8");

			// input grid dimensions and simulation duration in timesteps
			dimt = sc.nextInt();
			dimx = sc.nextInt();
			dimy = sc.nextInt();

			// initialize and load advection (wind direction and strength) and convection
			advection = new Vector[dimt][dimx][dimy];
			convection = new double[dimt][dimx][dimy];
			for(int t = 0; t < dimt; t++)
				for(int x = 0; x < dimx; x++)
					for(int y = 0; y < dimy; y++){
						advection[t][x][y] = new Vector();
						advection[t][x][y].add(sc.nextDouble());
						advection[t][x][y].add(sc.nextDouble());
						convection[t][x][y] = sc.nextDouble();
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
	void writeData(String fileName, Vector wind){
		 try{
			 FileWriter fileWriter = new FileWriter(fileName);
			 PrintWriter printWriter = new PrintWriter(fileWriter);
			 printWriter.printf("%d %d %d\n", dimt, dimx, dimy);
			 printWriter.printf("%f %f\n", wind.get(0), wind.get(1));

			 for(int t = 0; t < (dimt*scalingFactor); t++){
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

  /**
	* Determines the prevailing wind direction over all timeline grid values
	*
	* @return Vector of doubles representing the average x and y wind values
	*/
  public Vector<Double> findAverage()
  	{
  		//calculate  average wind vector for all air layer elements and time steps
  		//return vector with average x and y values
  		Vector<Double> wind = new Vector();
  		double xsum = 0;
  		double ysum = 0;
  		int numPoints = 0;
  		for(int t = 0; t < (dimt*scalingFactor); t++)
  			for(int x = 0; x < dimx; x++)
  				for(int y = 0; y < dimy; y++){
  					//sum all x wind values
  					xsum += advection[t][x][y].get(0);
  					//sum all y wind values
  					ysum += advection[t][x][y].get(1);
  					//count total number of data points
  					numPoints++;
  				}
  		//divide by number of entries/grid points
  		double xav = xsum/numPoints;
  		double yav = ysum/numPoints;

  		//add values to wind vector
  		wind.add(xav);
  		wind.add(yav);
  		return wind;

  	}

    /**
    * Determines the prevailing wind direction over all timeline grid values
    *
    * @param cutoff Used to vary the sequential cutoff value of the thread class for use in benchmarking
    * @return Vector of doubles representing the average x and y wind values
    */
    public Vector<Double> analyseData(int cutoff)
    {
      //calculate  average wind vector for all air layer elements and time steps
      //return vector with average x and y values
      Vector<Double> wind = new Vector();
      Vector<Double> sums = new Vector();
      if(cutoff==0)
      {
        sums = fjPool.invoke(new WriteClouds(classification, advection, convection, 0,dim()));
      }
      else
      {
        sums = fjPool.invoke(new WriteClouds(classification, advection, convection, 0,dim(), cutoff));
      }
      /*else
      {
        sums = process(cutoff); //for benchmarking
      }*/
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


    /**
    * Checks whether the given dimensions are within the bounds of the 3D array
    *
    *@param i Integer value representing the x position of the gridpoint to be verified
    *@param j Integer value representing the y position of the gridpoint to be verified
    *@return Boolean value indicating whether the gridpoint is within the bounds of the array
    */
    public boolean checkBounds(int i, int j)
    {
      boolean inBounds = false;
      if((i>=0)&&(i<dimx)&&(j>=0)&&(j<dimy))
      {
        inBounds = true;
      }
      return inBounds;
    }


    /**
    * Finds the cloud classification for the given gridpoint at the given time value
    *
    *@param time Integer value representing the time value at which to find the cloud classification
    *@param x Integer value representing the x position of the gridpoint
    *@param y Integer value representing the y position of the gridpoint
    *@return Integer value representing the type of cloud likely to form. 0 = Cumulus, 1= Striated stratus 2 = Amorphous stratus.
    */
    public int findCloud(int time, int x, int y)
    {
      //find average of x and y components as before - but only for local elements
      double xsum = 0;
      double ysum = 0;
      int numPoints = 0;
      //use indexes to sum correct blocks
      //check for boundaries
      boolean inBounds;
      for(int i = x-1; i <= x+1; i++)
        for(int j = y-1; j <= y+1; j++){
          inBounds = checkBounds(i, j);
          if(inBounds==true)
          {
            xsum += advection[time][i][j].get(0);
            ysum += advection[time][i][j].get(1);
            numPoints++;
          }
        }
      double xav = xsum/numPoints;
      double yav = ysum/numPoints;

      //magnitude
      double magnitude = Math.sqrt((xav*xav)+(yav*yav));
      //System.out.printf("Magnitude is %f\n", magnitude);
      int cloudType = 0;
      double uplift = convection[time][x][y]; //uplift value at the desired coordinate

      //assign to each air layer element an integer code (0, 1 or 2)
      //indicates the type of cloud that is likely to form in that location
      //classification for current element
      if (Math.abs(uplift) > magnitude)
      {
        cloudType = 0;
      }
      else if ((magnitude > 0.2)&&(magnitude >= Math.abs(uplift))) //significance of len??
      {
        cloudType = 1;
      }
      else
      {
        cloudType = 2;
      }
      classification[time][x][y] = cloudType;
      return cloudType;
    }


    /**
    * Finds cloud type for each value in the timeline grids
    */
    public void getClouds()
    {
      for(int t = 0; t < (dimt*scalingFactor); t++)
        for(int x = 0; x < dimx; x++)
          for(int y = 0; y < dimy; y++){
          findCloud(t, x, y); // finds cloud type for this point and writes value to classification array
          }
    }



    /**
     * Reads in text file of known correct output and inserts data into corresponding 3D arrays for use in verification of ouput
     *
     *@param correctFile Name of file to read correct output data from
     */
    void readCorrectData(String correctFile){
      try{
        Scanner sc = new Scanner(new File(correctFile), "UTF-8");

        // input grid dimensions and simulation duration in timesteps
        dt = sc.nextInt();
        dx = sc.nextInt();
        dy = sc.nextInt();

        // initialize and load advection (wind direction and strength) and convection
        checkWind = new Vector();
        checkWind.add(sc.nextDouble());
        checkWind.add(sc.nextDouble());
        checkClassification = new int[dt][dx][dy];
        for(int t = 0; t < dt; t++)
          for(int x = 0; x < dx; x++)
            for(int y = 0; y < dy; y++){
              checkClassification[t][x][y] = sc.nextInt();
            }
        sc.close();
      }
      catch (IOException e){
        System.out.println("Unable to open input file "+correctFile);
        e.printStackTrace();
      }
      catch (java.util.InputMismatchException e){
        System.out.println("Malformed input file "+correctFile);
        e.printStackTrace();
      }
    }

    /**
     * Computes average run time by discarding first two samples and averaging last five samples for benchmarking purposes
     *
     * @param times Array of times to be averaged
     * @return Float value representing average run time  
     */
    public float findAvTime(float[] times)
    {
      float av = (times[2]+times[3]+times[4]+times[5]+times[6])/5;
      return av;
    }




    /**
    * Performs benchmarking tests by varying data input size and sequential cutoff
    *
    * Generates results file containing benchmarking data
    */
    public void benchMarking()
    {
      try{
        FileWriter fileWriter = new FileWriter("benchmarking.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.printf("Benchmarking data\n");
        printWriter.printf("Data Sizes\n");
        System.out.println("Data Sizes\n");
      //different data sizes
      for (int d = 32; d < 513; d*=2)
      {
        scalingFactor = ((float)d/datasize);
        System.out.println("Scaling factor = " + scalingFactor);
        printWriter.printf("Scaling factor = %f\n", scalingFactor);
        System.out.println("Data size (no. of elements) = \n " + dim());
        printWriter.printf("Data size (no. of elements) = \n", dim());
        float[] times = new float[7]; //run program 7 times but only average last 5
        Vector<Double> wind = new Vector();
        for (int i = 0; i < 7; i++)
        {
           tick();
           wind = analyseData(0); //find prevailing wind vector, argument is sequential cut-off
           float time = tock();
           times[i] = time;
        }
        float averageTime = findAvTime(times);
        System.out.println("Threaded ");
        printWriter.printf("Threaded ");
        System.out.println("Average run took "+ averageTime +" milliseconds");
        printWriter.printf("Average run took %f milliseconds\n", averageTime);
        times = new float[7]; //run program 7 times but only average last 5
        wind = new Vector();
        for (int i = 0; i < 7; i++)
        {
           tick();
           wind = findAverage();
           getClouds();
           float time = tock();
           times[i] = time;
        }
        averageTime = findAvTime(times);
        System.out.println("Sequential");
        printWriter.printf("Sequential");
        System.out.println("Average run took "+ averageTime +" milliseconds");
        printWriter.printf("Average run took %f milliseconds\n", averageTime);
      }
      //different sequential cut-offs
      printWriter.printf("Sequential Cutoffs\n");
      System.out.println("Sequential Cutoffs\n");
      int[] cutoffs = {50, 500,5000,10000,20000,50000};
      for(int c = 0; c < cutoffs.length; c++)
      {
        float[] times = new float[7]; //run program 7 times but only average last 5
        Vector<Double> wind = new Vector();
        for (int i = 0; i < 7; i++)
        {
           tick();
           wind = analyseData(cutoffs[c]); //find prevailing wind vector, argument is sequential cut-off
           float time = tock();
           //System.out.println("Run took "+ time +" seconds");
           times[i] = time;
           //check if correct outside timing block
           checkOutput(wind);
         }
        float averageTime = findAvTime(times);
        System.out.println("Cutoff = " + cutoffs[c]);
        printWriter.printf("Cutoff = %d ", cutoffs[c]);
        System.out.println("Average run took "+ averageTime +" milliseconds");
        printWriter.printf("Average run took %f milliseconds ", averageTime);
        System.out.println("No. of threads " + WriteClouds.numThreads);
        printWriter.printf("No. of threads = %d\n", WriteClouds.numThreads);
        WriteClouds.numThreads = 0;
      }
      printWriter.close();
    }
    catch (IOException e){
      System.out.println("Unable to open input file ");
      e.printStackTrace();
    }
  }


  /**
  * Verifies that output is correct by comparison with known output data
  *
  * @param wind Vector containing computed prevailing wind values to be verified
  */
    public void checkOutput(Vector<Double> wind)
   	{
   		if (((wind.get(0)-checkWind.get(0) > EPSILON) | (wind.get(1)- checkWind.get(1) > EPSILON)))
   		{
   					isCorrect = false;
   					System.out.println("Average wind does not match");
   		}
   		if ((dimt!=dt)|(dimy!=dy)|(dimx!=dx))
   		{
   			isCorrect = false;
   			System.out.println("Dimensions differ");
   		}
   		for(int t = 0; t < (dt*scalingFactor); t++)
   			for(int x = 0; x < (dx); x++)
   				for(int y = 0; y < (dy); y++)
   				{
   					if(classification[t][x][y]!= checkClassification[t][x][y])
   					{
   						isCorrect = false;
   						System.out.println("Classification is incorrect");
   					}
   				}
   		if(isCorrect)
   		{
   			System.out.println("Output is correct");
   		}
   	}


    /**
    * Analyses input data to find prevailing wind direction and cloud types and writes output to a file.
    *
    *<p>
    * Additionally measures time taken for the prevailing wind and cloud types to be determined.
    *</p>
    *@param args The first argument args[0] given in the command line is the name of the input file to read from. The second argument args[1] given in the command line is the name of the output file to write to.
    */
    public static void main(String[] args){

	     CloudData cd = new CloudData();
       cd.readData(args[0]); //read in data from input file
       cd.readCorrectData(args[2]); //read in correct output data to compare

       if(args.length == 4)
       {
         if(args[3].equals("-t"))
         {
           //run benchmarking tests
           cd.benchMarking();
         }
       }
       else
       {
	        //standard config
	         cd.scalingFactor = 1;
           cd.tick();
           Vector wind = cd.analyseData(0);
           float time = cd.tock();
           System.out.println(time);
           cd.tick();
           wind = cd.findAverage();
           cd.getClouds();
           time = cd.tock();
           System.out.println(time);
           cd.checkOutput(wind);
           cd.writeData(args[1], wind);
         }

       }

}
