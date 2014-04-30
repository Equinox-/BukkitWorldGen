package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import com.pi.bukkit.worldgen.floatingisland.IslandConfig;

public class SnowPopulator extends BlockPopulator {

	@Override
	public void populate(World w, Random r, Chunk c) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				IslandConfig cfg = IslandConfig.forBiome(w.getBiome(
						x + (c.getX() << 4), z + (c.getZ() << 4)));
				int maxY = w.getHighestBlockYAt(x + (c.getX() << 4),
						z + (c.getZ() << 4)) - 1;
				if (maxY > 0) {
					boolean okToGen = false;
					int thick;
					for (thick = cfg.minSnowMin.length - 1; thick >= 0; thick--) {
						if (maxY >= cfg.minSnowMin[thick]) {
							if (maxY < cfg.minSnowMax[thick]
									&& cfg.minSnowMax[thick] != cfg.minSnowMin[thick]) {
								if (r.nextDouble() > ((double) (maxY - cfg.minSnowMin[thick]))
										/ ((double) (cfg.minSnowMax[thick] - cfg.minSnowMin[thick])))
									continue;
							}
							okToGen = true;
							break;
						}
					}
					if (okToGen) {
						Block below = c.getBlock(x, maxY, z);
						if (below.getType() == Material.GRASS
								|| below.getType() == Material.STONE
								|| below.getType() == Material.DIRT) {
							for (int y = maxY + 1; y < maxY + 1 + thick; y++) {
								c.getBlock(x, y, z)
										.setType(Material.SNOW_BLOCK);
							}
							c.getBlock(x, maxY + 1 + thick, z).setType(
									Material.SNOW);
						} else if (below.getType() == Material.WATER) {
							while (thick >= 0) {
								if (r.nextDouble() > 0.3D) {
									thick--;
								} else {
									break;
								}
							}
							if (thick >= 0) {
								below.setType(Material.ICE);
								if (thick > 0) {
									c.getBlock(x, maxY + 1, z).setType(
											Material.SNOW);
								}
								if (thick > 1) {
									for (int y = 1; y < thick; y++) {
										if (maxY - y < 0)
											break;
										Block mod = c.getBlock(x, maxY - y, z);
										if (mod.getType() == Material.WATER)
											mod.setType(Material.ICE);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
