package com.pi.bukkit.worldgen;

import org.bukkit.block.Biome;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.NoiseGenerator;

public class BiomeNoiseGenerator {
	private LayeredOctaveNoise noise;

	public BiomeNoiseGenerator(NoiseGenerator g) {
		noise = new LayeredOctaveNoise(g, Biome.values().length);
	}

	public void setScale(Biome b, Vector scale) {
		noise.setScale(b.ordinal(), scale);
	}

	public void setScale(Biome b, double scale) {
		noise.setScale(b.ordinal(), scale);
	}

	public double noise(float[] biome, double x, double y) {
		return noise(biome, x, y, 0);
	}

	public double noise(float[] biome, double x, double y, double z) {
		double total = 0;
		double tWeight = 0;
		for (Biome b : Biome.values()) {
			if (biome[b.ordinal()] > 0) {
				total += biome[b.ordinal()] * noise.noise(b.ordinal(), x, y, z);
				tWeight += biome[b.ordinal()];
			}
		}
		return tWeight == 0 ? 0.0 : total / tWeight;
	}

	public Vector gradient(float[] biome, double x, double y, double z) {
		double here = noise(biome, x, y, z);
		return new Vector(noise(biome, x + 1, y, z) - here, noise(biome, x,
				y + 1, z) - here, noise(biome, x, y, z + 1) - here);
	}
}
