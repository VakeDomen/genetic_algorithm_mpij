import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import graphics.Gfx;
import main.Simmulation;
import mpi.MPI;

public class Main {
	
	
	private static final int SIMMULATION_COUNT = 250;
	
	public static BufferedWriter w;
	
	private static double avrGeneration = 0.;
	private static double avrTime = 0.;
	
	
	
	public static void main(String[] args) {	
		
		try {
			w = new BufferedWriter(new FileWriter("Log.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		MPI.Init(args);
        int id = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        
		
		
		Simmulation s = new Simmulation(1, 100, args);
		if(id==0) {
			Gfx g = new Gfx(s);
			g.init();
		}
		
		if(id==0) log("simmulation count: " + SIMMULATION_COUNT);
		
		/*
		 *  POPULATION SIZE : 100
		 */
		
		for( int i = 0 ; i < SIMMULATION_COUNT ; i++) {
			double st = System.currentTimeMillis(); 
			
			s.incrementSimmulationNum();
			
			
			int end = s.simmulate();
			
			
			double et = System.currentTimeMillis();
			calcStat(end, et-st, i);
			
			s.resetSimmulation();
			
			
		}
		if(id==0) log("avr gen: " + avrGeneration + " avr time: " + avrTime);
		
		
		
		/*
		 *  POPULATION SIZE : 500
		 */
		s.setPopulationSize(500);
		
		
		for( int i = 0 ; i < SIMMULATION_COUNT ; i++) {
			double st = System.currentTimeMillis(); 
			
			s.incrementSimmulationNum();
			
			
			int end = s.simmulate();
			
			
			double et = System.currentTimeMillis();
			calcStat(end, et-st, i);
			
			s.resetSimmulation();
			
			
		}
		if(id==0) log("avr gen: " + avrGeneration + " avr time: " + avrTime);
		
		/*
		 *  POPULATION SIZE : 2500
		 */
		s.setPopulationSize(2500);
		
		for( int i = 0 ; i < SIMMULATION_COUNT ; i++) {
			double st = System.currentTimeMillis(); 
			
			s.incrementSimmulationNum();
			
			
			int end = s.simmulate();
			
			
			double et = System.currentTimeMillis();
			calcStat(end, et-st, i);
			
			s.resetSimmulation();
			
			
		}
		if(id==0) log("avr gen: " + avrGeneration + " avr time: " + avrTime);
		
		
		
		MPI.Finalize();
		
		
		
		
		
		
		
	}


	
	private static void calcStat(int gen, double time, int counter) {
		if(counter > 0) {
			avrGeneration = (avrGeneration * counter + gen)/(counter+1);
			avrTime = (avrTime * counter + time)/(counter+1);
			System.out.println("avr gen: " + avrGeneration + " avr time: " + avrTime);
		}else {
			avrGeneration = gen;
			avrTime = time;
			System.out.println("avr gen: " + avrGeneration + " avr time: " + avrTime);
		}
		
		
		
		
	}



	private static void log(String s) {
		try {
			w.write(s+"\n");
			w.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
