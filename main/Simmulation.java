package main;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.plaf.synth.SynthSpinnerUI;

import org.apache.commons.io.output.ByteArrayOutputStream;

import board.Board;
import board.Stage;
import helpers.VectorHelper;
import mpi.MPI;
import pop.Population;

public class Simmulation{
	

	
	private Object arrLock = new Object();
	private Object listLock = new Object();
	
	//private final int POPULATION_SIZE = 100;
	private final int GENOME_LENGTH = 400;
	private final int GENERATIONS_CAP = 1000;

	private final double SUCCESS_RATE_MINIMUM = 0.75;
	private final double PARTIAL_MUTATION_ODDS = 0.3;
	private final double FULL_MUTATION_ODDS = 0.08;
	
	private int populationSize;
	
	private Stage stage;
	private Population pop;
	
	private int generation = 0;
	private double successRate = 0.0;
	private int simmulationNum = 0;
	private Object generationLock = new Object();
	private Object successRateLock = new Object();
	private Object simmulationNumLock = new Object();
	
	
	
	private String[] args;
	
	public Simmulation(int stage, int populationSize, String[] args) {
		this.populationSize = populationSize;
		init(stage);
		this.args = args;
	}
	
	
	
	public void draw(Graphics2D g2d) {
		
		stage.draw(g2d);

		pop.draw(g2d);		
	
	}
	
	public void init(int st) {
		initStage(st);
		initPopulation();
	}
	
	
	private void initStage(int st) {
		stage = new Stage(st);
		
	}
	private void initPopulation() {
		pop = new Population(populationSize, GENOME_LENGTH);
		pop.setStartingPosition(VectorHelper.intToDouble(stage.getStartPosition()));
	}
	
	
	public int simmulate() {
		int id = MPI.COMM_WORLD.Rank();
		
		while( generation <= GENERATIONS_CAP) {
			incrementGeneration();
			
			try {
				simmulateMPI();
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			
	        
			if( checkSuccessRate() > SUCCESS_RATE_MINIMUM ) break;
			if(id==0) {
				evaluate();
				breed();
				mutate();
				killParents();
			}
			
			
			
		}
		return getGeneration();
	}
	
	
	
	private void simmulateMPI() {
		
        int id = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        MPI.COMM_WORLD.Barrier();
		
        Population [] submits = new Population[size];
        submits = splitPop(size);            
        
        MPI.COMM_WORLD.Bcast(submits, 0, submits.length, MPI.OBJECT, 0);        
        
        
        Population[] recv =  new Population[size];
        
        recv[id] = simmulateGeneration(submits[id]);
        
        MPI.COMM_WORLD.Gather(recv, id, 1, MPI.OBJECT, recv, 0, 1, MPI.OBJECT, 0);
                            
        MPI.COMM_WORLD.Barrier();
        if(id == 0 ) {
        	mergePopulation(recv);
        }
    }

	private Population[] splitPop(int chunks) {
		Population[] a = new Population[chunks];
		for(int i = 0 ; i < chunks ; i++) {
			a[i] = pop.split(i, chunks);
		}
		return a;
	}

	private byte[] populationToByteArr(Population p) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos;
		oos = new ObjectOutputStream(bos);
		oos.writeObject(p);
	    oos.flush();
	    byte[] b =  bos.toByteArray();
	    bos.close();
	    return b;
	}
	public static Object byteToObj(byte[] bytes) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
	    ObjectInputStream objStream = new ObjectInputStream(byteStream);

	    return objStream.readObject();
	}
	
	private Population simmulateGeneration(Population po) {
		for (int i = 0; i < GENOME_LENGTH; i++) {
			
			boolean simmulating = po.update(i);
			po.handleCollisions(stage.getWalls(), stage.getGoal());
			
			
			if(!simmulating) break;
			
		}
		return po;
	}

	private void mergePopulation(Object[] arr){
		for(Object o : arr) System.out.println("merge part: " + o.toString());
		Population  tmp = null;
		System.out.println("mergam");
		for( int i = 0 ; i < arr.length ; i++ ) {
			if( i == 0 ) tmp = (Population) arr[0];
			else tmp = tmp.merge((Population) arr[i]);
			
		}
		pop = tmp;
	}
	
	
	private void killParents() {
		pop.killParents();
		pop.setStartingPosition(VectorHelper.intToDouble(stage.getStartPosition()));
	}
	
	private void mutate() {
		pop.partialMutation( PARTIAL_MUTATION_ODDS );
		pop.fullMutation( FULL_MUTATION_ODDS );
	}
	
	
	
	private void breed() {
		pop.poolGenes();
		pop.breed();
		
	}
	
	
	
	private void evaluate() {
		pop.evaluate(stage);
	}
	
	
	
	
	
	
		
	public void resetSimmulation() {
		setGeneration(0);
		setSuccessRate(0);
		pop = new Population(populationSize, GENOME_LENGTH);
	}

	
	
	
	

	
	public Stage getStage() { return stage; }
	
	
	public void setPopulationSize(int popSize) {
		this.populationSize = popSize;
	}
	private double checkSuccessRate() {
		double rate = pop.getEvolved()*1.0 / populationSize*1.0;
		synchronized( successRateLock ) { successRate = rate; }
		return rate;
	}
	public double getSuccessRate() { 
		synchronized( successRateLock ) { return successRate; }
	}
	public void setSuccessRate(double sr) { 
		synchronized( successRateLock ) { this.successRate = sr; }
	}
	private void incrementGeneration() {
		synchronized( generationLock ) { generation++; }
	}
	public int getGeneration() {
		synchronized( generationLock ) { return generation; }
	}
	public void setGeneration(int gen) {
		synchronized( generationLock ) { this.generation = gen; }
	}
	public void setSimmulationNum(int n) {
		synchronized( simmulationNumLock ) { this.simmulationNum = n; }
	}
	public int getSimmulationNum() { 
		synchronized( simmulationNumLock ) { return this.simmulationNum; }
	}
	public void incrementSimmulationNum() {
		synchronized( simmulationNumLock ) { this.simmulationNum++; }
	}
	
	


}
