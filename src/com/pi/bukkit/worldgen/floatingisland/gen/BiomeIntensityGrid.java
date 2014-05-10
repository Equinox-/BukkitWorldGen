package com.pi.bukkit.worldgen.floatingisland.gen;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

public class BiomeIntensityGrid implements BiomeGrid {
	private static int BIOME_BLEND_RADIUS = 16;
	private final int chunkX, chunkZ;
	private final World world;
	/**
	 * If this is null it is a RO
	 */
	private final BiomeGrid backing;
	public Biome[][] biomes;
	private float[][][] intensity;

	public BiomeIntensityGrid(World w, BiomeGrid backing, int chunkX, int chunkZ) {
		this.backing = backing;
		this.world = w;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		regenerateBiomeLevels();
	}

	private BiomeIntensityGrid(World w, int chunkX, int chunkZ) {
		this.world = w;
		this.backing = null;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	private void applyBiome(int x, int z, Biome here, float mult) {
		for (int xO = -BIOME_BLEND_RADIUS; xO <= BIOME_BLEND_RADIUS; xO++) {
			for (int zO = -BIOME_BLEND_RADIUS; zO <= BIOME_BLEND_RADIUS; zO++) {
				int tX = x + xO;
				int tZ = z + zO;
				if (tX >= -GenerationTuning.BIOME_OVERSAMPLE
						&& tZ >= -GenerationTuning.BIOME_OVERSAMPLE
						&& tX < 16 + GenerationTuning.BIOME_OVERSAMPLE
						&& tZ < 16 + GenerationTuning.BIOME_OVERSAMPLE) {
					intensity[tX + GenerationTuning.BIOME_OVERSAMPLE][tZ
							+ GenerationTuning.BIOME_OVERSAMPLE][here.ordinal()] += mult
							/ (1.0 + (xO * xO + zO * zO));
				}
			}
		}
	}

	public void regenerateBiomeLevels() {
		if (backing == null) {
			System.err.println("WARN: Attempt biome level regen on RO grid");
			return;
		}
		intensity = new float[16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][Biome
				.values().length];
		biomes = new Biome[16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)];
		int realX = chunkX << 4;
		int realZ = chunkZ << 4;
		for (int x = -16; x < 32; x++) {
			for (int z = -16; z < 32; z++) {
				Biome here = null;
				if (x >= 0 && z >= 0 && x < 16 && z < 16) {
					here = backing.getBiome(x, z);
				} else {
					here = world.getBiome(realX + x, realZ + z);
				}
				if (x >= -GenerationTuning.BIOME_OVERSAMPLE
						&& z >= -GenerationTuning.BIOME_OVERSAMPLE
						&& x < 16 + GenerationTuning.BIOME_OVERSAMPLE
						&& z < 16 + GenerationTuning.BIOME_OVERSAMPLE) {
					biomes[x + GenerationTuning.BIOME_OVERSAMPLE][z
							+ GenerationTuning.BIOME_OVERSAMPLE] = here;
					applyBiome(x, z, here, 1f);
				}

			}
		}
	}

	@Override
	public Biome getBiome(int x, int z) {
		if (x >= -GenerationTuning.BIOME_OVERSAMPLE
				&& z >= -GenerationTuning.BIOME_OVERSAMPLE
				&& x < 16 + GenerationTuning.BIOME_OVERSAMPLE
				&& z < 16 + GenerationTuning.BIOME_OVERSAMPLE) {
			return biomes[x + GenerationTuning.BIOME_OVERSAMPLE][z
					+ GenerationTuning.BIOME_OVERSAMPLE];
		}
		return world.getBiome(x + (chunkX << 4), z + (chunkZ << 4));
	}

	public float[] getBiomeIntensity(int x, int z) {
		return intensity[x + GenerationTuning.BIOME_OVERSAMPLE][z
				+ GenerationTuning.BIOME_OVERSAMPLE];
	}

	public float getBiomeIntensity(int x, int z, Biome b) {
		return intensity[x + GenerationTuning.BIOME_OVERSAMPLE][z
				+ GenerationTuning.BIOME_OVERSAMPLE][b.ordinal()];
	}

	@Override
	public void setBiome(int x, int z, Biome bio) {
		if (x >= -GenerationTuning.BIOME_OVERSAMPLE
				&& z >= -GenerationTuning.BIOME_OVERSAMPLE
				&& x < 16 + GenerationTuning.BIOME_OVERSAMPLE
				&& z < 16 + GenerationTuning.BIOME_OVERSAMPLE) {
			// Clear out old values
			applyBiome(x, z, getBiome(x, z), -1f);
			biomes[x + GenerationTuning.BIOME_OVERSAMPLE][z
					+ GenerationTuning.BIOME_OVERSAMPLE] = bio;
			applyBiome(x, z, bio, 1f);
			if (backing != null) {
				if (x >= 0 && z >= 0 && x < 16 && z < 16) {
					backing.setBiome(x, z, bio);
				} else {
					world.setBiome((chunkX << 4) + x, (chunkZ << 4) + z, bio);
				}
			}
		}
	}

	@Override
	public BiomeIntensityGrid clone() {
		BiomeIntensityGrid g = new BiomeIntensityGrid(world, chunkX, chunkZ);
		g.intensity = new float[16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][Biome
				.values().length];
		g.biomes = new Biome[16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)][16 + (GenerationTuning.BIOME_OVERSAMPLE * 2)];
		for (int i = 0; i < 16 + (GenerationTuning.BIOME_OVERSAMPLE * 2); i++) {
			System.arraycopy(intensity[i], 0, g.intensity[i], 0,
					16 + (GenerationTuning.BIOME_OVERSAMPLE * 2));
			System.arraycopy(biomes[i], 0, g.biomes[i], 0,
					16 + (GenerationTuning.BIOME_OVERSAMPLE * 2));
		}
		return g;
	}
}
