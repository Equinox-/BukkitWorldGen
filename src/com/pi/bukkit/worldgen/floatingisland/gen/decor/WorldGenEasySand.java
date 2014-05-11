package com.pi.bukkit.worldgen.floatingisland.gen.decor;

import java.util.Random;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;

import net.minecraft.server.Block;
import net.minecraft.server.BlockGravel;
import net.minecraft.server.BlockSand;
import net.minecraft.server.Blocks;
import net.minecraft.server.Material;
import net.minecraft.server.World;
import net.minecraft.server.WorldGenerator;

public class WorldGenEasySand extends WorldGenerator {
	private Block genBlock;
	private int maxSize;

	public WorldGenEasySand(Block paramBlock, int paramInt) {
		this.genBlock = paramBlock;
		this.maxSize = paramInt;
	}

	@Override
	public boolean a(World world, Random random, int x, int y, int z) {
		if (world.getType(x, y, z).getMaterial() != Material.WATER && y > 0
				&& world.getType(x, y - 1, z) != Blocks.AIR)
			return false;
		BlockSand.instaFall = false;
		BlockGravel.instaFall = false;
		int size = random.nextInt(this.maxSize - 2) + 2;
		int j = 2;
		for (int xP = x - size; xP <= x + size; xP++) {
			for (int zP = z - size; zP <= z + size; zP++) {
				int xDis = xP - x;
				int zDis = zP - z;
				if (xDis * xDis + zDis * zDis <= size * size) {
					for (int yP = y - j; yP <= y + j; yP++) {
						if (yP > 0) {
							Block localBlock = world.getType(xP, yP, zP);
							Block belowBlock = world.getType(xP, yP - 1, zP);
							if (((localBlock == Blocks.DIRT) || (localBlock == Blocks.GRASS))
									&& belowBlock.getMaterial() != Material.AIR) {
								world.setTypeAndData(xP, yP, zP, this.genBlock,
										0, 2);
							}
						}
					}
				}
			}
		}
		BlockSand.instaFall = true;
		BlockGravel.instaFall = true;
		return true;
	}

}