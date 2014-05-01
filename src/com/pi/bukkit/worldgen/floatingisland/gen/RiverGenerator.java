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
	enum WaterfallType {
		NONE, NOISY, CLEAN
	}

	@Override
	public void populate(World world, Random random, Chunk source) {
		LayeredOctaveNoise noise = new LayeredOctaveNoise(
				new SimplexNoiseGenerator(new Random(world.getSeed())), 3);
		noise.setScale(0, 0.0001D);

		double waterChance = 1.0;
		int gorgeSize = 3;
		float gorgeIncline = 1;
		WaterfallType waterfalls = WaterfallType.CLEAN;
		int gorgeDepth = 1;

		List<Block> clear = new ArrayList<Block>();
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
					if (bestY > 2 && random.nextDouble() <= waterChance) {
						Block tB = source.getBlock(x, bestY - gorgeDepth, z);
						if (!tB.getRelative(1, 0, 0).isEmpty()
								&& !tB.getRelative(-1, 0, 0).isEmpty()
								&& !tB.getRelative(0, 0, -1).isEmpty()
								&& !tB.getRelative(0, 0, 1).isEmpty()) {
							clear.add(tB.getRelative(0, 1, 0));
							{
								float offset = 0;
								for (int i = 0; i <= gorgeSize; i++) {
									if (i != 0) {
										offset += gorgeIncline;
										clear.add(tB.getRelative(i,
												(int) offset, 0));
										clear.add(tB.getRelative(-i,
												(int) offset, 0));
										clear.add(tB.getRelative(0,
												(int) offset, i));
										clear.add(tB.getRelative(0,
												(int) offset, -i));
									}
								}
							}
							tB.setType(bb == Biome.FROZEN_RIVER ? Material.ICE
									: Material.WATER);
							for (int off = 1; off < 5; off++) {
								Biome b1 = tB.getRelative(off, 0, 0).getBiome();
								Biome b2 = tB.getRelative(-off, 0, 0)
										.getBiome();
								Biome b3 = tB.getRelative(0, 0, off).getBiome();
								Biome b4 = tB.getRelative(0, 0, -off)
										.getBiome();
								if ((b1 == Biome.RIVER || b1 == Biome.FROZEN_RIVER)
										&& (b2 == Biome.RIVER || b2 == Biome.FROZEN_RIVER)
										&& (b3 == Biome.RIVER || b3 == Biome.FROZEN_RIVER)
										&& (b4 == Biome.RIVER || b4 == Biome.FROZEN_RIVER)) {
									tB.getRelative(0, (int) -Math.sqrt(off), 0)
											.setType(Material.WATER);
								} else {
									break;
								}
							}
						}
					}
				}
			}
		}

		for (Block b : clear) {
			if (b.getChunk() == source && b.getType() != Material.WATER) {
				int limit = 5;
				for (int yO = 0; yO < 5; yO++) {
					Block set = b.getRelative(0, yO, 0);
					if (set != null && set.getType() != Material.AIR) {
						boolean waterfallFlag = false;
						switch (waterfalls) {
						case NONE:
							waterfallFlag = set.getRelative(-1, 0, 0).getType() == Material.WATER
									|| set.getRelative(1, 0, 0).getType() == Material.WATER
									|| set.getRelative(0, 0, -1).getType() == Material.WATER
									|| set.getRelative(0, 0, 1).getType() == Material.WATER;
							break;
						case NOISY:
							waterfallFlag = false;
							break;
						case CLEAN:
							waterfallFlag = set.getRelative(-1, 0, 0).getType() == Material.WATER
									|| set.getRelative(1, 0, 0).getType() == Material.WATER
									|| set.getRelative(0, 0, -1).getType() == Material.WATER
									|| set.getRelative(0, 0, 1).getType() == Material.WATER;
							waterfallFlag = waterfallFlag
									&& set.getRelative(0, -1, 0).getType() != Material.WATER;
							break;
						}
						if (set.getType() == Material.WATER || waterfallFlag) {
							limit = 0;
							break;
						}
					} else {
						limit = yO;
						break;
					}
				}
				for (int yO = 0; yO < limit; yO++) {
					Block set = b.getRelative(0, yO, 0);
					set.setType(Material.AIR);
				}
			}
		}
	}
}
