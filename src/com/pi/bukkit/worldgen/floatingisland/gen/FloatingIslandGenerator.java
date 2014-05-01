package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.DefferedDataPopulator;
import com.pi.bukkit.worldgen.floatingisland.FloatingIslandPlugin;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;
import com.pi.bukkit.worldgen.floatingisland.LayeredOctaveNoise;

public class FloatingIslandGenerator extends ChunkGenerator {

	private FloatingIslandPlugin plugin;

	private DefferedDataPopulator dPop = new DefferedDataPopulator();

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList(dPop, new LakesGenerator(), new RiverGenerator(),
				new SnowPopulator(), new BiomeDecoratorPopulator());
	}

	public FloatingIslandGenerator(FloatingIslandPlugin plugin) {
		this.plugin = plugin;
	}

	void setBlockInternal(short[][] result, int x, int y, int z, short blkid) {
		if (y < 0 || (y >> 4) >= result.length) {
			return;
		}
		if (result[y >> 4] == null) {
			result[y >> 4] = new short[4096];
		}
		result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	}

	short getBlockInternal(short[][] result, int x, int y, int z) {
		if (result[y >> 4] == null) {
			return (short) 0;
		}
		return result[y >> 4][((y & 0xF) << 8) | (z << 4) | x];
	}

	void setBlock(int chunkX, int chunkZ, short[][] result, int x, int y,
			int z, Object block) {
		setBlockInternal(result, x, y, z, IslandConfig.blockSpecToID(block));
		short data = IslandConfig.blockSpecToData(block);
		if (data != 0) {
			dPop.addBlock(chunkX, chunkZ, x, y, z, data);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public short[][] generateExtBlockSections(World w, Random random,
			int chunkX, int chunkZ, BiomeGrid biomes) {
		short[][] result = new short[16][];

		int realX = chunkX << 4;
		int realZ = chunkZ << 4;

		LayeredOctaveNoise noise = new LayeredOctaveNoise(
				new SimplexNoiseGenerator(new Random(w.getSeed())), 11);
		noise.setScale(0, 0.001D); // Threshold
		noise.setScale(1, 0.01D); // Island map
		noise.setBlend(1, 1);
		noise.setScale(2, 0.1D); // Dirt
		noise.setScale(3, 2D); // ext spike
		noise.setScale(4, 0.1D); // root spike
		noise.setScale(5, 0.1D); // lower coating
		noise.setScale(6, 0.0001D); // hill size

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int noiseX = realX + x;
				int noiseZ = realZ + z;
				Biome biome = biomes.getBiome(x, z);
				IslandConfig config = IslandConfig.forBiome(biome);

				noise.setScale(7, config.hillNoise); // hill

				for (int y = 0; y < 128; y++) {
					float thresh = .5f;
					thresh += (float) Math.pow(
							Math.abs(y - 64
									- (noise.noise(0, noiseX, noiseZ) - .5)
									* 32) / 64.0, 3) * .25f;
					double maskHere = noise.noise(1, noiseX, y, noiseZ);
					if (maskHere > thresh
							&& noise.noise(1, noiseX, y - 1, noiseZ) < maskHere
							&& noise.noise(1, noiseX, y + 1, noiseZ) < maskHere) {
						int iTop = y;

						// Terrain smoothing:
						if (config.smoothSize > 0) {
							int[] smoother = new int[] { iTop, iTop, iTop, iTop };
							int[] smoothDistance = new int[] {
									config.smoothSize, config.smoothSize,
									config.smoothSize, config.smoothSize };
							if (config.smoothSize == IslandConfig.SMOOTH_TO_BIOME_EDGE) {
								for (int r = 0; r < 4; r++) {
									for (int off = 1; off < IslandConfig.SMOOTH_TO_BIOME_EDGE_MAX_DIST; off++) {
										int baseX = noiseX
												+ (((r >> 1) & 1)
														* (((r & 1) << 1) - 1) * smoothDistance[r]);
										int baseZ = noiseZ
												+ (((~r >> 1) & 1)
														* (((r & 1) << 1) - 1) * smoothDistance[r]);
										if (w.getBiome(baseX, baseZ) != biome) {
											smoothDistance[r] = off;
											break;
										}
									}
								}
							}
							for (int yO = 0; yO < 25; yO++) {
								for (int r = 0; r < 4; r++) {
									if (smoother[r] == iTop) {
										for (int q = -1; q <= 1; q += 2) {
											int baseX = noiseX
													+ (((r >> 1) & 1)
															* (((r & 1) << 1) - 1) * smoothDistance[r]);
											int baseZ = noiseZ
													+ (((~r >> 1) & 1)
															* (((r & 1) << 1) - 1) * smoothDistance[r]);
											int baseY = iTop + (yO * q);
											double mHere = noise.noise(1,
													baseX, baseY, baseZ);
											double mAbove = noise.noise(1,
													baseX, baseY + 1, baseZ);
											double mBelow = noise.noise(1,
													baseX, baseY - 1, baseZ);
											if (mHere > thresh
													&& mHere > mAbove
													&& mHere > mBelow) {
												smoother[r] = baseY;
											}
										}
									}
								}
							}
							for (int i : smoother) {
								iTop += i;
							}
							iTop /= 5;
						}

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
						int hillHeight = (int) (noise.noise(7, noiseX, noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
								+ config.hillMin[hillSize];

						int stone = 2 + spikeHeight + spikeNoise;

						iTop += hillHeight;
						stone += hillHeight;

						int yI = iTop + dirt;
						if (yI > 128) {
							yI = 128;
						}
						y = Math.max(y, yI);

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
							setBlock(chunkX, chunkZ, result, x, yI, z,
									config.foundation[yI
											% config.foundation.length]);
						}
					}
				}
			}
		}
		genStructures(w, chunkX << 4, chunkZ << 4, result);
		return result;
	}

	public void genStructures(World world, int cX, int cZ, short[][] data) {
		net.minecraft.server.World handle = ((CraftWorld) world).getHandle();
		// WorldGenBase t = new CavesGenerator();
		// WorldGenStronghold u = new WorldGenStronghold();
		// WorldGenVillage v = new WorldGenVillage(0);
		// WorldGenMineshaft w = new WorldGenMineshaft();
		// WorldGenBase x = new WorldGenCanyon();

		// t.a(null, handle, cX, cZ, data);
		// x.a(null, handle, cX, cZ, data);
		if (world.canGenerateStructures()) {
			// w.a(null, handle, cX, cZ, data);
			// v.a(null, handle, cX, cZ, data);
			// u.a(null, handle, cX, cZ, data);
		}
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
