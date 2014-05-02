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
import com.pi.bukkit.worldgen.floatingisland.gen.decor.BiomeDecoratorPopulator;
import com.pi.bukkit.worldgen.floatingisland.gen.terrain.LakesGenerator;

public class FloatingIslandGenerator extends ChunkGenerator {

	private FloatingIslandPlugin plugin;

	private DefferedDataPopulator dPop = new DefferedDataPopulator();

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList((BlockPopulator) dPop, new LakesGenerator(),
		/* new RiverGenerator(), new SnowPopulator(), */
		new BiomeDecoratorPopulator());
	}

	public FloatingIslandGenerator(FloatingIslandPlugin plugin) {
		this.plugin = plugin;
	}

	final void setBlockInternal(short[][] result, int x, int y, int z,
			short blkid) {
		if (y < 0 || (y >> 4) >= result.length) {
			return;
		}
		if (result[y >> 4] == null) {
			result[y >> 4] = new short[4096];
		}
		result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	}

	final short getBlockInternal(short[][] result, int x, int y, int z) {
		if (result[y >> 4] == null) {
			return (short) 0;
		}
		return result[y >> 4][((y & 0xF) << 8) | (z << 4) | x];
	}

	final void setBlock(int chunkX, int chunkZ, short[][] result, int x, int y,
			int z, Object block) {
		setBlockInternal(result, x, y, z, IslandConfig.blockSpecToID(block));
		short data = IslandConfig.blockSpecToData(block);
		if (data != 0) {
			dPop.addBlock(chunkX, chunkZ, x, y, z, data);
		}
	}

	private static float[][][] generateBiomeLevels(World w, int chunkX,
			int chunkZ, BiomeGrid biomes) {
		float[][][] biomeLevels = new float[16][16][Biome.values().length];
		int realX = chunkX << 4;
		int realZ = chunkZ << 4;
		for (int x = -16; x < 32; x++) {
			for (int z = -16; z < 32; z++) {
				Biome here = null;
				if (x >= 0 && z >= 0 && x < 16 && z < 16) {
					here = biomes.getBiome(x, z);
				} else {
					here = w.getBiome(realX + x, realZ + z);
				}
				for (int xO = -5; xO < 5; xO++) {
					for (int zO = -5; zO < 5; zO++) {
						int tX = x + xO;
						int tZ = z + zO;
						if (tX >= 0 && tZ >= 0 && tX < 16 && tZ < 16) {
							biomeLevels[tX][tZ][here.ordinal()] += 1.0 / (1.0 + (xO
									* xO + zO * zO)) / 10.89748;
							// Divide by magic to normalize
						}
					}
				}
			}
		}
		return biomeLevels;
	}

	@Override
	public short[][] generateExtBlockSections(World w, Random random,
			int chunkX, int chunkZ, BiomeGrid biomes) {
		short[][] result = new short[24][];

		float[][][] biomeIntensity = generateBiomeLevels(w, chunkX, chunkZ,
				biomes);

		int realX = chunkX << 4;
		int realZ = chunkZ << 4;

		NoiseGenerator noiseRoot = new SimplexNoiseGenerator(new Random(
				w.getSeed()));
		LayeredOctaveNoise noise = new LayeredOctaveNoise(noiseRoot, 11);
		noise.setScale(0, 0.001D); // Threshold
		noise.setScale(1, 0.01D); // Island map
		noise.setScale(2, 0.1D); // Dirt
		noise.setScale(3, 2D); // ext spike
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
		Biome lastBiome = biomes.getBiome(15, 15);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int noiseX = realX + x;
				int noiseZ = realZ + z;
				Biome biome = biomes.getBiome(x, z);
				IslandConfig config = IslandConfig
						.forBiome(biome == Biome.RIVER
								|| biome == Biome.FROZEN_RIVER ? lastBiome
								: biome);
				if (biome != Biome.RIVER && biome != Biome.FROZEN_RIVER) {
					lastBiome = biome;
				}

				noise.setScale(7, config.hillNoise); // hill

				double islandYScale = 0;
				{
					double threshTotal = 0;
					for (Biome b : Biome.values()) {
						threshTotal += biomeIntensity[x][z][b.ordinal()];
						islandYScale += biomeIntensity[x][z][b.ordinal()]
								* IslandConfig.forBiome(b).islandScale.getY();
					}
					islandYScale /= threshTotal;
				}

				for (int y = 0; y < 128; y++) {
					float thresh = .5f;
					thresh += (float) Math.pow(
							Math.abs(y - 64
									- (noise.noise(0, noiseX, noiseZ) - .5)
									* 32) / 64.0, 3) * .25f;

					double maskHere = islandMap.noise(biomeIntensity[x][z],
							noiseX, y, noiseZ);
					if (maskHere > thresh
							&& islandMap.noise(biomeIntensity[x][z], noiseX,
									y - 1, noiseZ) < maskHere
							&& islandMap.noise(biomeIntensity[x][z], noiseX,
									y + 1, noiseZ) < maskHere) {
						int iTop = y;

						int dirt = (int) Math.round(noise.noise(2, noiseX,
								noiseZ) * config.grassNoise) + 3;
						if (biome == Biome.RIVER || biome == Biome.FROZEN_RIVER) {
							dirt -= Math.min(2, config.riverGorgeDepth);
							iTop -= Math.max(0, config.riverGorgeDepth - 2);
						}

						int lower = (int) (noise.noise(5, noiseX, y, noiseZ) * 2) + 1;

						int spikeHeight = (int) (noise.noise(4, noiseX, noiseZ) * (config.rootSpikeMax - config.rootSpikeMin))
								+ config.rootSpikeMin;

						int spikeNoise = (int) (noise.noise(3, noiseX, noiseZ) * (config.extSpikeMax - config.extSpikeMin))
								+ config.extSpikeMin;

						int hillSize = (int) (noise.noise(6, noiseX, noiseZ) * config.hillMax.length);
						int hillHeight = (int) (hillNoise.noise(
								biomeIntensity[x][z], noiseX, noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
								+ config.hillMin[hillSize];

						int stone = 2 + spikeHeight + spikeNoise;

						iTop += hillHeight;
						stone += hillHeight;

						int yI = iTop + dirt;
						y = Math.max(
								y,
								yI
										+ (stone / 4)
										+ (int) ((noise
												.noise(1, noiseX, noiseZ) * .75 + 0.25)
												* Math.sqrt(islandYScale / 0.01D) * stone));
						// TODO This causes issues. Noisy terrain, etc.

						setBlock(
								chunkX,
								chunkZ,
								result,
								x,
								yI,
								z,
								config.topCoating[yI % config.topCoating.length]);
						yI--;
						for (; yI >= iTop && yI >= 0; yI--) {
							setBlock(chunkX, chunkZ, result, x, yI, z,
									config.coating[yI % config.coating.length]);
						}
						for (; yI >= iTop - lower && yI >= 0; yI--) {
							setBlock(chunkX, chunkZ, result, x, yI, z,
									config.lowerCoating[yI
											% config.lowerCoating.length]);
						}
						for (; yI >= iTop - stone && yI >= 0; yI--) {
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
		}
		return result;
	}

	public FloatingIslandPlugin getPlugin() {
		return plugin;
	}

	@Override
	public Location getFixedSpawnLocation(World w, Random rand) {
		int chunkX = (rand.nextInt(100) - 50) << 4;
		int chunkZ = (rand.nextInt(100) - 50) << 4;

		int baseX = rand.nextInt(16);

		int offX = 1;
		while (baseX - offX >= 0 || baseX + offX < 16) {
			int baseZ = rand.nextInt(16);
			if (baseX - offX >= 0) {
				int offZ = 1;
				while (baseZ - offZ >= 0 || baseZ + offZ < 16) {
					if (baseZ - offZ >= 0) {
						int y = w.getHighestBlockYAt(chunkX + baseX - offX,
								chunkZ + baseZ - offZ);
						if (y > 0) {
							return new Location(w, chunkX + baseX - offX, y,
									chunkZ + baseZ - offZ);
						}
					}
					if (baseZ + offZ < 16) {
						int y = w.getHighestBlockYAt(chunkX + baseX - offX,
								chunkZ + baseZ + offZ);
						if (y > 0) {
							return new Location(w, chunkX + baseX - offX, y,
									chunkZ + baseZ + offZ);
						}
					}
					offZ++;
				}
			}
			if (baseX + offX < 16) {
				int offZ = 1;
				while (baseZ - offZ >= 0 || baseZ + offZ < 16) {
					if (baseZ - offZ >= 0) {
						int y = w.getHighestBlockYAt(chunkX + baseX + offX,
								chunkZ + baseZ - offZ);
						if (y > 0) {
							return new Location(w, chunkX + baseX + offX, y,
									chunkZ + baseZ - offZ);
						}
					}
					if (baseZ + offZ < 16) {
						int y = w.getHighestBlockYAt(chunkX + baseX + offX,
								chunkZ + baseZ + offZ);
						if (y > 0) {
							return new Location(w, chunkX + baseX + offX, y,
									chunkZ + baseZ + offZ);
						}
					}
					offZ++;
				}
			}
			offX++;
		}
		return getFixedSpawnLocation(w, rand);
	}
}
