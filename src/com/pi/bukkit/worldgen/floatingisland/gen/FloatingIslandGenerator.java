package com.pi.bukkit.worldgen.floatingisland.gen;

import java.awt.Point;
import java.util.ArrayList;
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
import com.pi.bukkit.worldgen.floatingisland.gen.base.BaselineLayer;
import com.pi.bukkit.worldgen.floatingisland.gen.base.EdgeCleaner;

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

	public static boolean isRiverBorder(World w, BiomeGrid biomes, int realX,
			int realZ, int x, int z) {
		for (int xO = -1; xO >= 1; xO++) {
			for (int zO = xO == 0 ? -1 : 0; zO >= 1; zO += 2) {
				Biome here = null;
				if (x >= 0 && z >= 0 && x < 16 && z < 16) {
					here = biomes.getBiome(x, z);
				} else {
					here = w.getBiome(realX + x, realZ + z);
				}
				if (here != Biome.RIVER || here != Biome.FROZEN_RIVER) {
					return true;
				}
			}
		}
		return false;
	}

	public static Point getNext(World w, BiomeGrid biomes, int realX,
			int realZ, int x, int z, boolean[][] closed) {
		for (int xO = -1; xO >= 1; xO++) {
			for (int zO = -1; zO >= 1; zO++) {
				if (xO == 0 && zO == 0) {
					continue;
				}
				if ((x + xO) >= 0 && (z + zO) >= 0 && (x + xO) < 16
						&& (z + zO) < 16) {
					closed[x + xO][z + zO] = true;
					if (!closed[x + xO][z + zO]
							&& isRiverBorder(w, biomes, realX, realZ, x + xO, z
									+ zO)) {
						return new Point(x + xO, z + zO);
					}
				}
			}
		}
		return null;
	}

	public static int getNeighbor(BiomeNoiseGenerator islandMap,
			float[] biomeIntensity, int x, int y, int z, int divergence,
			float thresh) {
		for (int nY = 0; nY < divergence; nY++) {
			for (int dY = -1; dY <= 1; dY += 2) {
				double mHere = islandMap.noise(biomeIntensity, x,
						y + (nY * dY), z);
				double mBelow = islandMap.noise(biomeIntensity, x, y
						+ (nY * dY) - 1, z);
				double mAbove = islandMap.noise(biomeIntensity, x, y
						+ (nY * dY) + 1, z);
				double mDiff = ((Math.abs(mHere - mBelow) + Math.abs(mHere
						- mAbove))) / 2.0;
				if (mHere > thresh && mBelow < mHere && mAbove < mHere
						&& mDiff > 1E-5) {
					return y + (nY * dY);
				}
			}
		}
		return -1;
	}

	@Override
	public byte[][] generateBlockSections(World w, Random random, int chunkX,
			int chunkZ, BiomeGrid biomes) {
		byte[][] result = new byte[24][];

		BiomeIntensityGrid biomeValues = new BiomeIntensityGrid(w, biomes,
				chunkX, chunkZ);
		Baseline baseline = new BaselineLayer(w, chunkX, chunkZ, biomeValues);
		baseline = new EdgeCleaner(w, chunkX, chunkZ, biomeValues, baseline);
		
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

		List<Point> border = new ArrayList<Point>();
		boolean[][] closed = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				// Generate river border spec
				if (!closed[x][z]
						&& (closed[x][z] = isRiverBorder(w, biomes, realX,
								realZ, x, z))) {
					border.add(new Point(x, z));
					while (true) {
						Point last = border.get(border.size() - 1);
						Point next = getNext(w, biomes, realX, realZ, last.x,
								last.y, closed);
						if (next == null) {
							break;
						}
						border.add(next);
					}
				}
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
				for (int y : yS) {
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
							biomeValues.getBiomeIntensity(x, z), noiseX,
							islandTop, noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
							+ config.hillMin[hillSize];

					int stone = 2 + spikeHeight + spikeNoise;

					islandTop += hillHeight;
					stone += hillHeight;

					int yI = islandTop + dirt;
					// TODO This causes issues. Noisy terrain, etc.

					// if (biome == Biome.RIVER) {
					// // Grab border size things
					// Vector perpCurrent = new Vector(1, 0, 0);
					// {
					// int bestPt = -1;
					// double bestDist = Double.MAX_VALUE;
					// for (int i = 0; i < border.size(); i++) {
					// int dX = border.get(i).x - x;
					// int dZ = border.get(i).y - z;
					// double dd = dX * dX + dZ * dZ;
					// if (dd < bestDist) {
					// bestDist = dd;
					// bestPt = i;
					// }
					// }
					// if (bestPt >= 0) {
					// int oPt = bestPt > 0 ? bestPt - 1 : 1;
					// if (oPt < border.size()) {
					// Point a = border.get(bestPt);
					// Point b = border.get(oPt);
					// perpCurrent.setX(a.y - b.y);
					// perpCurrent.setZ(b.x - a.x);
					// perpCurrent.normalize();
					// }
					// }
					// }
					//
					// // Blend height
					// int count = 0;
					// int totalY = 0;
					// for (int sign = -1; sign <= 1; sign += 2) {
					// int lastY = yI;
					// for (int t = 1; t < 10; t++) {
					// int tX = x
					// + (int) Math.round(perpCurrent.getX()
					// * t * sign);
					// int tZ = z
					// + (int) Math.round(perpCurrent.getZ()
					// * t * sign);
					// if (tX >= 0 && tZ >= 0 && tX < 16 && tZ < 16
					// && biomes.getBiome(tX, tZ) == biome) {
					// int res = baseline.getHeightNear(tX, lastY,
					// tZ);
					// if (Math.abs(lastY - res) <
					// GenerationTuning.NEIGHBOR_TOLERANCE) {
					// lastY = res;
					// totalY += res;
					// count++;
					// continue;
					// }
					// }
					// break;
					// }
					// }
					// yI = (int) ((yI + totalY) / (1 + count));
					//
					// // Check biome neighbors
					// count = 0;
					// Biome replace = null;
					// for (int xO = -1; xO <= 1; xO++) {
					// for (int zO = xO == 0 ? -1 : 0; zO <= 1; zO += 2) {
					// int resX = x + xO, resZ = z + zO;
					// if (resX >= 0 && resZ >= 0 && resX < 16
					// && resZ < 16) {
					// Biome test = biomes.getBiome(resX, resZ);
					// if (test == biome) {
					// count++;
					// } else {
					// replace = test;
					// }
					// }
					// }
					// }
					//
					// if ((count <= 1) && replace != null) {
					// biomes.setBiome(x, z, replace);
					// biome = replace;
					// config = IslandConfig.forBiome(replace);
					// }
					// }

					if (biome == Biome.RIVER) {
						// yI = (int) (5 * Math.floor(yI / 5.0)); // Ugly as
						// // fuck
						setBlock(chunkX, chunkZ, result, x, yI, z,
								Material.GLOWSTONE);
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
