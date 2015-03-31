class Genetic {
	public static void main(String[] args) {
    State s = new State();
    Weights initialWeights = Weights.randomWeights(State.COLS);
		PlayerSkeleton p = new PlayerSkeleton(initialWeights, State.ROWS-5, State.COLS);
		System.out.println("You have completed "+p.playAndReturnScore()+" rows.");
	}
}
