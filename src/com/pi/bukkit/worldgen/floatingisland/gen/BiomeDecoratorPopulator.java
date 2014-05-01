package com.pi.bukkit.worldgen.floatingisland.gen;

import java.util.Random;

import net.minecraft.server.BiomeBase;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.generator.BlockPopulator;

public class BiomeDecoratorPopulator extends BlockPopulator {
	@Override
	public void populate(World world, Random random, Chunk chunk) {
		if (world != null && chunk != null) {
			BiomeBase base = CraftBlock.biomeToBiomeBase(world.getBiome(
					chunk.getX() << 4, chunk.getZ() << 4));
			if (base != null) {
				new BiomeDecoratorPort().a(((CraftWorld) world).getHandle(),
						random, base, chunk.getX() << 4, chunk.getZ() << 4);
			}
		}
	}
}
