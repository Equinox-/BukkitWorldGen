package com.pi.bukkit.worldgen.floatingisland.gen.base;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.OctaveGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import com.pi.bukkit.worldgen.BiomeNoiseGenerator;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;
import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;
import com.pi.bukkit.worldgen.util.VoronoiGenerator;

/**
 * Generates a baseline from a voroni diagram. Very flat baselines at different
 * heights.
 * 
 * @author westin
 * 
 */
public class VoronoiBase extends Baseline {

	private NoiseGenerator noiseRoot;
	private BiomeNoiseGenerator islandMap;
	private BiomeIntensityGrid tempGrid;
	private VoronoiGenerator voronoiNoise;

	public VoronoiBase(final World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing) {
		super(w, chunkX, chunkZ, backing, GenerationTuning.HEIGHT_OVERSAMPLE);
		this.tempGrid = backing.clone();
		this.noiseRoot = new NoiseGenerator() {
			private final OctaveGenerator gen = new SimplexOctaveGenerator(
					new Random(w.getSeed()), 3);

			@Override
			public double noise(double x, double y, double z) {
				return gen.noise(x, y, z, 0.5, 0.5);
			}
		};
		this.voronoiNoise = new VoronoiGenerator(w.getSeed(), (short) 0);
		this.islandMap = new BiomeNoiseGenerator(noiseRoot);
		for (Biome b : Biome.values()) {
			IslandConfig cfg = IslandConfig.forBiome(b);
			if (cfg != null) {
				islandMap.setScale(b, cfg.islandScale);
			} else {
				islandMap.setScale(b, 0.01D);
			}
		}
		regenerateLayer();
	}

	public void regenerateLayer() {
		allocHeightMap();

		int[] results = new int[128];
		int[] metaResults = new int[128];

		int resultHead = 0;

		final double maxHeight = 128;
		final double islandScale = .01;
		final int islandLayers = 3; // >1
		final int islandSpacing = 30;
		final double distScale = 0.02;

		for (int x = -heightMapOversample; x < 16 + heightMapOversample; x++) {
			for (int z = -heightMapOversample; z < 16 + heightMapOversample; z++) {
				resultHead = 0;
				int worldX = x + (chunkX << 4);
				int worldZ = z + (chunkZ << 4);
				int lHeightHere = -1;
				double interval = (maxHeight / (islandLayers - 1.0));
				double stepInterval = (maxHeight / (islandLayers - 1.0));
				for (int y = 0; y < maxHeight; y += stepInterval) {
					Vector nearest = voronoiNoise.candidateVoronoi(worldX
							* islandScale, y * islandScale, worldZ
							* islandScale);
					int height = (int) ((Math.abs(noiseRoot.noise(
							nearest.getX(), nearest.getY(), nearest.getZ())) * interval)
							- ((interval - stepInterval) / 2.0) + y);
					if (height < 0)
						height = 0;
					if (height >= 128)
						height = 128;

					// Smooths edges of island by noise function
					double dX = nearest.getX() - (worldX * islandScale);
					double dZ = nearest.getZ() - (worldZ * islandScale);
					double centerLevel = Math.sqrt(dX * dX + dZ * dZ);
					if (centerLevel > Math.pow(noiseRoot.noise(worldX
							* distScale, y * distScale, worldZ * distScale), 2)) {
						continue;
					}
					if (lHeightHere < 0
							|| Math.abs(lHeightHere - height) > islandSpacing) {
						results[resultHead] = height;
						metaResults[resultHead] = (int) (centerLevel / islandScale);
						++resultHead;
						lHeightHere = height;
					}
				}

				int[] cpy = new int[resultHead];
				int[] metaCpy = new int[resultHead];
				System.arraycopy(results, 0, cpy, 0, resultHead);
				System.arraycopy(metaResults, 0, metaCpy, 0, resultHead);
				setHeights(x, z, cpy);
				setMetadata(x, z, metaCpy);
			}
		}
	}
}
