import java.util.concurrent.RecursiveTask;
import java.util.Vector;

/**
 * Threaded class to write values representing cloud classification to a 3D array and sum a 3D array of (x, y) wind vectors using parallelization
 *
 *@author Samantha Ball
 *@version 1.0
 *@since 1
 */
public class WriteClouds extends RecursiveTask<Vector<Double>>  {
	  int lo; // arguments
	  int hi;
		int[][][] classification;
		Vector<Double>[][][] advection;
		double[][][] convection;
		int dimx, dimy, dimt;
	  static int SEQUENTIAL_CUTOFF= 2500; //vary from 0-50000 //change back to final if necessary
		double xsum = 0;
		double ysum = 0;
		int ans = 0; // result
		static int numThreads = 0;


		/**
    *Creates a new WriteClouds instance with the specified parameters
    *
		*@param clas 3D array that classification values are written to
    *@param advec 3D array of advection values to be added
		*@param conv 3D array of convection values used in cloud classification method
    *@param l Lower bound of elements to be added and classified
    *@param h Upper bound of elements to be added and classified
    */
	  public WriteClouds(int[][][] clas, Vector<Double>[][][] advec, double[][][] conv, int l, int h) {
	    lo=l; hi=h; classification = clas; advection = advec; convection = conv;
			dimt = classification.length;
			dimx = classification[0].length;
			dimy = classification[0][0].length;
	  }

		public WriteClouds(int[][][] clas, Vector<Double>[][][] advec, double[][][] conv, int l, int h, int cutoff) {
		 lo=l; hi=h; classification = clas; advection = advec; convection = conv;
		 dimt = classification.length;
		 dimx = classification[0].length;
		 dimy = classification[0][0].length;
		 SEQUENTIAL_CUTOFF = cutoff;
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
				//classification[time][x][y] = cloudType;
				return cloudType;
			}


		/**
	  * Performs summing operation of (x,y) pairs and writing of classification data to the 3D array using parallelization
	  *
	  *<p>
	  *Parallelization is achieved by creating new threads until the sequential cutoff is reached. The x and y sums are then determined and returned as a 2D vector. The cloud classification values for each gridpoint are found and written to the 3D array.
	  *</p>
		*@return Vector object containing two float values representing the sum of the x values and the sum of the y values respectively
		*/
	  protected Vector<Double> compute(){// change back to protected integer if necessary
		  if((hi-lo) < SEQUENTIAL_CUTOFF) {
				//each position represents a specific grid point
				int[] gridPoint = new int[3];
				//System.out.println("Seq cutoff reached");
		    for(int i=lo; i < hi; i++)
				{
						locate(i, gridPoint);
						classification[gridPoint[0]][gridPoint[1]][gridPoint[2]] = findCloud(gridPoint[0],gridPoint[1],gridPoint[2]);
						xsum += advection[gridPoint[0]][gridPoint[1]][gridPoint[2]].get(0);
					  ysum += advection[gridPoint[0]][gridPoint[1]][gridPoint[2]].get(1);
				}
				Vector<Double> sums = new Vector();
			  sums.add(xsum);
			  sums.add(ysum);
			  return sums;
		  }
		  else {
			  WriteClouds left = new WriteClouds(classification, advection, convection, lo,(hi+lo)/2);
			  WriteClouds right= new WriteClouds(classification, advection, convection, (hi+lo)/2,hi);

			  // order of next 4 lines
			  // essential – why?
				left.fork();
				numThreads++;
			  Vector<Double> rightAns = right.compute();
				//System.out.println("Running in parallel");
			  Vector<Double> leftAns = left.join();
				Vector<Double> combined = new Vector();
				combined.add(rightAns.get(0)+leftAns.get(0));
				combined.add(rightAns.get(1)+leftAns.get(1));
				return combined;
		  }
	 }


}
