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

/**
 * Generates a baseline from the maximas of 3D noise. Typically varies quite a
 * bit. Rivers are not recommended.
 * 
 * @author westin
 * 
 */
public class MaximaBase extends Baseline {
	private final BiomeNoiseGenerator islandMap;
	private final NoiseGenerator noiseRoot;
	private final BiomeIntensityGrid tempGrid;

	public MaximaBase(final World w, int chunkX, int chunkZ,
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

	private void resetRiverBiome() {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				float[] intensity = tempGrid.getBiomeIntensity(x, z);

				// Overwrite -> River
				Biome best = null;
				for (Biome b : Biome.values()) {
					if (!RiverSmoother.isRiver(b)) {
						if (best == null
								|| intensity[b.ordinal()] > intensity[best
										.ordinal()]) {
							best = b;
						}
					}
				}
				tempGrid.setBiome(x, z, best);
			}
		}
	}

	public void regenerateLayer() {
		allocHeightMap();
		resetRiverBiome();

		int[] results = new int[128];
		int resultHead = 0;
		for (int x = -heightMapOversample; x < 16 + heightMapOversample; x++) {
			for (int z = -heightMapOversample; z < 16 + heightMapOversample; z++) {
				resultHead = 0;

				int noiseX = (chunkX << 4) + x;
				int noiseZ = (chunkZ << 4) + z;
				Biome biome = tempGrid.getBiome(x, z);

				IslandConfig config = IslandConfig.forBiome(biome);

				double islandYScale = 0;
				{
					double threshTotal = 0;
					for (Biome b : Biome.values()) {
						threshTotal += tempGrid.getBiomeIntensity(x, z, b);
						islandYScale += tempGrid.getBiomeIntensity(x, z, b)
								* IslandConfig.forBiome(b).islandScale.getY();
					}
					islandYScale /= threshTotal;
				}
				double maskLast = -1;
				boolean longjmp = false;

				for (int y = 0; y < 128; y++) {
					float thresh = .5f;
					final double maskHere = islandMap
							.noise(tempGrid.getBiomeIntensity(x, z), noiseX, y,
									noiseZ);
					final double maskBelow = islandMap.noise(
							tempGrid.getBiomeIntensity(x, z), noiseX, y - 1,
							noiseZ);
					final double maskAbove = islandMap.noise(
							tempGrid.getBiomeIntensity(x, z), noiseX, y + 1,
							noiseZ);
					final double maskDiff = ((Math.abs(maskHere - maskBelow) + Math
							.abs(maskHere - maskAbove))) / 2.0;
					if (maskLast >= 0
							&& longjmp
							&& maskAbove > maskBelow
							&& (Math.abs(maskAbove - maskHere) < Math
									.abs(maskHere - maskLast) || Math
									.signum(maskAbove - maskHere) != Math
									.signum(maskHere - maskLast))) {
						// reverse longjmp
						y -= 5;
						longjmp = false;
						maskLast = -1;
						continue;
					}

					if (maskHere > thresh && maskBelow < maskHere
							&& maskAbove < maskHere && maskDiff > 1E-5) {
						results[resultHead++] = y;
						int islandTop = y;

						int dirt = (int) (config.grassNoise + 3);

						islandTop += config.getAbsoluteHillMax();

						int yI = islandTop + dirt;

						longjmp = false;
						y = Math.max(
								y,
								yI
										+ ((2 + config.rootSpikeMax + config.extSpikeMax) / 4)
										+ (int) ((noiseRoot.noise(
												noiseX * 0.01D,
												islandTop * 0.01D,
												noiseZ * 0.01D) * .6 + 0.4)
												* Math.sqrt(islandYScale / 0.0075D) * (2 + config.rootSpikeMax)));
						maskLast = -1;
					}
					if (maskLast >= 0 && maskAbove < maskBelow) {
						y += 5;
						longjmp = true;
					} else {
						longjmp = false;
					}
					maskLast = maskHere;
				}
				int[] cpy = new int[resultHead];
				System.arraycopy(results, 0, cpy, 0, resultHead);
				setHeights(x, z, cpy);
			}
		}
	}
}
