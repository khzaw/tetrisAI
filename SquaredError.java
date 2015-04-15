import java.util.*;

class MoveInfo {
	public Features feat;
	public double lt;
	public MoveInfo(Features feat) {
		this.feat = feat; this.lt = 0;
	}
}

class SquaredError {
	private int rows, cols;

	public SquaredError(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
	}

	public double[] train(int numIter, int numGames) {
		Collection<List<MoveInfo>> games
			= new ArrayList<List<MoveInfo>>(numGames);
		double[] w = Features.jacobWeights(cols);

		while (numIter-- > 0) {
			PlayerSkeleton p = new PlayerSkeleton(rows, cols, w);
			int numgames = numGames;
			while (numgames-- > 0) {
				List<MoveInfo> gameMoves = new ArrayList<MoveInfo>();
				int score = p.playAndGatherMoveInfo(gameMoves);
				System.out.format("Game %d - Score: %d\n", numGames - numgames + 1, score);
				calcLastTerm(w, gameMoves, 0.6);
				games.add(gameMoves);
			}
System.out.format("W: %s\n", Arrays.toString(w));

			//System.out.format("\nIterration %d -------------------- \n", numIter);
			w = steepestDescent(w, games, 1000, 0.99, 0.0001, 20000, 5);
		}
		return w;
	}

	private void calcLastTerm(double[] w, List<MoveInfo> gameMoves, double lambda) {
		ListIterator<MoveInfo> moves = gameMoves.listIterator(gameMoves.size());
		double lastHeu, heu;
		MoveInfo move, lastMove = gameMoves.get(0);

		// temp save lambda powers in lt
		//lastMove.lt = 1/lambda;
		for (MoveInfo m : gameMoves) {
			m.lt = lastMove.lt * lambda;
			lastMove = m;
		}

		move = moves.previous(); // Last move doesn't contribute to LT
		lastHeu = move.feat.heu(w);
		lastMove = move;
		while (moves.hasPrevious()) {
			move = moves.previous();
			heu = move.feat.heu(w);
			move.lt *= (lastMove.feat.rowsCleared + lastHeu - heu);
			lastHeu = heu;
			lastMove = move;
		}
	}

	private double[] steepestDescent(double[] wt, Collection<List<MoveInfo>> gameMoves,
	                                 double stepSize, double stepChange,
	                                 double threshold, int cutoff, int count)
	{
		double[] w, bestW = null, dw = new double[wt.length], grad = new double[wt.length];
		double eval, lastEval, bestEval = Double.POSITIVE_INFINITY;

System.out.format("\n\nFirst: %f\n", eval(wt, gameMoves));

		while (count-- > 0) {
System.out.format("\n\nRandomStart: %d ----------------- best: %f %s\n", count, bestEval, Arrays.toString(bestW));

			w = Features.randomWeights(cols);
			arrMinus(w, wt, dw);
			eval = eval(dw, gameMoves);
			lastEval = eval + threshold + 1;

			int cut = cutoff;
			double stepS = stepSize;
			while (Math.abs(lastEval - eval) > threshold && cut-- > 0) {
				gradient(dw, gameMoves, grad);

				for (int i = 0; i < grad.length; i++)
					w[i] -= stepS * grad[i];
				stepS *= stepChange;

				lastEval = eval;
				arrMinus(w, wt, dw);
				eval = eval(dw, gameMoves);

				if (eval < bestEval) {
					bestEval = eval;
					bestW = w;
				}
//System.out.format("diff: %f, stepS: %f\n", Math.abs(lastEval - eval), stepS);
			}
		}

		return bestW;
	}

	private double eval(double[] dw, Collection<List<MoveInfo>> games) {
		double res = 0.0;
		for (Iterable<MoveInfo> gameMoves : games)
			for (MoveInfo move : gameMoves) {
				double tmp = move.feat.heu(dw) - move.lt;
				res += tmp * tmp;
			}
		return res;
	}

	private void gradient(double[] dw, Collection<List<MoveInfo>> games, double[] grad) {
		int[] featarr;
		double len = 0.0;

		for (int i = 0; i < grad.length; i++)
			grad[i] = 0.0;

		for (Iterable<MoveInfo> gameMoves : games) {
			for (MoveInfo move : gameMoves) {
				double mult = 2 * (move.feat.heu(dw) - move.lt);
				featarr = move.feat.toArray();
				for (int i = 0; i < grad.length; i ++) {
					grad[i] += featarr[i] * mult;
					len += grad[i] * grad[i];
				}
			}
		}

		len = Math.sqrt(len);
		for (int i = 0; i < grad.length; i++)
			grad[i] /= len;
	}

	private void arrMinus(double[] w1, double[] w2, double[] dest) {
		for (int i = 0; i < w1.length; i++)
			dest[i] = w1[i] - w2[i];
	}
}