package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Random;

import net.minecraft.server.BiomeBase;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;

import com.pi.bukkit.worldgen.floatingisland.IslandConfig;

public class BiomeDecoratorPopulator extends BlockPopulator {

	@Override
	public void populate(World world, Random random, Chunk chunk) {
		if (world != null && chunk != null) {
			BiomeBase base = IslandConfig.biomeBaseMapping.get(world.getBiome(
					chunk.getX() << 4, chunk.getZ() << 4));
			if (base != null) {
				base.a(((CraftWorld) world).getHandle(), random,
						chunk.getX() << 4, chunk.getZ() << 4);
			}
		}
	}
}
