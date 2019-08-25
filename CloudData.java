package cloudscapes;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;
import java.io.FileWriter;
import java.io.PrintWriter;

public class CloudData {

	Vector<Float> [][][] advection; // in-plane regular grid of wind vectors, that evolve over time
	float [][][] convection; // vertical air movement strength, that evolves over time
	int [][][] classification; // cloud type per grid point, evolving over time
	int dimx, dimy, dimt; // data dimensions

	// overall number of elements in the timeline grids
	int dim(){
		return dimt*dimx*dimy;
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

	// write classification output to file
	void writeData(String fileName, Vector wind){
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

	Vector<Float> findAverage()
	{
		//calculate  average wind vector for all air layer elements and time steps
		//actually return vector
		//sum all x wind values
		Vector<Float> wind = new Vector();
		float xsum = 0;
		float ysum = 0;
		int numPoints = 0;
		for(int t = 0; t < dimt; t++)
			for(int x = 0; x < dimx; x++)
				for(int y = 0; y < dimy; y++){
					xsum += advection[t][x][y].get(0);
					ysum += advection[t][x][y].get(1);
					numPoints++;
				}
		float xav = xsum/numPoints;
		float yav = ysum/numPoints;
		//System.out.printf("Sum is %f\n", xsum);
		//System.out.printf("No. points is %d\n", numPoints);
		//System.out.printf("Average is %f\n", xav);
		//divide by number of entries/grid points
		//return xav;
		/*for(int t = 0; t < dimt; t++)
			for(int x = 0; x < dimx; x++)
				for(int y = 0; y < dimy; y++){
					ysum += advection[t][x][y].get(1);
				}*/
		//float yav = ysum/numPoints;
		//System.out.printf("Sum is %f\n", ysum);
		//System.out.printf("No. points is %d\n", numPoints);
		//System.out.printf("Average is %f\n", yav);

		wind.add(xav);
		wind.add(yav);
		return wind;

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


	/*int findCloud(float time, float x, float y)
	{
		//find average of x and y components as before - but only for local elements
		float xsum = 0;
		float ysum = 0;
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
		float xav = xsum/numPoints;
		float yav = ysum/numPoints;

		//find combined magnitude and compare to u
		int cloudType;
		float u;
		float w = 0;
		//assign to each air layer element an integer code (0, 1 or 2)
		//indicates the type of cloud that is likely to form in that location based on a comparison of the local average wind
    //direction and uplift value

		//classification for current element
		u = convection[time][x][y]; //corresponding uplift value
		if (abs(u) > len(w))
		{
			cloudType = 0;
		}
		else if ((len(w) > 0.2)&&(len(w) >= abs(u))) //significance of len??
		{
			cloudType = 1;
		}
		else
		{
			cloudType = 2;
		}
		classification[time][x][y] = cloudType;
		return cloudType;
	}*/


	public static void main(String[] args)
	{
		CloudData cd = new CloudData();
		cd.readData(args[0]);
		Vector<Float> wind = cd.findAverage();
		System.out.println(wind.get(0));
		System.out.println(wind.get(1));
		System.out.println(cd.checkBounds(-1,6));
		cd.writeData(args[1], wind);
	}
}
