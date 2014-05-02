package com.pi.bukkit.worldgen.floatingisland;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.pi.bukkit.MesaDataValues;

public class IslandConfig {
	private final static IslandConfig defaultIslandConfig = new IslandConfig();
	private static Map<Biome, IslandConfig> biomeMapping = new HashMap<Biome, IslandConfig>();
	/**
	 * Beware. This is expensive.
	 * 
	 * @see IslandConfig#SMOOTH_TO_BIOME_EDGE_MAX_DIST
	 */
	public static int SMOOTH_TO_BIOME_EDGE = 0xDEAD;
	public static int SMOOTH_TO_BIOME_EDGE_MAX_DIST = 100;

	static {
		IslandConfig hills = new IslandConfig();
		hills.hillMin = new int[] { 0, 2, 5 };
		hills.hillMax = new int[] { 2, 10, 15 };
		hills.islandScale = new Vector(0.01D, 0.0075D, 0.01D);

		IslandConfig extremeHills = new IslandConfig();
		extremeHills.hillMin = new int[] { 0, 2, 5, 10 };
		extremeHills.hillMax = new int[] { 2, 10, 20, 20 };
		extremeHills.islandScale = new Vector(0.02D, 0.005D, 0.02D);
		biomeMapping.put(Biome.EXTREME_HILLS, extremeHills);

		IslandConfig plains = new IslandConfig();
		plains.hillMin = new int[] { 0, 2 };
		plains.hillMax = new int[] { 2, 3 };
		plains.hillNoise = 0.00075D;
		plains.islandScale = new Vector(0.01D, 0.03D, 0.01D);
		biomeMapping.put(Biome.PLAINS, plains);

		IslandConfig icePlains = plains.clone();
		icePlains.minSnowMin = new int[] { 20, 70 };
		icePlains.minSnowMax = new int[] { 40, 90 };
		biomeMapping.put(Biome.ICE_PLAINS, icePlains);

		IslandConfig iceMountains = extremeHills.clone();
		iceMountains.minSnowMin = new int[] { 20, 70 };
		iceMountains.minSnowMax = new int[] { 40, 90 };
		biomeMapping.put(Biome.ICE_MOUNTAINS, iceMountains);

		IslandConfig forestHills = hills.clone();
		biomeMapping.put(Biome.FOREST_HILLS, forestHills);
		biomeMapping.put(Biome.BIRCH_FOREST_HILLS, forestHills);

		IslandConfig ocean = plains.clone();
		biomeMapping.put(Biome.OCEAN, ocean);
		ocean.bigLakesSizeBase = 10;
		ocean.bigLakesSizeRand = 6;
		ocean.bigLakes = true;

		IslandConfig iceocean = ocean.clone();
		iceocean.minSnowMin = new int[] { 20, 70 };
		iceocean.minSnowMax = new int[] { 40, 90 };
		biomeMapping.put(Biome.FROZEN_OCEAN, iceocean);

		IslandConfig desert = plains.clone();
		desert.coating = desert.topCoating = new Material[] { Material.SAND };
		desert.lowerCoating = new Material[] { Material.SANDSTONE };
		biomeMapping.put(Biome.DESERT, desert);

		IslandConfig desertHills = hills.clone();
		desertHills.coating = desertHills.topCoating = new Material[] { Material.SAND };
		desertHills.lowerCoating = new Material[] { Material.SANDSTONE };
		biomeMapping.put(Biome.DESERT_HILLS, desertHills);

		IslandConfig swampLands = new IslandConfig();
		swampLands.bigLakes = true;
		swampLands.bigLakesDepth = 2;
		biomeMapping.put(Biome.SWAMPLAND, swampLands);

		IslandConfig river = new IslandConfig();
		// river.topCoating = new Object[] { Material.WATER };
		river.smoothSize = SMOOTH_TO_BIOME_EDGE;
		river.grassNoise = 0;
		river.topCoating = new Object[] { Material.DIRT };
		biomeMapping.put(Biome.RIVER, river);

		IslandConfig snowyRiver = river.clone();
		snowyRiver.minSnowMin = new int[] { 20, 70 };
		snowyRiver.minSnowMax = new int[] { 40, 90 };
		biomeMapping.put(Biome.FROZEN_RIVER, snowyRiver);

		IslandConfig mesa = desert.clone();
		mesa.topCoating = new Object[] { new ItemStack(Material.SAND, 1,
				MesaDataValues.RED_SAND) };// Red sand
		mesa.foundation = new Object[] {
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.ORANGE_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.WHITE_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.LIGHT_GRAY_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.GRAY_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BLACK_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.GRAY_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1, MesaDataValues.RED_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.BROWN_CLAY),
				new ItemStack(Material.STAINED_CLAY, 1,
						MesaDataValues.YELLOW_CLAY) };
		mesa.coating = mesa.lowerCoating = mesa.foundation;
		biomeMapping.put(Biome.MESA, mesa);
		biomeMapping.put(Biome.MESA_BRYCE, mesa);
		biomeMapping.put(Biome.MESA_PLATEAU, mesa);
		biomeMapping.put(Biome.MESA_PLATEAU_FOREST, mesa);
		biomeMapping.put(Biome.MESA_PLATEAU_FOREST_MOUNTAINS, mesa);
		biomeMapping.put(Biome.MESA_PLATEAU_MOUNTAINS, mesa);

