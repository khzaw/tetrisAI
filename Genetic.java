class Genetic {
	public static void main(String[] args) {
		PlayerSkeleton p = new PlayerSkeleton(Weights.getWeights(State.COLS), State.ROWS, State.COLS);
		System.out.println("You have completed "+p.playAndReturnScore()+" rows.");
	}
}
