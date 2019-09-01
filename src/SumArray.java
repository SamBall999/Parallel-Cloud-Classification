

import java.util.concurrent.RecursiveTask;
import java.util.Vector;

public class SumArray extends RecursiveTask<Vector<Double>>  {
	  int lo; // arguments
	  int hi;
		Vector<Float>[][][] advection;
		int dimx, dimy, dimt;
		//int xyFlag;
		double xsum = 0;
		double ysum = 0;
	  static final int SEQUENTIAL_CUTOFF=2000; //500 //3

	  int ans = 0; // result

	  SumArray(Vector<Float>[][][] advec, int l, int h) {
	    lo=l; hi=h; advection = advec;
			dimt = advection.length;
			dimx = advection[0].length;
			dimy = advection[0][0].length;
	  }

		// convert linear position into 3D location in simulation grid
		void locate(int pos, int [] ind)
		{
			ind[0] = (int) pos / (dimx*dimy); // t
			ind[1] = (pos % (dimx*dimy)) / dimy; // x
			ind[2] = pos % (dimy); // y
		}


	  protected Vector<Double> compute(){// return answer - instead of run
		  if((hi-lo) < SEQUENTIAL_CUTOFF) {
				//each position represents a specific grid point
				int[] gridPoint = new int[3];
		    for(int i=lo; i < hi; i++)
				{
						locate(i, gridPoint);
						xsum += advection[gridPoint[0]][gridPoint[1]][gridPoint[2]].get(0);
						ysum += advection[gridPoint[0]][gridPoint[1]][gridPoint[2]].get(1);
				}
				Vector<Double> sums = new Vector();
				sums.add(xsum);
				sums.add(ysum);
				return sums;
		  }
		  else {
			  SumArray left = new SumArray(advection, lo,(hi+lo)/2);
			  SumArray right= new SumArray(advection,(hi+lo)/2,hi);

			  // order of next 4 lines
			  // essential – why?
			  left.fork();
			  Vector<Double> rightAns = right.compute();
			  Vector<Double> leftAns = left.join();
				Vector<Double> combined = new Vector();
				combined.add(rightAns.get(0)+leftAns.get(0));
				combined.add(rightAns.get(1)+leftAns.get(1));
				return combined;
			  //return leftAns + rightAns;
		  }
	 }


}
