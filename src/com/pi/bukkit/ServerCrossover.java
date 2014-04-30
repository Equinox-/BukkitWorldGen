package com.pi.bukkit;

import org.bukkit.Material;

public class ServerCrossover {
	public static boolean isLiquid(Material m) {
		return m == Material.WATER || m == Material.LAVA;
	}
	

	public static boolean isBuildable(Material m){
		return !(m == Material.SNOW || m==Material.FIRE || m==Material.WATER || m==Material.LAVA || m==Material.PORTAL);
	}
	/*Not Buildable
	 * plant
	 * replaceable plant
	 * orientable
	 * snowlayer
	 * fire
	 * water
	 * lava
	 * portal
	 */
	/*
	 * IsLiquid
	 * Water
	 * Lava
	 * 
	 */
}
