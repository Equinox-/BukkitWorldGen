package com.pi.bukkit.worldgen.floatingisland.gen.base;

import org.bukkit.World;

import com.pi.bukkit.worldgen.floatingisland.gen.BiomeIntensityGrid;

public abstract class BaselineTransform extends Baseline {
	protected Baseline parent;

	public BaselineTransform(World w, int chunkX, int chunkZ,
			BiomeIntensityGrid backing, Baseline parent, int heightMapOversample) {
		super(w, chunkX, chunkZ, backing, heightMapOversample);
		this.parent = parent;
		regenerateLayer();
	}

	public abstract void regenerateLayer();
}
