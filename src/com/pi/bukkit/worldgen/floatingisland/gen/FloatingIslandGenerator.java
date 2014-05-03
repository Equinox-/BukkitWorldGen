package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.worldgen.BiomeNoiseGenerator;
import com.pi.bukkit.worldgen.DefferedDataPopulator;
import com.pi.bukkit.worldgen.DefferedDataPopulator.SetBlockData;
import com.pi.bukkit.worldgen.LayeredOctaveNoise;
import com.pi.bukkit.worldgen.floatingisland.FloatingIslandPlugin;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;

public class FloatingIslandGenerator extends ChunkGenerator {

	private FloatingIslandPlugin plugin;

	private DefferedDataPopulator dPop = new DefferedDataPopulator();

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList((BlockPopulator) dPop/*
												 * , new LakesGenerator(), new
												 * RiverGenerator(), new
												 * SnowPopulator(), new
												 * BiomeDecoratorPopulator()
												 */);
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

	private static int BIOME_BLEND_RADIUS = 16;
	private static int BIOME_BLEND_EXTENT = 5;
	private static int NEIGHBOR_DISTANCE = 1;

	private static float[][][] generateBiomeLevels(World w, int chunkX,
			int chunkZ, BiomeGrid biomes) {
		float[][][] biomeLevels = new float[16 + (BIOME_BLEND_EXTENT * 2)][16 + (BIOME_BLEND_EXTENT * 2)][Biome
				.values().length];
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
				for (int xO = -BIOME_BLEND_RADIUS; xO <= BIOME_BLEND_RADIUS; xO++) {
					for (int zO = -BIOME_BLEND_RADIUS; zO <= BIOME_BLEND_RADIUS; zO++) {
						int tX = x + xO;
						int tZ = z + zO;
						if (tX >= -BIOME_BLEND_EXTENT
								&& tZ >= -BIOME_BLEND_EXTENT
								&& tX < 16 + BIOME_BLEND_EXTENT
								&& tZ < 16 + BIOME_BLEND_EXTENT) {
							biomeLevels[tX + BIOME_BLEND_EXTENT][tZ
									+ BIOME_BLEND_EXTENT][here.ordinal()] += 1.0 / (1.0 + (xO
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
					float thresh = 0f;
					thresh += (float) Math.pow(
							Math.abs(y - 64
									- (noise.noise(0, noiseX, noiseZ) - .5)
									* 32) / 64.0, 3) * .25f;

					final double maskHere = islandMap.noise(biomeIntensity[x
							+ BIOME_BLEND_EXTENT][z + BIOME_BLEND_EXTENT],
							noiseX, y, noiseZ);
					final double maskBelow = islandMap.noise(biomeIntensity[x
							+ BIOME_BLEND_EXTENT][z + BIOME_BLEND_EXTENT],
							noiseX, y - 1, noiseZ);
					final double maskAbove = islandMap.noise(biomeIntensity[x
							+ BIOME_BLEND_EXTENT][z + BIOME_BLEND_EXTENT],
							noiseX, y + 1, noiseZ);
					final double maskDiff = ((Math.abs(maskHere - maskBelow) + Math
							.abs(maskHere - maskAbove))) / 2.0;

					if (maskHere > thresh && maskBelow < maskHere
							&& maskAbove < maskHere && maskDiff > 1E-5) {
						int islandTop = y;

						// Neighbors calc TODO Expensive. Cheaper plz
						int unknownNeighbors = 0;
						int[][] neighbors = new int[3][3];
						neighbors[1][1] = y;
						for (int nX = -1; nX <= 1; nX++) {
							next: for (int nZ = -1; nZ <= 1; nZ += (nX == 0 ? 2
									: 1)) {
								neighbors[nX + 1][nZ + 1] = -1;
								for (int nY = 0; nY < 15; nY++) {
									for (int dY = -1; dY <= 1; dY += 2) {
										double mHere = islandMap
												.noise(biomeIntensity[x
														+ (nX * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT][z
														+ (nZ * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT],
														noiseX
																+ (nX * NEIGHBOR_DISTANCE),
														y + (nY * dY),
														noiseZ
																+ (nZ * NEIGHBOR_DISTANCE));
										double mBelow = islandMap
												.noise(biomeIntensity[x
														+ (nX * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT][z
														+ (nZ * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT],
														noiseX
																+ (nX * NEIGHBOR_DISTANCE),
														y + (nY * dY) - 1,
														noiseZ
																+ (nZ * NEIGHBOR_DISTANCE));
										double mAbove = islandMap
												.noise(biomeIntensity[x
														+ (nX * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT][z
														+ (nZ * NEIGHBOR_DISTANCE)
														+ BIOME_BLEND_EXTENT],
														noiseX
																+ (nX * NEIGHBOR_DISTANCE),
														y + (nY * dY) + 1,
														noiseZ
																+ (nZ * NEIGHBOR_DISTANCE));
										double mDiff = ((Math.abs(mHere
												- mBelow) + Math.abs(mHere
												- mAbove))) / 2.0;
										if (mHere > thresh && mBelow < mHere
												&& mAbove < mHere
												&& mDiff > 1E-5) {
											neighbors[nX + 1][nZ + 1] = y
													+ (nY * dY);
											islandTop += (y + (nY * dY)) * 0.5;
											continue next;
										}
									}
								}
								unknownNeighbors++;
							}
						}
						if (unknownNeighbors >= 2) { // This stops the borders from spiking
							continue;
						}

						islandTop /= (1 + (8 - unknownNeighbors));

						int dirt = (int) Math.round(noise.noise(2, noiseX,
								noiseZ) * config.grassNoise) + 3;

						int lower = (int) (noise.noise(5, noiseX, islandTop,
								noiseZ) * 2) + 1;

						int spikeHeight = (int) (noise.noise(4, noiseX,
								islandTop, noiseZ) * (config.rootSpikeMax - config.rootSpikeMin))
								+ config.rootSpikeMin;

						int spikeNoise = (int) (Math.pow(
								noise.noise(3, noiseX, islandTop, noiseZ), 2) * (config.extSpikeMax - config.extSpikeMin))
								+ config.extSpikeMin;

						int hillSize = (int) (noise.noise(6, noiseX, islandTop,
								noiseZ) * config.hillMax.length);
						int hillHeight = (int) (hillNoise.noise(
								biomeIntensity[x + BIOME_BLEND_EXTENT][z
										+ BIOME_BLEND_EXTENT], noiseX,
								islandTop, noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
								+ config.hillMin[hillSize];

						int stone = 2 + spikeHeight + spikeNoise;

						islandTop += hillHeight;
						stone += hillHeight;

						int yI = islandTop + dirt;
						y = Math.max(
								y,
								yI
										+ ((2 + config.rootSpikeMax + config.extSpikeMax) / 4)
										+ (int) ((noise.noise(1, noiseX,
												islandTop, noiseZ) * .6 + 0.4)
												* Math.sqrt(islandYScale / 0.0075D) * (2 + config.rootSpikeMax))); //
						// TODO This causes issues. Noisy terrain, etc.

						if (biome == Biome.RIVER) {
							// Check biome neighbors
							int count = 0;
							Biome replace = null;
							for (int xO = -1; xO <= 1; xO++) {
								for (int zO = xO == 0 ? -1 : 0; zO <= 1; zO += 2) {
									int resX = x + xO, resZ = z + zO;
									if (resX >= 0 && resZ >= 0 && resX < 16
											&& resZ < 16) {
										Biome test = biomes
												.getBiome(resX, resZ);
										if (test == biome) {
											count++;
										} else {
											replace = test;
										}
									}
								}
							}

							if ((count <= 1 || unknownNeighbors > 1)
									&& replace != null) {
								biomes.setBiome(x, z, replace);
								biome = replace;
								config = IslandConfig.forBiome(replace);
							}
						}
						if (biome == Biome.RIVER) {
							yI = (int) (5 * Math.floor(yI / 5.0)); // Ugly as fuck
							setBlock(chunkX, chunkZ, result, x, yI, z,
									Material.GLOWSTONE);
						} else {
							setBlock(chunkX, chunkZ, result, x, yI, z,
									config.topCoating[yI
											% config.topCoating.length]);
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
