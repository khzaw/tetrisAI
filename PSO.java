import java.util.*;

class Particle {
	public static double globalBest = -1;
	public static double[] globalBestPos;

	private double[] pos, vel, bestPos;
	private double best = -1, w;

	private static double bestP = -1;
	private static Particle bestParticle;

	public static void resetBestParticle() {
		bestP = -1;
		bestParticle = null;
	}

	public Particle() {
		pos = Features.randomWeights(State.COLS);
		vel = Features.randomWeights(State.COLS);
		w = 0.9;
	}

	private void update(double eval, double[] pos) {
		if (eval > best) {
			bestPos = Arrays.copyOf(pos, pos.length);
			best = eval;

			if (eval > globalBest) {
				globalBestPos = Arrays.copyOf(pos, pos.length);
				globalBest = eval;
			}
		}

		if (eval > bestP) {
			bestP = eval;
			bestParticle = this;
		}
	}

	public double eval(double[] pos) {
		double avg = 0;
		PlayerSkeleton p = new PlayerSkeleton(State.ROWS-10, State.COLS, pos);
		for (int i = 0; i < 3; i++)
			avg += p.playAndReturnScore();
		return avg / 3;
	}

	public void updateW(int numIter, int iter) {
		w = 0.9 - (0.5 / numIter) * iter;
	}

	public void move(int numIter, int iter) {
		double rLocal = Math.random(), rGroup = Math.random();

		update(eval(pos), pos);
//		updateW(numIter, iter);

		for (int i = 0; i < pos.length; i++) {
			vel[i] = (0.5 + Math.random()/2) * vel[i]
			       + 0.8 * rLocal * (bestPos[i] - pos[i])
			       + 0.8 * rGroup * (bestParticle.pos[i] - pos[i]);
			pos[i] += vel[i];
		}
	}
}

class PSOTrainer {
	private Particle[] particles;

	public PSOTrainer(int numParticles) {
		particles = new Particle[numParticles];
		for (int i = 0; i < particles.length; i++)
			particles[i] = new Particle();
	}

	public double[] train(int numIter) {
		for (int iter = 0; iter < numIter; iter++) {
			Particle.resetBestParticle();
			for (Particle p : particles)
				p.move(numIter, iter);
			System.out.format("Iter: %d/%d - global best: %f\n", iter, numIter, Particle.globalBest);
		}

		System.out.format("%s\n", Arrays.toString(Particle.globalBestPos));
		return Particle.globalBestPos;
	}
}