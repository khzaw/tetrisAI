import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
class Individual {
	public int fitness, cols;
	public double[] chromosomes;
	private static int MUCHANCE = 20;
	Random r;

	public Individual (int cols) {
		chromosomes = Weights.randomWeights().toArray();
		this.cols = cols;
		this.r = new Random();
	}

	public static Individual procreate(Individual i1, Individual i2) {
		Individual child = new Individual(i1.cols);
		int rand = child.r.nextInt(child.chromosomes.length);
		for (int i = 0; i < child.chromosomes.length; i++) {
			if (i < rand)
				child.chromosomes[i] = i1.chromosomes[i];
			else
				child.chromosomes[i] = i2.chromosomes[i];
		}
		return child;
	}

	public void mutate() {
		double stdDev = 0.3; // std dev for random multiplication and scale factor
		double scale = 0.5*stdDev*r.nextGaussian()+1.0;
		// double scale = 1;
		for (int i = 0; i < chromosomes.length; i++) {
			double chr = chromosomes[i];
			double mutated = chr * (stdDev*r.nextGaussian()+1.0) * scale;
			chromosomes[i] = mutated;
		}
	}

	public void print(int num) {
		System.out.format("%d Fitness: %d - Chromosomes: ", num, fitness);
		for (int i = 0; i < chromosomes.length; i++)
			System.out.format("[%f]", chromosomes[i]);
		System.out.format("\n");
	}
}

class Genetic {
	Individual allTimeBest;
	Individual[] individuals;
	PlayerSkeleton p;
	int rows, cols, populationSize;

	public Genetic(int populationSize, int rows, int cols) {
		individuals = new Individual[populationSize];
		this.rows = rows;
		this.cols = cols;
		this.populationSize = populationSize;
		this.allTimeBest = new Individual(cols);
		this.allTimeBest.fitness = 0;
		for (int i = 0; i < populationSize; i++)
			individuals[i] = new Individual(cols);
	}

	private void resetIndividuals(){
		this.allTimeBest = new Individual(cols);
		for (int i = 0; i < populationSize; i++)
			individuals[i] = new Individual(cols);
	}

	private void calculateFitness() {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int i = 0; i < individuals.length; i++) {
			final Weights w = Weights.fromArray(individuals[i].chromosomes);
			// PlayerSkeleton p = new PlayerSkeleton(w, rows, cols);
			// individuals[i].fitness = p.playAndReturnScore();

			int noRounds = 10; // How many rounds to take the average of
			final Individual in = individuals[i];
			in.fitness = 0;
			for(int j = 0; j < noRounds; j++) {
				Runnable aRunnable = new Runnable(){
		            @Override
		            public void run() {
						PlayerSkeleton p = new PlayerSkeleton(w, rows, cols);
						in.fitness += p.playAndReturnScore();
					}
				};
				executor.execute(aRunnable);
			}
			individuals[i].fitness = individuals[i].fitness/noRounds;
		}
		executor.shutdown();
        while (!executor.isTerminated()) {}
        System.out.println("Finished all threads");
	}

	private int totalFitness() {
		int total = 0;
		for (int i = 0; i < individuals.length; i++)
			total += individuals[i].fitness;
		return total;
	}

	private Individual selectParent(int rand, Individual exclude) {
		int i = 0;
		Individual indi;

		while (rand >= 0) {
			indi = individuals[i++];
			if (rand < indi.fitness && indi != exclude) {
				System.out.format("%d ", i-1);
				return indi;
			} else if (indi != exclude) {
				rand -= indi.fitness;
			}
		}
		return null; // will not happen
	}

	private void selectAndProcreateFittest() {
		int total = totalFitness();
		Individual[] children = new Individual[individuals.length];
		Individual p1, p2;

		// keep best 1
		children[0] = getBest();

		// create the rest (NOTE FROM i = 1)
		for (int i = 1; i < children.length; i++) {
			p1 = selectParent(individuals[0].r.nextInt(total), null);
			p2 = selectParent(individuals[0].r.nextInt(total), null);
			children[i] = Individual.procreate(p1, p2);
			children[i].mutate();
		}

		individuals = children;
		System.out.println();
	}

	private Individual getBest() {
		Individual best = individuals[0];
		for (int i = 0; i < individuals.length; i++)
			if (individuals[i].fitness > best.fitness)
				best = individuals[i];

		System.out.format("\nBest: ");
		best.print(1);
		// if(best.fitness > allTimeBest.fitness) {
		// 	allTimeBest.fitness = best.fitness;
		// 	System.arraycopy(best.chromosomes, 0, allTimeBest.chromosomes, 0, best.chromosomes.length);
		// }

		return best;
	}

	private void printGenerationInfo(int gen) {
		System.out.format("\nGeneration %d:\n", gen);
		for (int i = 0; i < individuals.length; i++) {
			individuals[i].print(i);
		}
		System.out.println("\nGENERATION AVERAGE: " + totalFitness()/populationSize);
	}

	public Weights train(int generations) {
		System.out.println("training for " + generations + " generations");
		for (int i = 0; i < generations; i++) {
			System.out.println("generation begin");
			calculateFitness();
			while(totalFitness() == 0){
				resetIndividuals();
				calculateFitness();
				System.out.println("resetting");
			}
			printGenerationInfo(i);
			selectAndProcreateFittest();
			System.out.println("generation complete");
		}
		System.out.println("generation begin");
		calculateFitness();
		printGenerationInfo(generations);
		Individual best = getBest();
		System.out.println("generation complete");
		return Weights.fromArray(best.chromosomes);
	}
}
