package com.pi.bukkit;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class ServerCrossover {
	public static boolean isLiquid(Material m) {
		return m == Material.WATER || m == Material.LAVA;
	}

	public static boolean isBuildable(Material m) {
		return !(m == Material.SNOW || m == Material.FIRE
				|| m == Material.WATER || m == Material.LAVA || m == Material.PORTAL);
	}

	/*
	 * Not Buildable plant replaceable plant orientable snowlayer fire water
	 * lava portal
	 */
	/*
	 * IsLiquid Water Lava
	 */

	public static boolean isTop(Block block) {
		return block.getRelative(0, 1, 0).getType() == Material.AIR
				&& block.getRelative(0, 3, 0).getType() == Material.AIR
				&& (block.getType().isSolid() && !block.getType().isBurnable() && block
						.getType().isOccluding());
	}

	public static boolean isTop(int id) {
		@SuppressWarnings("deprecation")
		Material type = Material.getMaterial(id);
		return (type.isSolid() && !type.isBurnable() && type.isOccluding());
	}
}
