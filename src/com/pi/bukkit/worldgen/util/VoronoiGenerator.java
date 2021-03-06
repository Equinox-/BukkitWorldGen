package com.pi.bukkit.worldgen.util;

import java.util.Random;

import org.bukkit.util.Vector;

public class VoronoiGenerator {

	private static final double SQRT_2 = 1.4142135623730950488;
	private static final double SQRT_3 = 1.7320508075688772935;

	private long seed;
	private short distanceMethod;

	public VoronoiGenerator(long seed, short distanceMethod) {
		this.seed = seed;
		this.distanceMethod = distanceMethod;
	}

	private double getDistance(double xDist, double zDist) {
		switch (distanceMethod) {
		case 0:
			return Math.sqrt(xDist * xDist + zDist * zDist) / SQRT_2;
		case 1:
			return xDist + zDist;
		case 3:
			return Math.abs(xDist) + Math.abs(zDist);
		case 2:
			return Math.pow(Math.E, Math.sqrt(xDist * xDist + zDist * zDist)
					/ SQRT_2)
					/ Math.E;
		default:
			return 1.0;
		}
	}

	private double getDistance(double xDist, double yDist, double zDist) {
		switch (distanceMethod) {
		case 0:
			return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist)
					/ SQRT_3;
		case 1:
			return xDist + yDist + zDist;
		case 3:
			return Math.abs(xDist) + Math.abs(yDist) + Math.abs(zDist);
		default:
			return 1.0;
		}
	}

	public short getDistanceMethod() {
		return distanceMethod;
	}

	public long getSeed() {
		return seed;
	}

	public Vector candidateVoronoi(double x, double z) {
		int xInt = (x > .0 ? (int) x : (int) x - 1);
		int zInt = (z > .0 ? (int) z : (int) z - 1);

		double minDist = 32000000.0;

		double xCandidate = 0;
		double zCandidate = 0;

		for (int zCur = zInt - 2; zCur <= zInt + 2; zCur++) {
			for (int xCur = xInt - 2; xCur <= xInt + 2; xCur++) {

				double xPos = xCur + valueNoise2D(xCur, zCur, seed);
				double zPos = zCur
						+ valueNoise2D(xCur, zCur, new Random(seed).nextLong());
				double xDist = xPos - x;
				double zDist = zPos - z;
				double dist = getDistance(xDist, zDist);

				if (dist < minDist) {
					minDist = dist;
					xCandidate = xPos;
					zCandidate = zPos;
				}
			}
		}
		return new Vector(xCandidate, 0, zCandidate);
	}

	public Vector candidateVoronoi(double x, double y, double z) {
		int xInt = (x > .0 ? (int) x : (int) x - 1);
		int yInt = (y > .0 ? (int) y : (int) y - 1);
		int zInt = (z > .0 ? (int) z : (int) z - 1);

		double minDist = 32000000.0;

		double xCandidate = 0;
		double yCandidate = 0;
		double zCandidate = 0;

		Random rand = new Random(seed);

		for (int zCur = zInt - 2; zCur <= zInt + 2; zCur++) {
			for (int yCur = yInt - 2; yCur <= yInt + 2; yCur++) {
				for (int xCur = xInt - 2; xCur <= xInt + 2; xCur++) {

					double xPos = xCur + valueNoise3D(xCur, yCur, zCur, seed);
					double yPos = yCur
							+ valueNoise3D(xCur, yCur, zCur, rand.nextLong());
					double zPos = zCur
							+ valueNoise3D(xCur, yCur, zCur, rand.nextLong());
					double xDist = xPos - x;
					double yDist = yPos - y;
					double zDist = zPos - z;
					double dist = getDistance(xDist, yDist, zDist);

					if (dist < minDist) {
						minDist = dist;
						xCandidate = xPos;
						yCandidate = yPos;
						zCandidate = zPos;
					}
				}
			}
		}
		return new Vector(xCandidate, yCandidate, zCandidate);
	}

	public double noise(double x, double z, double frequency) {
		x *= frequency;
		z *= frequency;
		Vector v = candidateVoronoi(x, z);
		double xDist = v.getX() - x;
		double zDist = v.getZ() - z;

		return getDistance(xDist, zDist);
	}

	public double noise(double x, double y, double z, double frequency) {
		x *= frequency;
		y *= frequency;
		z *= frequency;

		Vector v = candidateVoronoi(x, y, z);

		double xDist = v.getX() - x;
		double yDist = v.getY() - y;
		double zDist = v.getZ() - z;

		return getDistance(xDist, yDist, zDist);
	}

	public void setDistanceMethod(short distanceMethod) {
		this.distanceMethod = distanceMethod;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public static double valueNoise2D(int x, int z, long seed) {
		long n = (1619 * x + 6971 * z + 1013 * seed) & 0x7fffffff;
		n = (n >> 13) ^ n;
		return 1.0 - ((double) ((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
	}

	public static double valueNoise3D(int x, int y, int z, long seed) {
		long n = (1619 * x + 31337 * y + 6971 * z + 1013 * seed) & 0x7fffffff;
		n = (n >> 13) ^ n;
		return 1.0 - ((double) ((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
	}

}