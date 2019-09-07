

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;

import java.util.concurrent.ForkJoinPool;

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

	// convert linear position into 3D location in simulation grid
	void locate(int pos, int [] ind)
	{
		ind[0] = (int) pos / (dimx*dimy); // t
		ind[1] = (pos % (dimx*dimy)) / dimy; // x
		ind[2] = pos % (dimy); // y
	}

	// read cloud simulation data from file
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

	// write classification output to file
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

    public Vector<Double> analyseData()
    {
      //calculate  average wind vector for all air layer elements and time steps
      //return vector with average x and y values
      Vector<Double> wind = new Vector();
      Vector<Double> sums = new Vector();

      sums = fjPool.invoke(new WriteClouds(classification, advection, convection, 0,dim()));

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
    *@param i Integer value representing the x position of the gridpoint
    *@param j Integer value representing the y position of the gridpoint
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


    /*void writeResults(String results){
	try{
	    FileWriter fileWriter = new FileWriter("results.txt");
	    PrintWriter printWriter = new PrintWriter(fileWriter);
	    printWriter.printf("%s", results);
	    printWriter.close();
	}
	catch (IOException e){
	    System.out.println("Unable to open output file "+"results.txt");
	    e.printStackTrace();
	}
}*/


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

public static void main(String[] args){

	CloudData cd = new CloudData();
  cd.readData(args[0]); //read in data from input file
  cd.readCorrectData(args[2]); //read in correct output data to compare

  if(args.length == 4)
  {
    if(args[3].equals("-t"))
    {
    //run benchmaring tests
    }
  }
  else
  {
	//standard config
	cd.scalingFactor = 1;
  cd.tick();
  Vector wind = cd.analyseData();
  float time = cd.tock();
  System.out.println(time);
  cd.tick();
  wind = cd.findAverage();
  cd.getClouds();
  time = cd.tock();
  System.out.println(time);
	//data.validate(correctOutputFileName);
  cd.checkOutput(wind);
  cd.writeData(args[1], wind);
  }

}

}
