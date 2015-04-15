import java.util.*;

class Moves extends State {
	public static int[][][] allMoves = legalMoves;
	public static int getNumMoves(int piece) {
		return allMoves[piece].length;
	}
	public static int[][] getLegalMoves(int piece) {
		return allMoves[piece];
	}
}

class Features {
	public int numHoles;
	public int maxHeight;
	public int rowsCleared;
	public int[] colHeights;
	public int[] adjColHeightDiffs;

	private int cols;

	private static int numFeatures(int cols) {
		return 3 + cols + cols - 1;
	}

	public Features(int cols) {
		colHeights = new int[cols];
		adjColHeightDiffs = new int[cols - 1];
		this.cols = cols;
	}

	public Features(Features feat) {
		this(feat.cols);
		System.arraycopy(feat.colHeights, 0, colHeights, 0, colHeights.length);
		System.arraycopy(feat.adjColHeightDiffs, 0,
		                 adjColHeightDiffs, 0, adjColHeightDiffs.length);
		rowsCleared = feat.rowsCleared;
		maxHeight = feat.maxHeight;
		numHoles = feat.numHoles;
	}

	public int[] toArray() {
		int[] arr = new int[numFeatures(cols) + 1];
		int wi = 1;

		arr[0] = 1; // for multiplying with the "offset weight";
		arr[wi++] = numHoles;
		arr[wi++] = maxHeight;
		arr[wi++] = rowsCleared;
		for (int i = 0; i < colHeights.length; i++)
			arr[wi++] = colHeights[i];
		for (int i = 0; i < adjColHeightDiffs.length; i++)
			arr[wi++] = adjColHeightDiffs[i];
		return arr;
	}

	public double heu(double[] w) {
		double heu = 0;
		int[] featarr = toArray();
		for (int i = 0; i < w.length; i++)
			heu += w[i] * featarr[i];
		return heu;
	}

	public static double[] randomWeights(int cols) {
		double[] w = new double[numFeatures(cols) + 1];
		for (int i = 0; i < w.length; i++)
			w[i] = Math.random()*10-5; // -1 to 1
		return w;
	}

	public static double[] jacobWeights(int cols) {
		double[] w = new double[numFeatures(cols) + 1];
		int wi = 1;

		w[0] = 0;
		w[wi++] = 5;
		w[wi++] = 1.5;
		w[wi++] = -2;
		for (int i = 0; i < cols; i++)
			w[wi++] = 0.5;
		for (int i = 0; i < cols - 1; i++)
			w[wi++] = 1.5;
		return w;
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
	private int[][] field;
	private int[] top;
	private int turn, score, rows, cols;
	private boolean lost = false;
	private Features feat;

	public Simulator(Simulator sim) {
		this(sim.rows, sim.cols);
	}

	public Simulator(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		this.field = new int[rows][cols];
		this.top = new int[cols];
		this.feat = new Features(cols);
	}

	public int getScore() {
		return score;
	}

	public void reset() {
		Arrays.fill(top, 0);
		for (int i = 0; i < field.length; i++)
			Arrays.fill(field[i], 0);

		turn = 0;
		score = 0;
		lost = false;
		this.feat = new Features(cols);
	}

	public void revertTo(Simulator sim) {
		System.arraycopy(sim.top, 0, top, 0, top.length);
		for (int i = 0; i < field.length; i++)
			System.arraycopy(sim.field[i], 0, field[i], 0, field[i].length);

		turn = sim.turn;
		score = sim.score;
		lost = sim.lost;
		feat = new Features(sim.feat);
	}

	public Features getFeatures() {
		for(int i = 0; i < top.length - 1; i++)
			feat.adjColHeightDiffs[i] = Math.abs(top[i] - top[i+1]);

		return new Features(feat);
	}

	public boolean hasLost() {
		return lost;
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
		if(height + pHeight[piece][orient] >= this.rows) {
			lost = true;
			return false;
		}

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
			feat.colHeights[slot + col] = colTop;

			if (colTop > feat.maxHeight)
				feat.maxHeight = colTop;

			// For each field in piece-column - bottom to top
			for (int row = colBottom; row < colTop; row++)
				field[row][col + slot] = turn;

			// Adjust holes feature by looking for new holes under the col
			while (--colBottom > 0 && field[colBottom][col + slot] == 0)
				feat.numHoles++;
		}
	}

	private void clearRows(int piece, int orient, int height) {
		feat.rowsCleared = 0;
		// Check for full rows - starting at the top of the piece
		for (int row = height + pHeight[piece][orient] - 1; row >= height; row--) {
			boolean full = true;

			// Is this row full?
			for (int col = 0; col < this.cols; col++)
				if (field[row][col] == 0) {
					full = false;
					break;
				}

			if (full)
				removeRow(row);
		}
	}

	private void removeRow(int row) {
		int newMaxHeight = 0;
		feat.rowsCleared++;
		score++;

		// For each column
		for (int col = 0; col < this.cols; col++) {
			// Slide down all bricks
			for (int r = row; r < top[col]; r++)
				field[r][col] = field[r + 1][col];

			// Lower the top
			top[col]--;
			feat.colHeights[col]--;

			// If a hole opened up, andjust top
			while (top[col] > 0 && field[top[col] - 1][col] == 0) {
				top[col]--;
				feat.colHeights[col]--;
				feat.numHoles--;
			}

			// Find the new max height
			if (top[col] > newMaxHeight)
				newMaxHeight = top[col];
		}

		feat.maxHeight = newMaxHeight;
	}
}


public class PlayerSkeleton {
	private Simulator gameSim;
	private double[] w;

	public PlayerSkeleton(int rows, int cols, double[] w) {
		this.w = w;
		gameSim = new Simulator(rows, cols);
	}

	public int playAndGatherMoveInfo(List<MoveInfo> gameMoves) {
		gameSim.reset();
		while (!gameSim.hasLost()) {
			int piece = randomPiece();
			int move = pickMove(Moves.getLegalMoves(piece), piece);
			gameMoves.add(new MoveInfo(gameSim.getFeatures()));
		}
		return gameSim.getScore();
	}

	public int playAndReturnScore() {
		gameSim.reset();
		while (!gameSim.hasLost()) {
			int piece = randomPiece();
			int move = pickMove(Moves.getLegalMoves(piece), piece);
		}
		return gameSim.getScore();
	}

	private int forwardLookAvg(Simulator s, int maxdepth) {
		int average = 0;
		Simulator sim = new Simulator(s);

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
					heu = sim.getFeatures().heu(w);

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

			double heu = sim.getFeatures().heu(w); //forwardLookAvg(sim, 1);
			if (heu < bestHeuristic) {
				bestMove = move;
				bestHeuristic = heu;
			}
		}

		gameSim.simMove(bestMove, piece);
		return bestMove;
	}

	private int randomPiece() {
		return (int)(Math.random() * 7);
	}

	public static void main(String[] args) {
		State s = new State();
		TFrame tFrame = new TFrame(s);

		SquaredError trainer = new SquaredError(State.ROWS, State.COLS);
		double[] w = trainer.train(6, 40);
		//double[] w = Features.jacobWeights(State.COLS);

		PlayerSkeleton bob = new PlayerSkeleton(State.ROWS, State.COLS, w);

		while(!s.hasLost()) {
			int move = bob.pickMove(s.legalMoves(), s.getNextPiece());
			s.makeMove(move);
			s.draw();
			tFrame.setScoreLabel(s.getRowsCleared());
			s.drawNext(0,0);
			//try {
			//	Thread.sleep(100);
			//} catch (InterruptedException e) {
			//	e.printStackTrace();
			//}
		}

		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
}
