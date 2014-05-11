package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.worldgen.BiomeNoiseGenerator;
import com.pi.bukkit.worldgen.DefferedDataPopulator;
import com.pi.bukkit.worldgen.LayeredOctaveNoise;
import com.pi.bukkit.worldgen.floatingisland.FloatingIslandPlugin;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;
import com.pi.bukkit.worldgen.floatingisland.gen.base.Baseline;
import com.pi.bukkit.worldgen.floatingisland.gen.base.EdgeCleaner;
import com.pi.bukkit.worldgen.floatingisland.gen.base.RiverContainer;
import com.pi.bukkit.worldgen.floatingisland.gen.base.RiverSmoother;
import com.pi.bukkit.worldgen.floatingisland.gen.base.VoronoiBase;
import com.pi.bukkit.worldgen.floatingisland.gen.decor.BiomeDecoratorPopulator;
import com.pi.bukkit.worldgen.floatingisland.gen.terrain.LakesGenerator;

public class FloatingIslandGenerator extends ChunkGenerator {

	private FloatingIslandPlugin plugin;

	private DefferedDataPopulator dPop = new DefferedDataPopulator();

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList((BlockPopulator) dPop, new LakesGenerator(),
				new BiomeDecoratorPopulator());
	}

	public FloatingIslandGenerator(FloatingIslandPlugin plugin) {
		this.plugin = plugin;
	}

	final void setBlockInternal(byte[][] result, int x, int y, int z, byte blkid) {
		if (y < 0 || (y >> 4) >= result.length) {
			return;
		}
		if (result[y >> 4] == null) {
			result[y >> 4] = new byte[4096];
		}
		result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	}

	final short getBlockInternal(byte[][] result, int x, int y, int z) {
		if (result[y >> 4] == null) {
			return (short) 0;
		}
		return result[y >> 4][((y & 0xF) << 8) | (z << 4) | x];
	}

	final void setBlock(int chunkX, int chunkZ, byte[][] result, int x, int y,
			int z, Object block) {
		setBlockInternal(result, x, y, z,
				(byte) IslandConfig.blockSpecToID(block));
		short data = IslandConfig.blockSpecToData(block);
		if (data != 0) {
			dPop.addBlock(chunkX, chunkZ, x, y, z, data);
		}
	}

	@Override
	public byte[][] generateBlockSections(World w, Random random, int chunkX,
			int chunkZ, BiomeGrid biomeOriginal) {
		byte[][] result = new byte[24][];

		BiomeIntensityGrid biomes = new BiomeIntensityGrid(w, biomeOriginal,
				chunkX, chunkZ);
		Baseline baseline = new VoronoiBase(w, chunkX, chunkZ, biomes);
		// baseline = new EdgeCleaner(w, chunkX, chunkZ, biomes, baseline);

		RiverSmoother riverSmoother = new RiverSmoother(w, chunkX, chunkZ,
				biomes, baseline);
		baseline = riverSmoother;
		baseline = new RiverContainer(w, chunkX, chunkZ, biomes, baseline);

		int realX = chunkX << 4;
		int realZ = chunkZ << 4;

		NoiseGenerator noiseRoot = new SimplexNoiseGenerator(new Random(
				w.getSeed()));
		LayeredOctaveNoise noise = new LayeredOctaveNoise(noiseRoot, 11);
		noise.setScale(0, 0.001D); // Threshold
		noise.setScale(1, 0.01D); // Island map
		noise.setScale(2, 0.1D); // Dirt
		noise.setScale(3, 0.5D); // ext spike
		noise.setScale(4, 0.1D); // root spike
		noise.setScale(5, 0.1D); // lower coating
		noise.setScale(6, 0.0001D); // hill size
		noise.setScale(8, 0.05D);

		BiomeNoiseGenerator hillNoise = new BiomeNoiseGenerator(noiseRoot);
		BiomeNoiseGenerator islandMap = new BiomeNoiseGenerator(noiseRoot);
		for (Biome b : Biome.values()) {
			IslandConfig cfg = IslandConfig.forBiome(b);
			if (cfg != null) {
				hillNoise.setScale(b, cfg.hillNoise);
				islandMap.setScale(b, cfg.islandScale);
			} else {
				hillNoise.setScale(b, 0.02D);
				islandMap.setScale(b, 0.01D);
			}
		}

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int noiseX = realX + x;
				int noiseZ = realZ + z;
				Biome biome = biomes.getBiome(x, z);

				IslandConfig config = IslandConfig.forBiome(biome);

				noise.setScale(7, config.hillNoise); // hill

				int[] yS = baseline.getHeights(x, z);
				int[] riverMeta = riverSmoother.getMeta(x, z);
				for (int j = 0; j < yS.length; j++) {
					int y = yS[j];

					int islandTop = y;

					int dirt = (int) Math.round(noise.noise(2, noiseX, noiseZ)
							* config.grassNoise) + 3;

					int lower = (int) (noise
							.noise(5, noiseX, islandTop, noiseZ) * 2) + 1;

					int spikeHeight = (int) (noise.noise(4, noiseX, islandTop,
							noiseZ) * (config.rootSpikeMax - config.rootSpikeMin))
							+ config.rootSpikeMin;

					int spikeNoise = (int) (Math.pow(
							noise.noise(3, noiseX, islandTop, noiseZ), 2) * (config.extSpikeMax - config.extSpikeMin))
							+ config.extSpikeMin;

					int hillSize = (int) (noise.noise(6, noiseX, islandTop,
							noiseZ) * config.hillMax.length);
					int hillHeight = (int) (hillNoise.noise(
							biomes.getBiomeIntensity(x, z), noiseX, islandTop,
							noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
							+ config.hillMin[hillSize];

					int stone = 2 + spikeHeight + spikeNoise;

					islandTop += hillHeight;
					stone += hillHeight;

					int yI = islandTop + dirt;

					if (riverMeta != null
							&& (riverMeta[j] & RiverSmoother.RIVER_MASK) == RiverSmoother.RIVER_MASK) {
						if ((riverMeta[j] & RiverSmoother.RIVER_FALL_MASK) == RiverSmoother.RIVER_FALL_MASK) {
							for (int yO = -10; yO <= 0; yO++) {
								setBlock(chunkX, chunkZ, result, x, yI + yO, z,
										Material.GRASS);
							}
						} else if ((riverMeta[j] & RiverSmoother.RIVER_SHORE_MASK) == RiverSmoother.RIVER_SHORE_MASK) {
							for (int yO = -10; yO <= 0; yO++) {
								setBlock(chunkX, chunkZ, result, x, yI + yO, z,
										Material.GRASS);
							}
						} else {
							int depth = (riverMeta[j] & RiverSmoother.RIVER_DEPTH_MASK);
							for (int yO = 1; yO <= depth; yO++) {
								setBlock(chunkX, chunkZ, result, x, yI + yO, z,
										Material.WATER);
							}
							setBlock(
									chunkX,
									chunkZ,
									result,
									x,
									yI,
									z,
									(riverMeta[j] & RiverSmoother.RIVER_ICE_MASK) == RiverSmoother.RIVER_ICE_MASK ? Material.ICE
											: Material.WATER);
						}
					} else {
						setBlock(
								chunkX,
								chunkZ,
								result,
								x,
								yI,
								z,
								config.topCoating[yI % config.topCoating.length]);
					}

					yI--;
					for (; yI >= islandTop && yI >= 0; yI--) {
						setBlock(chunkX, chunkZ, result, x, yI, z,
								config.coating[yI % config.coating.length]);
					}
					for (; yI >= islandTop - lower && yI >= 0; yI--) {
						setBlock(chunkX, chunkZ, result, x, yI, z,
								config.lowerCoating[yI
										% config.lowerCoating.length]);
					}
					for (; yI >= islandTop - stone && yI >= 0; yI--) {
						Object type = config.foundation[yI
								% config.foundation.length];
						double glowNoise = noise.noise(8, x, yI, z);
						if (glowNoise > 0.45 && glowNoise < 0.55) {
							type = Material.GLOWSTONE;
						}
						setBlock(chunkX, chunkZ, result, x, yI, z, type);
					}
				}
			}
		}

		// System.out.println("BIOME: " + (biomeTime / 1000.0f) + "\tBASE: "
		// + (baselineTime / 1000.0f) + "\tEDGE: " + (edgeTime / 1000.0f)
		// + "\tRIVER: " + (riverTime / 1000f) + "\tGEN: "
		// + (genTime / 1000f));
		return result;
	}

	public FloatingIslandPlugin getPlugin() {
		return plugin;
	}

	@Override
	public Location getFixedSpawnLocation(World w, Random rand) {
		return new Location(w, 0, 64, 0);
		// int chunkX = (rand.nextInt(100) - 50) << 4;
		// int chunkZ = (rand.nextInt(100) - 50) << 4;
		//
		// int baseX = rand.nextInt(16);
		//
		// int offX = 1;
		// while (baseX - offX >= 0 || baseX + offX < 16) {
		// int baseZ = rand.nextInt(16);
		// if (baseX - offX >= 0) {
		// int offZ = 1;
		// while (baseZ - offZ >= 0 || baseZ + offZ < 16) {
		// if (baseZ - offZ >= 0) {
		// int y = w.getHighestBlockYAt(chunkX + baseX - offX,
		// chunkZ + baseZ - offZ);
		// if (y > 0) {
		// return new Location(w, chunkX + baseX - offX, y,
		// chunkZ + baseZ - offZ);
		// }
		// }
		// if (baseZ + offZ < 16) {
		// int y = w.getHighestBlockYAt(chunkX + baseX - offX,
		// chunkZ + baseZ + offZ);
		// if (y > 0) {
		// return new Location(w, chunkX + baseX - offX, y,
		// chunkZ + baseZ + offZ);
		// }
		// }
		// offZ++;
		// }
		// }
		// if (baseX + offX < 16) {
		// int offZ = 1;
		// while (baseZ - offZ >= 0 || baseZ + offZ < 16) {
		// if (baseZ - offZ >= 0) {
		// int y = w.getHighestBlockYAt(chunkX + baseX + offX,
		// chunkZ + baseZ - offZ);
		// if (y > 0) {
		// return new Location(w, chunkX + baseX + offX, y,
		// chunkZ + baseZ - offZ);
		// }
		// }
		// if (baseZ + offZ < 16) {
		// int y = w.getHighestBlockYAt(chunkX + baseX + offX,
		// chunkZ + baseZ + offZ);
		// if (y > 0) {
		// return new Location(w, chunkX + baseX + offX, y,
		// chunkZ + baseZ + offZ);
		// }
		// }
		// offZ++;
		// }
		// }
		// offX++;
		// }
		// return getFixedSpawnLocation(w, rand);
	}
}
