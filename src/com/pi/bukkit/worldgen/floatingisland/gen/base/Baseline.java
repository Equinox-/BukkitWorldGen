package com.pi.bukkit.worldgen.floatingisland.gen.base;

import org.bukkit.World;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;

public abstract class Baseline {
	protected final int chunkX, chunkZ;
	protected final World world;
	protected final BiomeIntensityGrid biomes;
	protected final int heightMapOversample;
	protected int[][][] heightMap;

	protected int[][][] metaData;

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

	public final int[] getMeta(int x, int z) {
		return metaData[x + heightMapOversample][z + heightMapOversample];
	}

	protected final void setHeights(int x, int z, int... heights) {
		heightMap[x + heightMapOversample][z + heightMapOversample] = heights;
	}

	protected final void setMetadata(int x, int z, int... meta) {
		metaData[x + heightMapOversample][z + heightMapOversample] = meta;
	}

	protected final void allocHeightMap() {
		heightMap = new int[16 + (2 * heightMapOversample)][16 + (2 * heightMapOversample)][];
		metaData = new int[16 + (2 * heightMapOversample)][16 + (2 * heightMapOversample)][];
	}

	public int getHeightIndexNear(int x, int y, int z) {
		int[] res = getHeights(x, z);
		int near = -1;
		int cDist = 999999;
		for (int i = 0; i < res.length; i++) {
			int dist = Math.abs(res[i] - y);
			if (dist < cDist) {
				cDist = dist;
				near = i;
			}
		}
		return near;
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

	public int getHeightBelow(int x, int y, int z) {
		int[] res = getHeights(x, z);
		for (int q = res.length - 1; q >= 0; q--) {
			if (res[q] <= y) {
				return res[q];
			}
		}
		return -1;
	}

	public boolean isInBounds(int x, int z) {
		return x >= -heightMapOversample && z >= -heightMapOversample
				&& x < 16 + heightMapOversample && z < 16 + heightMapOversample;
	}
}
