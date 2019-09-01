import java.util.concurrent.RecursiveAction;
import java.util.Vector;

public class WriteClouds extends RecursiveAction { //change back to recusive task if necessary
	  int lo; // arguments
	  int hi;
		int[][][] classification;
		Vector<Float>[][][] advection;
		float[][][] convection;
		int dimx, dimy, dimt;
	  static final int SEQUENTIAL_CUTOFF= 2000; //500 //3

	  //int ans = 0; // result

	  WriteClouds(int[][][] clas, Vector<Float>[][][] advec, float[][][] conv, int l, int h) {
	    lo=l; hi=h; classification = clas; advection = advec; convection = conv;
			dimt = classification.length;
			dimx = classification[0].length;
			dimy = classification[0][0].length;
	  }

		// convert linear position into 3D location in simulation grid
		void locate(int pos, int [] ind)
		{
			ind[0] = (int) pos / (dimx*dimy); // t
			ind[1] = (pos % (dimx*dimy)) / dimy; // x
			ind[2] = pos % (dimy); // y
		}

		boolean checkBounds(int i, int j)
		{
			boolean inBounds = false;
			if((i>=0)&&(i<dimx)&&(j>=0)&&(j<dimy))
			{
				inBounds = true;
			}
			return inBounds;
		}

		int findCloud(int time, int x, int y)
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
				float uplift = convection[time][x][y]; //uplift value at the desired coordinate

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


	  protected void compute(){// change back to protected integer if necessary
		  if((hi-lo) < SEQUENTIAL_CUTOFF) {
				//each position represents a specific grid point
			  //int ans = 0;
				int[] gridPoint = new int[3];
		    for(int i=lo; i < hi; i++)
				{
						locate(i, gridPoint);
						classification[gridPoint[0]][gridPoint[1]][gridPoint[2]] = findCloud(gridPoint[0],gridPoint[1],gridPoint[2]);
				}
		  }
		  else {
			  WriteClouds left = new WriteClouds(classification, advection, convection, lo,(hi+lo)/2);
			  WriteClouds right= new WriteClouds(classification, advection, convection, (hi+lo)/2,hi);

			  // order of next 4 lines
			  // essential – why?
			  left.fork();
			  right.compute();
			  left.join();
			  //return classification;
		  }
	 }


}
