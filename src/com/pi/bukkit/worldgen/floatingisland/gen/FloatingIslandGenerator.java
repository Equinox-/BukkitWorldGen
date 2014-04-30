package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.worldgen.floatingisland.FloatingIslandPlugin;
import com.pi.bukkit.worldgen.floatingisland.IslandConfig;
import com.pi.bukkit.worldgen.floatingisland.LayeredOctaveNoise;

public class FloatingIslandGenerator extends ChunkGenerator {

	private FloatingIslandPlugin plugin;

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList(new RiverGenerator(), new LakesGenerator(),
				new SnowPopulator(), new BiomeDecoratorPopulator());
	}

	public FloatingIslandGenerator(FloatingIslandPlugin plugin) {
		this.plugin = plugin;
	}

	private int getBlockIndex(int x, int y, int z) {
		if (y > 127)
			y = 127;
		return (((x << 4) + z) << 7) + y;
	}

	@SuppressWarnings("deprecation")
	@Override
	public byte[] generate(World w, Random rand, int chunkX, int chunkZ) {
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

		int beginY = -1;
		int endY = -1;

		byte[] data = new byte[32768];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int noiseX = realX + x;
				int noiseZ = realZ + z;
				((CraftWorld) w).getBiome(0, 0);
				IslandConfig config = IslandConfig.forBiome(w.getBiome(noiseX,
						noiseZ));
				noise.setScale(7, config.hillNoise); // hill

				for (int y = 0; y < 128; y++) {
					float thresh = .5f;
					thresh += (float) Math.pow(
							Math.abs(y - 64
									- (noise.noise(0, noiseX, noiseZ) - .5)
									* 32) / 64.0, 3) * .25f;
					boolean island = noise.noise(1, noiseX, y, noiseZ) > thresh;
					if (beginY == -1 && island) {
						beginY = y;
					}
					if ((!island || y == 128) && beginY != -1) {
						endY = y;
						int iTop = (beginY + endY) / 2;
						int divergence = (int) Math.sqrt(endY - beginY);
						int dirt = (int) (noise.noise(2, noiseX, noiseZ) * 1.0) + 3;
						int lower = (int) (noise.noise(5, noiseX, y, noiseZ) * 2) + 1;

						int spikeHeight = (int) (noise.noise(4, noiseX, noiseZ) * (config.rootSpikeMax - config.rootSpikeMin))
								+ config.rootSpikeMin;

						int spikeNoise = (int) (noise.noise(3, noiseX, noiseZ) * (config.extSpikeMax - config.extSpikeMin))
								+ config.extSpikeMin;

						int hillSize = (int) (noise.noise(6, noiseX, noiseZ) * config.hillMax.length);
						int hillHeight = (int) (noise.noise(7, noiseX, noiseZ) * (config.hillMax[hillSize] - config.hillMin[hillSize]))
								+ config.hillMin[hillSize];

						int stone = 2 + spikeHeight + spikeNoise;

						iTop += divergence;
						stone += divergence * 2;

						iTop += hillHeight;
						stone += hillHeight;

						int yI = iTop + dirt;
						if (yI > 128) {
							yI = 128;
						}

						data[getBlockIndex(x, yI--, z)] = (byte) config.topCoating
								.getId();
						for (; yI >= iTop && yI >= 0; yI--) {
							data[getBlockIndex(x, yI, z)] = (byte) config.coating
									.getId();
						}
						for (; yI >= iTop - lower && yI >= 0; yI--) {
							data[getBlockIndex(x, yI, z)] = (byte) config.lowerCoating
									.getId();
						}
						for (; yI >= iTop - stone && yI >= 0; yI--) {
							data[getBlockIndex(x, yI, z)] = (byte) config.foundation
									.getId();
						}
						beginY = -1;
						endY = -1;
					}
				}
				beginY = -1;
				endY = -1;
			}
		}
		genStructures(w, chunkX << 4, chunkZ << 4, data);
		return data;
	}

	public void genStructures(World world, int cX, int cZ, byte[] data) {
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
