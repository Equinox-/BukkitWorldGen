package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Random;

import net.minecraft.server.EnumSkyBlock;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;

import com.pi.bukkit.worldgen.floatingisland.IslandConfig;

public class LakesGenerator extends BlockPopulator {
	private Material liquid = Material.WATER;
	private int sizeBase = 7, sizeRand = 5, depth = 4;

	public boolean genLake(World paramWorld, Random paramRandom, int paramInt1,
			int paramInt2, int paramInt3) {
		paramInt1 -= 8;
		paramInt3 -= 8;
		while ((paramInt2 > depth + 1)
				&& (!paramWorld.getBlockAt(paramInt1, paramInt2, paramInt3)
						.getType().isOccluding()))
			paramInt2--;
		if (paramInt2 <= depth) {
			return false;
		}

		paramInt2 -= depth;

		boolean[] arrayOfBoolean = new boolean[2048];

		int i = paramRandom.nextInt(sizeRand) + sizeBase;
		for (int xOff = 0; xOff < i; xOff++) {
			double d1 = paramRandom.nextDouble() * 6.0D + 3.0D;
			double d2 = paramRandom.nextDouble() * 4.0D + 2.0D;
			double d3 = paramRandom.nextDouble() * 6.0D + 3.0D;

			double d4 = paramRandom.nextDouble() * (16.0D - d1 - 2.0D) + 1.0D
					+ d1 / 2.0D;
			double d5 = paramRandom.nextDouble() * (8.0D - d2 - 4.0D) + 2.0D
					+ d2 / 2.0D;
			double d6 = paramRandom.nextDouble() * (16.0D - d3 - 2.0D) + 1.0D
					+ d3 / 2.0D;

			for (int k = 1; k < 15; k++)
				for (int m = 1; m < 15; m++)
					for (int n = 1; n < 7; n++) {
						double d7 = (k - d4) / (d1 / 2.0D);
						double d8 = (n - d5) / (d2 / 2.0D);
						double d9 = (m - d6) / (d3 / 2.0D);
						double d10 = d7 * d7 + d8 * d8 + d9 * d9;
						if (d10 >= 1.0D)
							continue;
						arrayOfBoolean[((k * 16 + m) * 8 + n)] = true;
					}
		}
		int zOff;
		int yOff;
		int xOff;
		for (xOff = 0; xOff < 16; xOff++) {
			for (zOff = 0; zOff < 16; zOff++) {
				for (yOff = 0; yOff < 8; yOff++) {
					int i3 = (arrayOfBoolean[((xOff * 16 + zOff) * 8 + yOff)] == false)
							&& (((xOff < 15) && (arrayOfBoolean[(((xOff + 1) * 16 + zOff) * 8 + yOff)]))
									|| ((xOff > 0) && (arrayOfBoolean[(((xOff - 1) * 16 + zOff) * 8 + yOff)]))
									|| ((zOff < 15) && (arrayOfBoolean[((xOff * 16 + (zOff + 1)) * 8 + yOff)]))
									|| ((zOff > 0) && (arrayOfBoolean[((xOff * 16 + (zOff - 1)) * 8 + yOff)]))
									|| ((yOff < 7) && (arrayOfBoolean[((xOff * 16 + zOff) * 8 + (yOff + 1))])) || ((yOff > 0) && (arrayOfBoolean[((xOff * 16 + zOff) * 8 + (yOff - 1))]))) ? 1
							: 0;

					if (i3 != 0) {
						Material localMaterial = paramWorld.getBlockAt(
								paramInt1 + xOff, paramInt2 + yOff,
								paramInt3 + zOff).getType();
						if ((yOff >= depth)
								&& (localMaterial == Material.LAVA || localMaterial == Material.STATIONARY_LAVA))
							// Used to check if water too
							return false;

						if ((yOff < depth)
								&& (!localMaterial.isOccluding())
								&& (paramWorld.getBlockTypeIdAt(paramInt1
										+ xOff, paramInt2 + yOff, paramInt3
										+ zOff) != liquid.getId()))
							return false;
					}
				}
			}

		}

		for (xOff = 0; xOff < 16; xOff++) {
			for (zOff = 0; zOff < 16; zOff++) {
				for (yOff = 0; yOff < 8; yOff++) {
					if (arrayOfBoolean[((xOff * 16 + zOff) * 8 + yOff)]) {
						paramWorld.getBlockAt(paramInt1 + xOff,
								paramInt2 + yOff, paramInt3 + zOff).setType(
								yOff >= depth ? Material.AIR : liquid);
					}
				}
			}
		}

		for (xOff = 0; xOff < 16; xOff++) {
			for (zOff = 0; zOff < 16; zOff++) {
				for (yOff = 4; yOff < 8; yOff++) {
					if ((arrayOfBoolean[((xOff * 16 + zOff) * 8 + yOff)] == false)
							|| (paramWorld.getBlockTypeIdAt(paramInt1 + xOff,
									paramInt2 + yOff - 1, paramInt3 + zOff) != Material.DIRT
									.getId())
							|| (paramWorld.getBlockTypeIdAt(paramInt1 + xOff,
									paramInt2 + yOff, paramInt3 + zOff) <= 0))
						continue;
					Biome localBiomeBase = paramWorld.getBiome(
							paramInt1 + xOff, paramInt3 + zOff);
					paramWorld
							.getBlockAt(paramInt1 + xOff, paramInt2 + yOff - 1,
									paramInt3 + zOff)
							.setTypeId(
									localBiomeBase == Biome.MUSHROOM_ISLAND
											|| localBiomeBase == Biome.MUSHROOM_SHORE ? Material.MYCEL
											.getId() : Material.GRASS.getId());
				}
			}

		}

		if (liquid == Material.LAVA) {
			for (xOff = 0; xOff < 16; xOff++) {
				for (zOff = 0; zOff < 16; zOff++) {
					for (yOff = 0; yOff < 8; yOff++) {
						int i4 = (arrayOfBoolean[((xOff * 16 + zOff) * 8 + yOff)] == false)
								&& (((xOff < 15) && (arrayOfBoolean[(((xOff + 1) * 16 + zOff) * 8 + yOff)]))
										|| ((xOff > 0) && (arrayOfBoolean[(((xOff - 1) * 16 + zOff) * 8 + yOff)]))
										|| ((zOff < 15) && (arrayOfBoolean[((xOff * 16 + (zOff + 1)) * 8 + yOff)]))
										|| ((zOff > 0) && (arrayOfBoolean[((xOff * 16 + (zOff - 1)) * 8 + yOff)]))
										|| ((yOff < 7) && (arrayOfBoolean[((xOff * 16 + zOff) * 8 + (yOff + 1))])) || ((yOff > 0) && (arrayOfBoolean[((xOff * 16 + zOff) * 8 + (yOff - 1))]))) ? 1
								: 0;

						if ((i4 == 0)
								|| ((yOff >= depth) && (paramRandom.nextInt(2) == 0))
								|| (!paramWorld
										.getBlockAt(paramInt1 + xOff,
												paramInt2 + yOff,
												paramInt3 + zOff).getType()
										.isSolid()))
							continue;
						paramWorld.getBlockAt(paramInt1 + xOff,
								paramInt2 + yOff, paramInt3 + zOff).setType(
								Material.STONE);
					}
				}

			}

		}

		if (liquid == Material.WATER) {
			for (xOff = 0; xOff < 16; xOff++) {
				for (zOff = 0; zOff < 16; zOff++) {
					yOff = depth;
					/*
					 * if (!paramWorld.s(paramInt1 + xOff, paramInt2 + yOff,
					 * paramInt3 + zOff)) continue;
					 */
					Biome b = paramWorld.getBlockAt(paramInt1 + xOff,
							paramInt2 + yOff, paramInt3 + zOff).getBiome();
					if (b != null
							&& (b.name().toLowerCase().contains("ice") || b
									.name().toLowerCase().contains("snow"))) {
						paramWorld.getBlockAt(paramInt1 + xOff,
								paramInt2 + yOff, paramInt3 + zOff).setType(
								Material.ICE);
					}
				}
			}
		}

		return true;
	}

	@Override
	public void populate(World world, Random random, Chunk chunk) {
		IslandConfig cfg = IslandConfig.forBiome(world.getBiome(
				chunk.getX() << 4, chunk.getZ() << 4));
		if (cfg.bigLakes) {
			sizeBase = cfg.bigLakesSizeBase;
			sizeRand = cfg.bigLakesSizeRand;
			depth = 4;// cfg.bigLakesDepth;
			for (int i = 0; i < 25; i++) {
				int x = (chunk.getX() << 4) + random.nextInt(16);
				int z = (chunk.getZ() << 4) + random.nextInt(16);
				int y = world.getHighestBlockYAt(x, z);
				if (y > 0) {
					if (genLake(world, random, x, y / cfg.bigLakeHeightClamp, z))
						return;
				}
			}
		}
	}
}
