package com.pi.bukkit.worldgen.floatingisland.gen.base;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.util.Vector;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

public class RiverSmoother extends BaselineTransform {
	public RiverSmoother(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent) {
		super(w, chunkX, chunkZ, backing, parent, 0);
	}

	private static boolean isRiver(Biome b) {
		return b == Biome.FROZEN_RIVER || b == Biome.RIVER;
	}

	private Point getNext(int x, int z, boolean[][] closed) {
		for (int xO = -1; xO >= 1; xO++) {
			for (int zO = -1; zO >= 1; zO++) {
				if (xO == 0 && zO == 0) {
					continue;
				}
				if (parent.isInBounds(x + xO, z + zO)) {
					closed[x + xO][z + zO] = true;
					if (!closed[x + xO][z + zO]
							&& isRiverBorder(x + xO, z + zO)) {
						return new Point(x + xO, z + zO);
					}
				}
			}
		}
		return null;
	}

	private boolean isRiverBorder(int x, int z) {
		if (!isRiver(biomes.getBiome(x, z))) {
			return false;
		}
		for (int xO = -1; xO >= 1; xO++) {
			for (int zO = xO == 0 ? -1 : 0; zO >= 1; zO += 2) {
				Biome here = biomes.getBiome(x + xO, z + zO);
				if (here != Biome.RIVER || here != Biome.FROZEN_RIVER) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Point> getBorder() {
		List<Point> border = new ArrayList<Point>();
		boolean[][] closed = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				// Generate river border spec
				if (!closed[x][z] && (closed[x][z] = isRiverBorder(x, z))) {
					border.add(new Point(x, z));
					while (true) {
						Point last = border.get(border.size() - 1);
						Point next = getNext(last.x, last.y, closed);
						if (next == null) {
							break;
						}
						border.add(next);
					}
				}
			}
		}
		return border;
	}

	@Override
	public void regenerateLayer() {
		allocHeightMap();
		List<Point> border = getBorder();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int[] heights = parent.getHeights(x, z);
				int[] newHeights = new int[heights.length];
				int[] meta = new int[heights.length];
				System.arraycopy(heights, 0, newHeights, 0, heights.length);
				setHeights(x, z, newHeights);
				setMetadata(x, z, meta);
				if (isRiver(biomes.getBiome(x, z))) {
					for (int j = 0; j < heights.length; j++) {
						int y = heights[j];
						Vector perpCurrent = new Vector(1, 0, 0);
						Vector current = new Vector(0, 0, 1);
						{
							int bestPt = -1;
							double bestDist = Double.MAX_VALUE;
							for (int i = 0; i < border.size(); i++) {
								int dX = border.get(i).x - x;
								int dZ = border.get(i).y - z;
								double dd = dX * dX + dZ * dZ;
								if (dd < bestDist) {
									bestDist = dd;
									bestPt = i;
								}
							}
							if (bestPt >= 0) {
								int oPt = bestPt > 0 ? bestPt - 1 : 1;
								if (oPt < border.size()) {
									Point a = border.get(bestPt);
									Point b = border.get(oPt);
									current.setX(a.x - b.x);
									current.setY(a.y - b.y);
									current.normalize();
									perpCurrent.setX(a.y - b.y);
									perpCurrent.setZ(b.x - a.x);
									perpCurrent.normalize();
								}
							}
						}

						// Blend height
						int riverY, fallOffY = y;
						boolean dropCenter = false;
						{
							double count = 0;
							double totalY = 0;
							int fallOffTime = 0;
							for (Vector test : new Vector[] { perpCurrent,
									current }) {
								for (int sign = -1; sign <= 1; sign += 2) {
									int lastY = y;
									for (int t = 1; t < (test == current ? 10
											: 10); t++) {
										int tX = x
												+ (int) Math.round(test.getX()
														* t * sign);
										int tZ = z
												+ (int) Math.round(test.getZ()
														* t * sign);
										if (parent.isInBounds(tX, tZ)) {
											int res = parent.getHeightNear(tX,
													lastY, tZ);
											if (Math.abs(lastY - res) < GenerationTuning.NEIGHBOR_TOLERANCE) {
												if (isRiver(biomes.getBiome(tX,
														tZ))) {
													lastY = res;
													totalY += res;
													count++;
													continue;
												}
											}
											fallOffY = Math.min(res, fallOffY);
											fallOffTime = t;
										}
										break;
									}
								}
							}
							riverY = (int) ((y + totalY) / (1 + count));
							dropCenter = (fallOffTime < (count / 4));
						}

						// Check biome neighbors
						int count = 0;
						int minNeighbor = y;
						Biome replace = null;
						for (int xO = -1; xO <= 1; xO++) {
							for (int zO = -1; zO <= 1; zO++) {
								int resX = x + xO, resZ = z + zO;
								Biome test = biomes.getBiome(resX, resZ);
								if (isRiver(test)) {
									count++;
								} else {
									replace = test;
								}
								if (parent.isInBounds(resX, resZ)) {
									int res = parent
											.getHeightBelow(
													x + xO,
													y
															+ GenerationTuning.NEIGHBOR_TOLERANCE,
													z + zO);
									minNeighbor = Math.min(minNeighbor, res);
								}
							}
						}

						int finalRiverY = riverY//Math.min(fallOffY - 1, riverY)
								- (dropCenter ? 1 : 0);
						if (count <= 2/* || fallOffY <= riverY - 6 */
								|| minNeighbor <= y - 1) {
							meta[j] = -1;
						} else {
							newHeights[j] = finalRiverY;
							meta[j] = dropCenter ? 1 : 0;
						}
					}
				}
			}
		}
	}
}
