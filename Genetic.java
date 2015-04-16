class Individual {
	public int fitness, cols;
	public int[] chromosomes;
	private static int MUCHANCE = 20;
	java.util.Random r;

	public Individual (int cols) {
		chromosomes = Weights.randomWeights().toArray();
		this.cols = cols;
		r = new java.util.Random();
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
			int chr = chromosomes[i];
			int mutated = (int) ((double) Math.round(chr * (stdDev*r.nextGaussian()+1.0) * scale) );
			chromosomes[i] = mutated;
		}
	}

	public void print(int num) {
		System.out.format("%d Fitness: %d - Chromosomes: ", num, fitness);
		for (int i = 0; i < chromosomes.length; i++)
			System.out.format("[%d]", chromosomes[i]);
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
		Weights w;
		for (int i = 0; i < individuals.length; i++) {
			w = Weights.fromArray(individuals[i].chromosomes);
			// PlayerSkeleton p = new PlayerSkeleton(w, rows, cols);
			// individuals[i].fitness = p.playAndReturnScore();

			int noRounds = 5; // How many rounds to take the average of
			int score = 0;
			for(int j = 0; j < noRounds; j++) {
				PlayerSkeleton p = new PlayerSkeleton(w, rows, cols);
				score += p.playAndReturnScore();
			}
			individuals[i].fitness = score/noRounds;
		}
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

	private void selectAndProcreate() {
		Individual[] children = new Individual[individuals.length];
		Individual p1, p2;

		java.util.List<Integer> firstRound = new java.util.ArrayList<Integer>();
		java.util.List<Integer> secondRound = new java.util.ArrayList<Integer>();
		for (int i = 0; i < individuals.length; i++)
			firstRound.add(i);
		java.util.Collections.shuffle(firstRound);
		for (int i = 0; i < firstRound.size(); i+=2) {
			if (individuals[firstRound.get(i)].fitness >
			    individuals[firstRound.get(i+1)].fitness)	
				secondRound.add(firstRound.get(i));
			else
				secondRound.add(firstRound.get(i+1));
		}
		java.util.Collections.shuffle(secondRound);

		for (int i = 0; i < secondRound.size(); i+=2) {
			p1 = individuals[secondRound.get(i)];
			p2 = individuals[secondRound.get(i+1)];
			children[i] = Individual.procreate(p1, p2);
			children[i*2] = Individual.procreate(p1, p2);
			children[i].mutate();
			children[i*2].mutate();
		}

		individuals = children;
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