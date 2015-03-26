public class PlayerSkeleton {

  //implement this function to have a working system
  public int pickMove(State s, int[][] legalMoves) {
    return 0;
  }

  public double heuristic(State s, double[] weights) {
	double sum = 0.0;
	int[] top = s.getTop();
	int[][] field = s.getField();
	int wi = 0;

	int maxHeight = 0;
	for(int i = 0; i < top.length; i++) {
		sum += top[i] * weights[wi++];
		if(top[i] > maxHeight) maxHeight = top[i];
	}

	for(int i = 0; i < top.length - 1; i++){ sum += Math.abs(top[i] - top[i+1]) * weights[wi++];
	}

	int holes = 0;
	for(int col = 0; col < field[0].length; col++){
		for(int row = 0; row < top[col]; row++){
			if(field[row][col] == 0) holes++;
		}
	}

	sum += holes * weights[wi++];
			
	return sum;
  }

  public static void main(String[] args) {
    State s = new State();
    new TFrame(s);
    PlayerSkeleton p = new PlayerSkeleton();
    while(!s.hasLost()) {
      s.makeMove(p.pickMove(s,s.legalMoves()));
      s.draw();
      s.drawNext(0,0);
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("You have completed "+s.getRowsCleared()+" rows.");
  }

}
