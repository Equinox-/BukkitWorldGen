package com.pi.bukkit.worldgen.floatingisland.gen.base;

import org.bukkit.World;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

public abstract class Baseline {
	protected final int chunkX, chunkZ;
	protected final World world;
	protected final BiomeIntensityGrid biomes;
	protected final int heightMapOversample;
	protected int[][][] heightMap;

	public Baseline(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, int heightMapOversample) {
		this.world = w;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.biomes = backing;
		this.heightMapOversample = heightMapOversample;
	}

	public final int[] getHeights(int x, int z) {
		return heightMap[x + heightMapOversample][z + heightMapOversample];
	}

	protected final void setHeights(int x, int z, int... heights) {
		heightMap[x + heightMapOversample][z + heightMapOversample] = heights;
	}

	protected final void allocHeightMap() {
		heightMap = new int[16 + (2 * heightMapOversample)][16 + (2 * heightMapOversample)][];
	}

	public int getHeightNear(int x, int y, int z) {
		int[] res = getHeights(x, z);
		int near = -1;
		int cDist = 999999;
		for (int j : res) {
			int dist = Math.abs(j - y);
			if (dist < cDist) {
				cDist = dist;
				near = j;
			}
		}
		return near;
	}
}
