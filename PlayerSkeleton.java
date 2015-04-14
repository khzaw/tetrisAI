class Moves extends State {
	public static int[][][] allMoves = legalMoves;
	public static int getNumMoves(int piece) {
		return allMoves[piece].length;
	}
	public static int[][] getLegalMoves(int piece) {
		return allMoves[piece];
	}
}

class Weights {
	public int numHoles;
	public int maxHeight;
	public int rowsCleared;
	public int colHeights;
	public int adjColHeightDiffs;

	public Weights() {}

	public int[] toArray() {
		int[] arr = new int[5];
		int wi = 0;

		arr[wi++] = numHoles;
		arr[wi++] = maxHeight;
		arr[wi++] = rowsCleared;
		arr[wi++] = colHeights;
		arr[wi++] = adjColHeightDiffs;
		return arr;
	}

	public static Weights fromArray(int[] arr) {
		Weights w = new Weights();
		int wi = 0;

		w.numHoles = arr[wi++];
		w.maxHeight = arr[wi++];
		w.rowsCleared = arr[wi++];
		w.colHeights = arr[wi++];
		w.adjColHeightDiffs = arr[wi++];
		return w;
	}

	public static Weights jacobWeights() {
		Weights w = new Weights();
		w.colHeights = 1;
		w.adjColHeightDiffs = 3;
		w.maxHeight = 3;
		w.numHoles = 10;
		w.rowsCleared = -4;
		return w;
	}

	public static Weights randomWeights() {
		Weights w = new Weights();
		w.colHeights = getRandom();
		w.adjColHeightDiffs = getRandom();
		w.maxHeight = getRandom();
		w.numHoles = getRandom();
		w.rowsCleared = getRandom();
		return w;
	}

	public static int getRandom() {
		java.util.Random r = new java.util.Random();
		return r.nextInt(100);
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
	public int turn, maxHeight, rowsCleared, rows, cols;
	public Weights weights;

	// For quick heuristics, simMove keeps this field updated for:
	// - Max height
	// - Column Heights
	// - Holes
	// - Cleared
	public int heuristic;

	public Simulator(Simulator sim) {
		this(sim.rows, sim.cols, sim.weights);
	}

	public Simulator(int rows, int cols, Weights w) {
		this.weights = w;
		this.rows = rows;
		this.cols = cols;
		this.field = new int[rows][cols];
		this.top = new int[cols];
	}

	public void revertTo(Simulator sim) {
		System.arraycopy(sim.top, 0, top, 0, top.length);
		for (int i = 0; i < field.length; i++)
			System.arraycopy(sim.field[i], 0, field[i], 0, field[i].length);

		turn = sim.turn;
		rowsCleared = sim.rowsCleared;
		heuristic = sim.heuristic;
		maxHeight = sim.maxHeight;
	}

	public int getHeuristic() {
		int sum = heuristic;

		for(int i = 0; i < top.length - 1; i++)
			sum += Math.abs(top[i] - top[i+1]) * weights.adjColHeightDiffs;

		return sum;
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
		if(height + pHeight[piece][orient] >= this.rows)
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
				heuristic += weights.colHeights;
			}
			// Adjust holes heuristic by looking for new holes under the col
			while (--colBottom > 0 && field[colBottom][col + slot] == 0)
				heuristic += weights.numHoles;
		}
	}

	private void clearRows(int piece, int orient, int height) {
		// Check for full rows - starting at the top of the piece
		for (int row = height + pHeight[piece][orient] - 1; row >= height; row--) {
			boolean full = true;

			// Is this row full?
			for (int col = 0; col < this.cols; col++) {
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
		rowsCleared++;

		// For each column in row
		for (int col = 0; col < this.cols; col++) {
			// Slide down all bricks
			for (int r = row; r < top[col]; r++)
				field[r][col] = field[r + 1][col];

			// Lower the top
			top[col]--;
			heuristic -= weights.colHeights;

			// If a hole opened up, adjust top and heuristic
			while (top[col] > 0 && field[top[col] - 1][col] == 0) {
				heuristic -= weights.colHeights;
				heuristic -= weights.numHoles;
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
	private Simulator gameSim;

	public PlayerSkeleton(Weights w, int rows,int  cols) {
		gameSim = new Simulator(rows,cols,w);
	}

	public int playAndReturnScore() {
		int piece = randomPiece();
		while(gameSim.simMove(pickMove(Moves.getLegalMoves(piece), piece), piece))
			piece = randomPiece();
		return gameSim.rowsCleared;
	}

	private int forwardLookAvg(Simulator s, int maxdepth) {
		int average = 0;
		Simulator sim = new Simulator(s);

		// For all possible pieces
		for (int piece = 0; piece < State.N_PIECES; piece++) {
			int numMoves = Moves.getNumMoves(piece);
			int pieceBestHeu = Integer.MAX_VALUE;

			// Try all possible moves for piece
			for (int move = 0; move < numMoves; move++) {
				sim.revertTo(s);
				if (!sim.simMove(move, piece))
					continue;

				int heu;
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
	public int pickMove(int[][] legalMoves, int piece) {
		Simulator sim = new Simulator(gameSim);
		int bestMove = 0;
		double bestHeuristic = Double.POSITIVE_INFINITY;

		for (int move = 0; move < legalMoves.length; move++) {
			sim.revertTo(gameSim);
			if (!sim.simMove(move, piece))
				continue;

			// double heu = forwardLookAvg(sim, 1);
			double heu = sim.getHeuristic();
			if (heu < bestHeuristic) {
				bestMove = move;
				bestHeuristic = heu;
			}
		}

		return bestMove;
	}

	public int randomPiece() {
		return randomWithRange(0,6);
	}

	public static int randomWithRange(int min, int max) {
		int range = (max - min) + 1;
		return (int)(Math.random() * range) + min;
	}

	public static void main(String[] args) {
		State s = new State();
		TFrame tFrame = new TFrame(s);

		// Genetic gen = new Genetic(10, State.ROWS-10, State.COLS);
		// Weights w = gen.train(10); // Number of generations

		Weights w = Weights.jacobWeights();
		PlayerSkeleton p = new PlayerSkeleton(w, State.ROWS, State.COLS);

		while(!s.hasLost()) {
			int move = p.pickMove(s.legalMoves(), s.getNextPiece());
			p.gameSim.simMove(move, s.getNextPiece());
			s.makeMove(move);
			s.draw();
			tFrame.setScoreLabel(s.getRowsCleared());
			s.drawNext(0,0);
			try {
				Thread.sleep(00);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
}