		IslandConfig beach = new IslandConfig();
		beach.coating = beach.topCoating = new Object[] { Material.SAND };
		beach.lowerCoating = new Object[] { Material.SANDSTONE };
	}

	public static IslandConfig forBiome(Biome biome) {
		IslandConfig mapping = biomeMapping.get(biome);
		return mapping != null ? mapping : defaultIslandConfig;
	}

	public int riverGorgeDepth = 3;

	public int dirtMax = 7;
	public int dirtMin = 4;
	public int[] hillMin = { 0, 2 };
	public int[] hillMax = { 2, 5 };

	public int extSpikeMin = -4;
	public int extSpikeMax = 4;
	public int rootSpikeMin = 4;
	public int rootSpikeMax = 16;
	public int minStoneMinThickness = 5;
	public int maxStoneMinThickness = 7;

	// Island Size Settings
	public int smoothSize = 0;
	/**
	 * x and z control size. HIGHER IS SMALLER y controls how much the height
	 * changes. LOWER IS STEEPER
	 */
	public Vector islandScale = new Vector(0.01D, 0.01D, 0.01D);

	public double hillNoise = 0.02D;

	public int[] minSnowMin = { 128 };
	public int[] minSnowMax = { 128 };

	public Object[] coating = { Material.DIRT };
	public Object[] topCoating = { Material.GRASS };
	public Object[] lowerCoating = { Material.DIRT };
	public Object[] foundation = { Material.STONE };

	public boolean bigLakes = false;
	public int bigLakesSizeBase = 7;
	public int bigLakesSizeRand = 5;
	public int bigLakesDepth = 4;
	public int bigLakeHeightClamp = 1;

	public double grassNoise = 0.75;

	@Override
	public IslandConfig clone() {
		IslandConfig clone = new IslandConfig();
		for (Field f : IslandConfig.class.getFields()) {
			try {
				f.set(clone, f.get(this));
			} catch (Exception e) {
			}
		}
		return clone;
	}

	public static short blockSpecToData(Object o) {
		if (o instanceof ItemStack) {
			return (short) (((ItemStack) o).getDurability());
		}
		return 0;
	}

	public static short blockSpecToID(Object o) {
		if (o instanceof Material) {
			return (byte) ((Material) o).getId();
		} else if (o instanceof ItemStack) {
			return (short) (((ItemStack) o).getType().getId());
		}
		return 0;
	}
}
