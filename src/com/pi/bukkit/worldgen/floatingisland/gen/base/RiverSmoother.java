package com.pi.bukkit.worldgen.floatingisland.gen.base;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.Vector;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

public class RiverSmoother extends BaselineTransform {
	public static int RIVER_MASK = 0x10;
	public static int RIVER_SHORE_MASK = 0x20 | RIVER_MASK;

	private BiomeIntensityGrid riverGrid;

	public RiverSmoother(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent) {
		super(w, chunkX, chunkZ, backing, parent, 0);
		riverGrid = backing.clone();
		regenerateLayer();
	}

	public static boolean isRiver(Biome b) {
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

	private List<Point> getBorder(List<Point> border) {
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
		List<Point> border = new ArrayList<Point>();
		getBorder(border);

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int[] heights = parent.getHeights(x, z);
				int[] newHeights = new int[heights.length];
				int[] meta = new int[heights.length];
				System.arraycopy(heights, 0, newHeights, 0, heights.length);
				setHeights(x, z, newHeights);
				setMetadata(x, z, meta);
			}
		}
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int[] newHeights = getHeights(x, z);
				int[] meta = getMeta(x, z);
				int[] heights = parent.getHeights(x, z);

				if (isRiver(riverGrid.getBiome(x, z))) {
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
							Point a = border.get(bestPt);
							current = current.multiply(0f);
							for (int j = -3; j <= 3; j += 2) {
								int opt = bestPt + j;
								if (opt < 0) {
									opt += border.size();
								} else if (opt >= border.size()) {
									opt -= border.size();
								}
								if (opt >= 0 && opt < border.size()) {
									Point b = border.get(opt);
									current.setX(current.getX() + (a.x - b.x));
									current.setY(current.getY() + (a.y - b.y));
								}
							}
						}
					}
					if (current.lengthSquared() > 0) {
						current.normalize();
					}
					perpCurrent.setX(current.getZ());
					perpCurrent.setY(-current.getY());
					perpCurrent.normalize();

					for (int j = 0; j < heights.length; j++) {
						int y = heights[j];

						// Blend height
						int riverY, fallOffY = y;
						boolean dropCenter = false;
						{
							double count = 0;
							double totalY = 0;
							int fallOffTime = 0;
							for (Vector test : new Vector[] { perpCurrent }) {
								for (int sign = -1; sign <= 1; sign += 2) {
									int lastY = y;
									for (int t = 1; t < 10; t++) {
										int tX = x
												+ (int) Math.round(test.getX()
														* t * sign);
										int tZ = z
												+ (int) Math.round(test.getZ()
														* t * sign);
										if (parent.isInBounds(tX, tZ)) {
											int hI = parent.getHeightIndexNear(
													tX, lastY, tZ);
											if (hI >= 0) {
												int res = parent.getHeights(tX,
														tZ)[hI];
												if (Math.abs(lastY - res) < GenerationTuning.NEIGHBOR_TOLERANCE) {
													if (isRiver(riverGrid
															.getBiome(tX, tZ))) {
														lastY = res;
														totalY += res;
														count++;
														if (isInBounds(tX, tZ)) {
															getMeta(tX, tZ)[hI] |= 0x40;
														}
														continue;
													}
												}
												fallOffY = Math.min(res,
														fallOffY);
												fallOffTime = t;
											} else {
												fallOffY = 0;
												fallOffTime = t;
											}
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
								Biome test = riverGrid.getBiome(resX, resZ);
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

						int finalRiverY = riverY - (dropCenter ? 1 : 0);
						if (count <= 2/* || fallOffY <= riverY - 6 */
								|| minNeighbor <= y - 5) {
							meta[j] = 0 | RIVER_SHORE_MASK;
						} else {
							newHeights[j] = finalRiverY;
							meta[j] = (dropCenter ? 1 : 0) | RIVER_MASK;
						}
					}
					break;
				}
			}
		}
	}

	public BiomeIntensityGrid getRiverGrid() {
		return riverGrid;
	}
}
