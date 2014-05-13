package com.pi.bukkit.worldgen.tropicfjord;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.worldgen.LayeredOctaveNoise;
import com.pi.bukkit.worldgen.decor.BiomeDecoratorPopulator;

public class TropicFjordGenerator extends ChunkGenerator {

	@Override
	public List<BlockPopulator> getDefaultPopulators(World w) {
		return Arrays.asList((BlockPopulator) new BiomeDecoratorPopulator());
	}

	final void setBlock(byte[][] result, int x, int y, int z, int blkid) {
		if (y < 0 || (y >> 4) >= result.length) {
			return;
		}
		if (result[y >> 4] == null) {
			result[y >> 4] = new byte[4096];
		}
		result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = (byte) blkid;
	}

	@Override
	public byte[][] generateBlockSections(World w, Random random, int chunkX,
			int chunkZ, BiomeGrid biomeOriginal) {
		byte[][] result = new byte[24][];

		final int baseY = 160;

		int realX = chunkX << 4;
		int realZ = chunkZ << 4;

		NoiseGenerator noiseRoot = new SimplexNoiseGenerator(new Random(
				w.getSeed()));
		LayeredOctaveNoise noise = new LayeredOctaveNoise(noiseRoot, 11);
		noise.setScale(0, new Vector(0.015D, 0.01D, 0.015D)); // Threshold
		noise.setScale(1, 0.001D); // Height
		noise.setScale(2, 0.1D); // Dirt
		noise.setScale(3, 0.001D); // water
		noise.setScale(4, 0.01D); // tunnel size
		noise.setScale(5, 0.0001D); // tunnel root
		noise.setScale(6, 0.01D); // hills
		noise.setScale(7, 0.05D); // lightstone

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				double noiseX = realX + x;
				double noiseZ = realZ + z;
				int startY = (int) ((noise.noise(1, noiseX, noiseZ) * 64) + baseY);
				int waterLevel = 32;// (int) ((noise.noise(3, noiseX, noiseZ) *
									// 32) + (startY - 32));
				startY += (int) (noise.noise(6, noiseX, noiseZ) * 16); // Aka
																		// hills
				double tunnelBound = (noise.noise(4, noiseX, noiseZ) * 0.125)
						+ 0.125 + (noise.noise(5, noiseX, noiseZ) * 0.125);
				int dirtLeft = -1;

				for (int y = startY; y >= 0; y--) {
					double thresh = noise.noise(0, noiseX, y, noiseZ);
					thresh = Math.pow(thresh, 2);
					if (thresh > .5 - tunnelBound && thresh < .5 + tunnelBound) {
						setBlock(result, x, y, z,
								y <= waterLevel ? Material.WATER.getId() : 0);
						dirtLeft = -1;
						continue;
					}
					if (dirtLeft < 0) {
						setBlock(result, x, y, z,
								y <= waterLevel ? Material.DIRT.getId()
										: Material.GRASS.getId());
						dirtLeft = (int) (3 + noise.noise(2, noiseX, y, noiseZ));
					} else if (dirtLeft > 0) {
						setBlock(result, x, y, z, Material.DIRT.getId());
						dirtLeft--;
					} else {
						double glowNoise = noise.noise(7, noiseX, y, noiseZ);
						setBlock(
								result,
								x,
								y,
								z,
								glowNoise > 0.45 && glowNoise < 0.55 ? Material.GLOWSTONE
										.getId() : Material.STONE.getId());
					}
				}
			}
		}
		return result;
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
