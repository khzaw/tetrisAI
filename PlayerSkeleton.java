class Moves extends State {
	public static int[][][] allMoves = legalMoves;
	public static int getNumMoves(int piece) {
		return allMoves[piece].length;
	}
}

class Weights {
	public double holes;
	public double maxHeight;
	public double rowsCleared;
	public double[] topHeights = new double[State.COLS];
	public double[] topHeightDiffs = new double[State.COLS - 1];

	public void setWeights() {
		for (int i = 0; i < topHeights.length; i++)
			topHeights[i] = .5;
		for (int i = 0; i < topHeightDiffs.length; i++)
			topHeightDiffs[i] = 1.5;
		maxHeight = 1.5;
		holes = 5;
		rowsCleared = -2;
	}
}

class Simulator
{
	// Handy pointers to have locally
	private static int[][][] legalMoves = Moves.allMoves;
	private static int[][][] pBottom = State.getpBottom();
	private static int[][][] pTop = State.getpTop();
	private static int[][] pWidth = State.getpWidth();
	private static int[][] pHeight = State.getpHeight();

	// Simulator state
	public int[][] field = new int[State.ROWS][State.COLS];
	public int[] top = new int[State.COLS];
	public int turn = 0;
	public int maxHeight = 0;

	// For quick heuristics, simMove keeps this field updated for:
	// - Max height
	// - Column Heights
	// - Holes
	// - Cleared
	public double heuristic;

  public Weights weights;

	public void revertTo(Simulator sim) {
		System.arraycopy(sim.top, 0, top, 0, top.length);
		for (int i = 0; i < field.length; i++)
			System.arraycopy(sim.field[i], 0, field[i], 0, field[i].length);

		turn = sim.turn;
		heuristic = sim.heuristic;
		maxHeight = sim.maxHeight;
	}

	public double getHeuristic() {
		double sum = heuristic;

		for(int i = 0; i < top.length - 1; i++)
			sum += Math.abs(top[i] - top[i+1]) * weights.topHeightDiffs[i];

		return sum;
	}

  public int randomPiece() {
    return (int)(Math.random()*7);
  }

	public boolean simMove(int move, int piece) {
		int orient = legalMoves[piece][move][State.ORIENT];
		int slot = legalMoves[piece][move][State.SLOT];
		turn++;

		// height if the first column makes contact
		int height = top[slot] - pBottom[piece][orient][0];
		// for each column beyond the first in the piece
		for(int col = 1; col < pWidth[piece][orient]; col++)
			height = Math.max(height, top[slot + col] - pBottom[piece][orient][col]);

		// Check if game ended
		if(height + pHeight[piece][orient] >= State.ROWS)
			return false;

		placePiece(piece, orient, slot, height);
		clearRows(piece, orient, height);
		return true;
	}

	private void placePiece(int piece, int orient, int slot, int height) {
		// For each column in the piece
		for (int col = 0; col < pWidth[piece][orient]; col++) {
			int colBottom = height + pBottom[piece][orient][col];
			int colTop = height + pTop[piece][orient][col];

			// Adjust top and max height heuristic
			top[slot + col] = colTop;
			if (colTop > maxHeight) {
				heuristic += weights.maxHeight * (colTop - maxHeight);
				maxHeight = colTop;
			}
			// For each field in piece-column - bottom to top
			for (int row = colBottom; row < colTop; row++) {
				field[row][col + slot] = turn;
				heuristic += weights.topHeights[col + slot];
			}
			// Adjust holes heuristic by looking for new holes under the col
			while (--colBottom > 0 && field[colBottom][col + slot] == 0)
				heuristic += weights.holes;
		}
	}

	private void clearRows(int piece, int orient, int height) {
		// Check for full rows - starting at the top of the piece
		for (int row = height + pHeight[piece][orient] - 1; row >= height; row--) {
			boolean full = true;

			// Is this row full?
			for (int col = 0; col < State.COLS; col++) {
				if (field[row][col] == 0) {
					full = false;
					break;
				}
			}

			if (full)
				removeRow(row);
		}
	}

	private void removeRow(int row) {
		int newMaxHeight = 0;

		// For each column in row
		for (int col = 0; col < State.COLS; col++) {
			// Slide down all bricks
			for (int r = row; r < top[col]; r++)
				field[r][col] = field[r + 1][col];

			// Lower the top
			top[col]--;
			heuristic -= weights.topHeights[col];

			// If a hole opened up, andjust top and heuristic
			while (top[col] > 0 && field[top[col] - 1][col] == 0) {
				heuristic -= weights.topHeights[col];
				heuristic -= weights.holes;
				top[col]--;
			}

			// Find the new max height
			if (top[col] > newMaxHeight)
				newMaxHeight = top[col];
		}

		heuristic += weights.rowsCleared;
		heuristic -= weights.maxHeight * (maxHeight - newMaxHeight);
		maxHeight = newMaxHeight;
	}

}


public class PlayerSkeleton {

	private Simulator gameSim ;

	public PlayerSkeleton(int width, Weights weights) {
    gameSim = new Simulator();
    gameSim.weights = weights;
	}

  public double playAndReturnScore() {
		while(!gameSim.simMove(pickMove(gameSim,Moves.getLegalMoves(gameSim.randomPiece())))) {}
    return gameSim.cleared;
  }

	private double forwardLookAvg(Simulator s, int maxdepth) {
		double average = 0;
		Simulator sim = new Simulator();

		// For all possible pieces
		for (int piece = 0; piece < State.N_PIECES; piece++) {
			int numMoves = Moves.getNumMoves(piece);
			double pieceBestHeu = Double.POSITIVE_INFINITY;

			// Try all possible moves for piece
			for (int move = 0; move < numMoves; move++) {
				sim.revertTo(s);
				if (!sim.simMove(move, piece))
					continue;

				double heu;
				if (maxdepth != 1)
					heu = forwardLookAvg(sim, maxdepth - 1);
				else
					heu = sim.getHeuristic();

				if (heu < pieceBestHeu)
					pieceBestHeu = heu;
			}

			average += pieceBestHeu;
		}

		average /= State.N_PIECES;
		return average;
	}

	// implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		Simulator sim = new Simulator();
		int bestMove = 0;
		double bestHeuristic = Double.POSITIVE_INFINITY;

		for (int move = 0; move < legalMoves.length; move++) {
			sim.revertTo(gameSim);
			if (!sim.simMove(move, s.getNextPiece()))
				continue;

			double heu = forwardLookAvg(sim, 1);
			if (heu < bestHeuristic) {
				bestMove = move;
				bestHeuristic = heu;
			}
		}

		gameSim.simMove(bestMove, s.getNextPiece());
		return bestMove;
	}
}
