package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.ServerCrossover;
import com.pi.bukkit.worldgen.floatingisland.LayeredOctaveNoise;

public class RiverGenerator extends BlockPopulator {
	@Override
	public void populate(World world, Random random, Chunk source) {
		LayeredOctaveNoise noise = new LayeredOctaveNoise(
				new SimplexNoiseGenerator(new Random(world.getSeed())), 3);
		noise.setScale(0, 0.0001D);

		List<Block> riverBlocks = new ArrayList<Block>();
		List<Block> riverBorders = new ArrayList<Block>();
		List<Block> riverCenters = new ArrayList<Block>();
		Block riverMax = null;
		Block riverMin = null;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int realX = (source.getX() << 4) + x;
				int realZ = (source.getZ() << 4) + z;
				Biome bb = source.getBlock(x, 0, z).getBiome();
				if (bb == Biome.RIVER || bb == Biome.FROZEN_RIVER) {
					int bestY = -1;
					int wantedY = (int) (noise.noise(0, realX, realZ) * 128);
					for (int y = 120; y >= 0; y--) {
						Block block = source.getBlock(x, y, z);
						if (ServerCrossover.isTop(block)) {
							if (Math.abs(bestY - wantedY) > Math.abs(y
									- wantedY)) {
								bestY = y;
							}
						}
					}
					if (bestY > 2) {
						Block tB = source.getBlock(x, bestY, z);
						riverBlocks.add(tB);
						if (world.getBiome(realX, realZ - 1) != bb
								|| world.getBiome(realX, realZ + 1) != bb
								|| world.getBiome(realX - 1, realZ) != bb
								|| world.getBiome(realX + 1, realZ) != bb) {
							riverBorders.add(tB);
							if (riverMin == null || tB.getY() < riverMin.getY()) {
								riverMin = tB;
							}
							if (riverMax == null || tB.getY() > riverMax.getY()) {
								riverMax = tB;
							}
						} else {
							riverCenters.add(tB);
						}
					}
				}
			}
		}

		if (riverMin != null && riverMax != null) {
			for (Block b : riverBlocks) {
				double minL = b.getLocation().distance(riverMin.getLocation());
				double maxL = b.getLocation().distance(riverMax.getLocation());
				double total = minL + maxL;
				if (total > 0) {
					minL = total - minL;
					maxL = total - maxL;
					minL /= total;
					maxL /= total;

					double y = minL * riverMin.getY() + maxL * riverMax.getY();
					if (y < b.getY()) {
						for (int j = 0; b.getY() - j > y; j++) {
							b.getRelative(0, -j, 0).setType(Material.AIR);
						}
					}
				}
			}
		}
	}
}
