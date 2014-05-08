package com.pi.bukkit.worldgen.floatingisland.gen.base;

import org.bukkit.World;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

public class EdgeCleaner extends BaselineTransform {
	public EdgeCleaner(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent) {
		super(w, chunkX, chunkZ, backing, parent,
				GenerationTuning.EDGE_OVERSAMPLE);
		regenerateLayer();
	}

	@Override
	public void regenerateLayer() {
		allocHeightMap();

		// Threshold edges to max 2 unknown
		for (int x = -heightMapOversample; x < 16 + heightMapOversample; x++) {
			for (int z = -heightMapOversample; z < 16 + heightMapOversample; z++) {
				int[] originalHeights = parent.getHeights(x, z);
				int[] resY = new int[originalHeights.length];
				int resH = 0;
				for (int y : originalHeights) {
					int smoothY = y;
					int neighbors = 0;
					for (int nX = -1; nX <= 1; nX++) {
						for (int nZ = -1; nZ <= 1; nZ++) {
							int heightNear = parent.getHeightNear(x + nX, y, z
									+ nZ);
							if (Math.abs(heightNear - y) < GenerationTuning.NEIGHBOR_TOLERANCE) {
								neighbors++;
								smoothY += heightNear;
							}
						}
					}
					smoothY /= (neighbors + 1);
					if (neighbors > 3) {
						resY[resH++] = smoothY;
					}
				}

				int[] cpy = new int[resH];
				System.arraycopy(resY, 0, cpy, 0, resH);
				setHeights(x, z, cpy);
			}
		}
	}
}
