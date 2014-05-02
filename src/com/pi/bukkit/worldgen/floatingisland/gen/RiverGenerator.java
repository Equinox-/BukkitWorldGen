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
import org.bukkit.util.Vector;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import com.pi.bukkit.ServerCrossover;
import com.pi.bukkit.worldgen.floatingisland.LayeredOctaveNoise;

public class RiverGenerator extends BlockPopulator {
	private static class RiverClump {
		private int minY = 128;
		private int maxY = 0;

		List<Block> riverBlocks = new ArrayList<Block>();
		List<Block> riverBorders = new ArrayList<Block>();
		List<Block> riverCenters = new ArrayList<Block>();
		Vector[] averages = { new Vector(), new Vector(), new Vector(),
				new Vector() };
		int[] counts = new int[4];
	}

	public RiverClump getClumpFor(List<RiverClump> clumps, int y) {
		for (RiverClump c : clumps) {
			if (c.maxY + 10 > y && c.minY - 10 < y) {
				c.maxY = Math.max(c.maxY, y);
				c.minY = Math.min(c.minY, y);
				return c;
			}
		}
		RiverClump create = new RiverClump();
		create.minY = y;
		create.maxY = y;
		clumps.add(create);
		return create;
	}

	@Override
	public void populate(World world, Random random, Chunk source) {
		LayeredOctaveNoise noise = new LayeredOctaveNoise(
				new SimplexNoiseGenerator(new Random(world.getSeed())), 3);
		noise.setScale(0, 0.0001D);
		List<RiverClump> clumps = new ArrayList<RiverClump>();

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
						RiverClump river = getClumpFor(clumps, bestY);
						Block tB = source.getBlock(x, bestY, z);
						river.riverBlocks.add(tB);
						if (world.getBiome(realX, realZ - 1) != bb
								|| world.getBiome(realX, realZ + 1) != bb
								|| world.getBiome(realX - 1, realZ) != bb
								|| world.getBiome(realX + 1, realZ) != bb) {
							river.riverBorders.add(tB);
							if (x == 0) {
								river.averages[0].add(tB.getLocation()
										.toVector());
								river.counts[0]++;
							}
							if (z == 0) {
								river.averages[1].add(tB.getLocation()
										.toVector());
								river.counts[1]++;
							}
							if (z == 15) {
								river.averages[2].add(tB.getLocation()
										.toVector());
								river.counts[2]++;
							}
							if (x == 15) {
								river.averages[3].add(tB.getLocation()
										.toVector());
								river.counts[3]++;
							}
						} else {
							river.riverCenters.add(tB);
						}
					}
				}
			}
		}
		for (RiverClump river : clumps) {
			Block riverMin = null;
			Block riverMax = null;
			for (int i = 0; i < 4; i++) {
				if (river.counts[i] > 0) {
					river.averages[i].multiply(1f / river.counts[i]);
					Block c = world.getBlockAt(river.averages[i].toLocation(world));
					if (riverMin == null || riverMin.getY() > c.getY()) {
						riverMin = c;
					}
					if (riverMax == null || riverMax.getY() < c.getY()) {
						riverMax = c;
					}
				}
			}

			if (riverMin != null && riverMax != null) {
				if (Math.abs(riverMin.getY() - riverMax.getY()) < 20) {

				}
				for (Block b : river.riverBlocks) {
					double minL = b.getLocation().distance(
							riverMin.getLocation());
					double maxL = b.getLocation().distance(
							riverMax.getLocation());
					double total = minL + maxL;
					if (total > 0) {
						minL = total - minL;
						maxL = total - maxL;
						minL /= total;
						maxL /= total;

						double y = minL * riverMin.getY() + maxL
								* riverMax.getY();
						if (y < b.getY()) {
							for (int j = 0; b.getY() - j >= y; j++) {
								b.getRelative(0, -j, 0).setType(Material.AIR);
							}
						}
						world.getBlockAt(b.getX(), (int) Math.min(b.getY(), y),
								b.getZ()).setType(Material.WATER);
					}
				}
			}
		}
	}
}
