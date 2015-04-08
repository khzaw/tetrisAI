class Individual {
	public int fitness, cols;
	public int[] chromosomes;
	private static int MUCHANCE = 20;
	java.util.Random r;

	public Individual (int cols) {
		chromosomes = Weights.randomWeights(cols).toArray();
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
		for (int i = 0; i < chromosomes.length; i++)
			if (r.nextInt(101) < MUCHANCE)
				chromosomes[i] = Weights.getRandom();
	}

	public void print(int num) {
		System.out.format("%d Fitness: %d - Chromosomes: ", num, fitness);
		for (int i = 0; i < chromosomes.length; i++)
			System.out.format("[%d]", chromosomes[i]);
		System.out.format("\n");
	}
}

class Genetic {
	Individual[] individuals;
	PlayerSkeleton p;
	int rows, cols;

	public Genetic(int populationSize, int rows, int cols) {
		individuals = new Individual[populationSize];
		this.rows = rows;
		this.cols = cols;
		for (int i = 0; i < populationSize; i++)
			individuals[i] = new Individual(cols);
	}

	private void calculateFitness() {
		Weights w;
		for (int i = 0; i < individuals.length; i++) {
			w = Weights.fromArray(individuals[i].chromosomes);
			PlayerSkeleton p = new PlayerSkeleton(w, rows, cols);
			individuals[i].fitness = p.playAndReturnScore();
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

		for (int i = 0; i < children.length; i++) {
			p1 = selectParent(individuals[0].r.nextInt(total), null);
			p2 = selectParent(individuals[0].r.nextInt(total), null);
			children[i] = Individual.procreate(p1, p2);
			children[i].mutate();
		}

		individuals = children;
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
		return best;
	}

	private void printGenerationInfo(int gen) {
		System.out.format("\nGeneration %d:\n", gen);
		for (int i = 0; i < individuals.length; i++)
			individuals[i].print(i);
	}

	public Weights train(int generations) {
		for (int i = 0; i < generations; i++) {
			calculateFitness();
			printGenerationInfo(i);
			selectAndProcreateFittest();
		}
		calculateFitness();
		printGenerationInfo(generations);
		Individual best = getBest();
		return Weights.fromArray(best.chromosomes);
	}
}
















