package com.pi.bukkit.worldgen.floatingisland.gen.base;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.OctaveGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import com.pi.bukkit.worldgen.BiomeNoiseGenerator;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;
import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

public class BaselineLayer extends Baseline {
	private final BiomeNoiseGenerator islandMap;
	private final NoiseGenerator noiseRoot;

	public BaselineLayer(final World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing) {
		super(w, chunkX, chunkZ, backing, GenerationTuning.HEIGHT_OVERSAMPLE);
		this.noiseRoot = new NoiseGenerator() {
			private final OctaveGenerator gen = new SimplexOctaveGenerator(
					new Random(w.getSeed()), 3);

			@Override
			public double noise(double x, double y, double z) {
				return gen.noise(x, y, z, 0.5, 0.5);
			}
		};
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
		int resultHead = 0;
		for (int x = -heightMapOversample; x < 16 + heightMapOversample; x++) {
			for (int z = -heightMapOversample; z < 16 + heightMapOversample; z++) {
				resultHead = 0;

				int noiseX = (chunkX << 4) + x;
				int noiseZ = (chunkZ << 4) + z;
				Biome biome = biomes.getBiome(x, z);

				IslandConfig config = IslandConfig.forBiome(biome);

				double islandYScale = 0;
				{
					double threshTotal = 0;
					for (Biome b : Biome.values()) {
						threshTotal += biomes.getBiomeIntensity(x, z, b);
						islandYScale += biomes.getBiomeIntensity(x, z, b)
								* IslandConfig.forBiome(b).islandScale.getY();
					}
					islandYScale /= threshTotal;
				}

				for (int y = 0; y < 128; y++) {
					float thresh = .5f;
					final double maskHere = islandMap.noise(
							biomes.getBiomeIntensity(x, z), noiseX, y, noiseZ);
					final double maskBelow = islandMap.noise(
							biomes.getBiomeIntensity(x, z), noiseX, y - 1,
							noiseZ);
					final double maskAbove = islandMap.noise(
							biomes.getBiomeIntensity(x, z), noiseX, y + 1,
							noiseZ);
					final double maskDiff = ((Math.abs(maskHere - maskBelow) + Math
							.abs(maskHere - maskAbove))) / 2.0;

					if (maskHere > thresh && maskBelow < maskHere
							&& maskAbove < maskHere && maskDiff > 1E-5) {
						results[resultHead++] = y;
						int islandTop = y;

						int dirt = (int) (config.grassNoise + 3);

						islandTop += config.getAbsoluteHillMax();

						int yI = islandTop + dirt;

						y = Math.max(
								y,
								yI
										+ ((2 + config.rootSpikeMax + config.extSpikeMax) / 4)
										+ (int) ((noiseRoot.noise(
												noiseX * 0.01D,
												islandTop * 0.01D,
												noiseZ * 0.01D) * .6 + 0.4)
												* Math.sqrt(islandYScale / 0.0075D) * (2 + config.rootSpikeMax)));
					}
				}
				int[] cpy = new int[resultHead];
				System.arraycopy(results, 0, cpy, 0, resultHead);
				setHeights(x, z, cpy);
			}
		}
	}
}
