class Genetic {
	public static void main(String[] args) {
    Weights w = new Weights();
    w.setWeights();
		PlayerSkeleton p = new PlayerSkeleton(w);
		System.out.println("You have completed "+p.playAndReturnScore()+" rows.");
	}
}
