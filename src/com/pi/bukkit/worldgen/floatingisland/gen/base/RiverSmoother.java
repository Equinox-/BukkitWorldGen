package com.pi.bukkit.worldgen.floatingisland.gen.base;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.Vector;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;
import com.pi.bukkit.worldgen.floatingisland.gen.GenerationTuning;

/**
 * Smooths rivers so that they are generally the same height perpendicular to their current.
 * Generates metadata about what is a shore, ice, etc..
 * Generates depth metadata
 * @author westin
 *
 */
public class RiverSmoother extends BaselineTransform {
	public static int RIVER_MASK = 0x10;
	public static int RIVER_SHORE_MASK = 0x20 | RIVER_MASK;
	public static int RIVER_FALL_MASK = 0x40 | RIVER_MASK;
	public static int RIVER_ICE_MASK = 0x80;
	public static int RIVER_DEPTH_MASK = 0x07;

	private BiomeIntensityGrid riverGrid;

	public RiverSmoother(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent) {
		super(w, chunkX, chunkZ, backing, parent, 0);
		riverGrid = backing;// backing.clone();
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
		if (!isRiver(riverGrid.getBiome(x, z))) {
			return false;
		}
		for (int xO = -1; xO <= 1; xO++) {
			for (int zO = -1; zO <= 1; zO++) {
				Biome here = riverGrid.getBiome(x + xO, z + zO);
				if (!isRiver(here)) {
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
							current = current.multiply(0f);
							Point a = null;
							for (int j = -3; j <= 3; j++) {
								int opt = bestPt + j;
								if (opt < 0) {
									opt += border.size();
								} else if (opt >= border.size()) {
									opt -= border.size();
								}
								if (opt >= 0 && opt < border.size()) {
									Point b = border.get(opt);
									if (a != null) {
										current.setX(current.getX()
												+ (a.x - b.x));
										current.setZ(current.getZ()
												+ (a.y - b.y));
									}
									a = b;
								}
							}
						}
					}
					if (current.lengthSquared() > 0) {
						current.normalize();
					}
					perpCurrent.setX(current.getZ());
					perpCurrent.setZ(-current.getX());

					for (int j = 0; j < heights.length; j++) {
						int y = heights[j];

						// Blend height
						int riverY, fallOffY = y;
						int fallOffTime = 0;
						boolean dropCenter = false;
						{
							double count = 0;
							double totalY = 0;
							for (Vector test : new Vector[] { perpCurrent,
									current }) {
								for (int sign = -1; sign <= 1; sign += 2) {
									int lastY = y;
									for (int t = 1; t < (test == current ? 3
											: 10); t++) {
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
														continue;
													}
												}
												if (test == perpCurrent) {
													fallOffY = Math.min(res,
															fallOffY);
													fallOffTime = t;
												}
											} else {
												if (test == perpCurrent) {
													fallOffY = 0;
													fallOffTime = t;
												}
											}
										}
										break;
									}
								}
							}
							riverY = (int) ((y + totalY) / (1 + count));
							dropCenter = (fallOffTime > (count / 2));
						}

						// Check biome neighbors
						int count = 0;
						int minNeighbor = y;
						for (int xO = -1; xO <= 1; xO++) {
							for (int zO = -1; zO <= 1; zO++) {
								int resX = x + xO, resZ = z + zO;
								Biome test = riverGrid.getBiome(resX, resZ);
								if (isRiver(test)) {
									count++;
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

						int finalRiverY = Math.min(minNeighbor, riverY)
								- (dropCenter ? 1 : 0);
						if ((fallOffY <= y - 5 && fallOffTime <= 1)
								|| minNeighbor <= y - 5) {
							meta[j] = 0 | RIVER_FALL_MASK;
						} else if (count <= 2) {
							meta[j] = 0 | RIVER_SHORE_MASK;
						} else {
							newHeights[j] = finalRiverY;
							meta[j] = ((dropCenter ? 1 : 0) & RIVER_DEPTH_MASK)
									| RIVER_MASK;
							if (!dropCenter
									&& riverGrid.getBiome(x, z) == Biome.FROZEN_RIVER) {
								meta[j] |= RIVER_ICE_MASK;
							}
						}
					}
				}
			}
		}
	}

	public BiomeIntensityGrid getRiverGrid() {
		return riverGrid;
	}
}
