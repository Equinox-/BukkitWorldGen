package com.pi.bukkit.worldgen.floatingisland.gen;

@SuppressWarnings("unused")
public class GenerationTuning {
	public static final int BIOME_OVERSAMPLE = 16;
	public static final int HEIGHT_OVERSAMPLE = 3;
	public static final int EDGE_OVERSAMPLE = 2;

	public static final int NEIGHBOR_TOLERANCE = 10;
	static {
		if (BIOME_OVERSAMPLE < HEIGHT_OVERSAMPLE) {
			throw new RuntimeException("BIOME_OVERSAMPLE < HEIGHT_OVERSAMPLE");
		}
		if (HEIGHT_OVERSAMPLE <= EDGE_OVERSAMPLE) {
			throw new RuntimeException("HEIGHT_OVERSAMPLE <= EDGE_OVERSAMPLE");
		}
	}
}
