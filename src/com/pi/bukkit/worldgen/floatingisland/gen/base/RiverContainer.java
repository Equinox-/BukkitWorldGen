package com.pi.bukkit.worldgen.floatingisland.gen.base;

import org.bukkit.World;
import org.bukkit.block.Biome;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

/**
 * A second pass at keeping rivers contained within their banks
 * @author westin
 *
 */
public class RiverContainer extends BaselineTransform {

	private BiomeIntensityGrid riverGrid;

	public RiverContainer(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent) {
		super(w, chunkX, chunkZ, backing, parent, 0);
		riverGrid = backing;// backing.clone();
		regenerateLayer();
	}

	private boolean isRiverShore(int x, int z) {
		if (RiverSmoother.isRiver(riverGrid.getBiome(x, z))) {
			return false;
		}
		for (int xO = -1; xO <= 1; xO++) {
			for (int zO = -1; zO <= 1; zO++) {
				Biome here = riverGrid.getBiome(x + xO, z + zO);
				if (RiverSmoother.isRiver(here)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void regenerateLayer() {
		allocHeightMap();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int[] heights = parent.getHeights(x, z);
				int[] newHeights = new int[heights.length];
				int[] meta = new int[heights.length];

				System.arraycopy(heights, 0, newHeights, 0, heights.length);
				setHeights(x, z, newHeights);
				setMetadata(x, z, meta);
				if (isRiverShore(x, z)) {
					for (int i = 0; i < heights.length; i++) {
						int y = heights[i];
						// Process on x-z.
						int maxRiverHeight = y;
						for (int xR = 0; xR < 16; xR++) {
							if (RiverSmoother
									.isRiver(riverGrid.getBiome(xR, z))) {
								int tH = parent.getHeightNear(xR, y, z);
								if (tH - y < GenerationTuning.NEIGHBOR_TOLERANCE) {
									maxRiverHeight = Math.max(maxRiverHeight,
											tH);
								}
							}
						}
						for (int zR = 0; zR < 16; zR++) {
							if (RiverSmoother
									.isRiver(riverGrid.getBiome(x, zR))) {
								int tH = parent.getHeightNear(x, y, zR);
								if (tH - y < GenerationTuning.NEIGHBOR_TOLERANCE) {
									maxRiverHeight = Math.max(maxRiverHeight,
											tH);
								}
							}
						}
						newHeights[i] = maxRiverHeight;
					}
				}
			}
		}
	}
}
