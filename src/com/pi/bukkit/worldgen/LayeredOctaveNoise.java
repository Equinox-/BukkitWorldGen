package com.pi.bukkit.worldgen;

import java.util.Arrays;

import org.bukkit.util.Vector;
import org.bukkit.util.noise.NoiseGenerator;

public class LayeredOctaveNoise {
	private final NoiseGenerator gen;
	// private int layers;
	private Vector[] scale;
	private double[] freq;
	private double[] amp;
	private int[] blendSize;

	public LayeredOctaveNoise(NoiseGenerator gen, int layers) {
		this.gen = gen;
		// this.layers = layers;
		this.scale = new Vector[layers];
		this.freq = new double[layers];
		this.amp = new double[layers];
		this.blendSize = new int[layers];

		setScale(new Vector(1, 1, 1));
		setFrequency(.5D);
		setAmplitude(.5D);
		setBlend(0);
	}

	public void setFrequency(double d) {
		Arrays.fill(freq, d);
	}

	public void setAmplitude(double d) {
		Arrays.fill(amp, d);
	}

	public void setScale(Vector d) {
		Arrays.fill(scale, d);
	}

	public void setBlend(int i) {
		Arrays.fill(blendSize, i);
	}

	public void setFrequency(int layer, double d) {
		freq[layer] = d;
	}

	public void setBlend(int layer, int i) {
		blendSize[layer] = i;
	}

	public void setAmplitude(int layer, double d) {
		amp[layer] = d;
	}

	public void setScale(int layer, Vector d) {
		scale[layer] = d;
	}

	public void setScale(int layer, double d) {
		scale[layer] = new Vector(d, d, d);
	}

	public double noise(int layer, double x, double y) {
		double ttl = 0;
		for (double xO = -blendSize[layer]; xO <= blendSize[layer]; xO++) {
			for (double yO = -blendSize[layer]; yO <= blendSize[layer]; yO++) {
				ttl += cleanNoise(gen.noise((x + xO) * scale[layer].getX(),
						(y + yO) * scale[layer].getY(), layer, 2, freq[layer],
						amp[layer], true));
			}
		}
		int bArea = (1 + (2 * blendSize[layer]));
		return ttl / (bArea * bArea);
	}

	public double noise(int layer, double x, double y, double z) {
		double ttl = 0;
		for (double xO = -blendSize[layer]; xO <= blendSize[layer]; xO++) {
			for (double zO = -blendSize[layer]; zO <= blendSize[layer]; zO++) {
				ttl += cleanNoise(gen.noise((x + xO) * scale[layer].getX(), y
						* scale[layer].getY(), (layer * 100000D)
						+ ((z + zO) * scale[layer].getZ()), 2, freq[layer],
						amp[layer], true));
			}
		}
		int bArea = (1 + (2 * blendSize[layer]));
		return ttl / (bArea * bArea);
	}

	public double noiseSlope(int layer, double x, double y, double z,
			double dX, double dY, double dZ) {
		double sDX = scale[layer].getX() * dX;
		double sDY = scale[layer].getY() * dY;
		double sDZ = scale[layer].getZ() * dZ;
		return (noise(layer, x + dX, y + dY, z + dZ) - noise(layer, x, y, z))
				/ (Math.sqrt(sDX * sDX + sDY * sDY + sDZ * sDZ));
	}

	private static double cleanNoise(double d) {
		return Math.abs((d + 1D) / 2D);
	}
}
