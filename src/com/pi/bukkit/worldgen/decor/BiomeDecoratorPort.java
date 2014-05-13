package com.pi.bukkit.worldgen.decor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.server.BiomeBase;
import net.minecraft.server.BiomeDecorator;
import net.minecraft.server.BlockFlowers;
import net.minecraft.server.Blocks;
import net.minecraft.server.Material;
import net.minecraft.server.World;
import net.minecraft.server.WorldGenCactus;
import net.minecraft.server.WorldGenClay;
import net.minecraft.server.WorldGenDeadBush;
import net.minecraft.server.WorldGenFlowers;
import net.minecraft.server.WorldGenHugeMushroom;
import net.minecraft.server.WorldGenLiquids;
import net.minecraft.server.WorldGenMinable;
import net.minecraft.server.WorldGenPumpkin;
import net.minecraft.server.WorldGenReed;
import net.minecraft.server.WorldGenTreeAbstract;
import net.minecraft.server.WorldGenWaterLily;
import net.minecraft.server.WorldGenerator;

import com.pi.bukkit.ServerCrossover;

public class BiomeDecoratorPort {
	protected World world;
	protected Random random;
	protected int baseX;
	protected int baseZ;
	protected WorldGenerator clayGen = new WorldGenClay(4);
	protected WorldGenerator sandGen = new WorldGenEasySand(Blocks.SAND, 7);
	protected WorldGenerator gravelGen = new WorldGenEasySand(Blocks.GRAVEL, 6);
	protected WorldGenerator dirtGen = new WorldGenMinable(Blocks.DIRT, 32);
	protected WorldGenerator gravelMineGen = new WorldGenMinable(Blocks.GRAVEL,
			32);
	protected WorldGenerator coalGen = new WorldGenMinable(Blocks.COAL_ORE, 16);
	protected WorldGenerator ironGen = new WorldGenMinable(Blocks.IRON_ORE, 8);
	protected WorldGenerator goldGen = new WorldGenMinable(Blocks.GOLD_ORE, 8);
	protected WorldGenerator redstoneGen = new WorldGenMinable(
			Blocks.REDSTONE_ORE, 7);
	protected WorldGenerator diamondGen = new WorldGenMinable(
			Blocks.DIAMOND_ORE, 7);
	protected WorldGenerator lapisGen = new WorldGenMinable(Blocks.LAPIS_ORE, 6);
	protected WorldGenFlowers yellowFlowerGen = new WorldGenFlowers(
			Blocks.YELLOW_FLOWER);
	protected WorldGenerator brownMushroomGen = new WorldGenFlowers(
			Blocks.BROWN_MUSHROOM);
	protected WorldGenerator redMushroomGen = new WorldGenFlowers(
			Blocks.RED_MUSHROOM);
	protected WorldGenerator hugeMushroomGen = new WorldGenHugeMushroom();
	protected WorldGenerator reedGen = new WorldGenReed();
	protected WorldGenerator cactusGen = new WorldGenCactus();
	protected WorldGenerator waterLilyGen = new WorldGenWaterLily();
	/**
	 * Water lily passes
	 */
	protected int w;
	/**
	 * Base tree passes
	 */
	protected int x;
	/**
	 * Yellow flower passes
	 */
	protected int y = 2;
	/**
	 * Grass decoration passes
	 */
	protected int z = 1;
	/**
	 * Dead bush passes
	 */
	protected int A;
	/**
	 * Red/brown mushroom passes
	 */
	protected int B;
	/**
	 * Reed gen passes
	 */
	protected int C;
	/**
	 * Cactus gen passes
	 */
	protected int D;
	protected int E = 1;
	/**
	 * Sand passes
	 */
	protected int F = 3;
	/**
	 * Clay passes
	 */
	protected int G = 1;
	/**
	 * Huge mushroom passes
	 */
	protected int H;
	/**
	 * Liquids gen passes
	 */
	public boolean I = true;

	public void a(World paramWorld, Random paramRandom,
			BiomeBase paramBiomeBase, int baseX, int baseZ) {
		// Duplicate fields
		try {
			Class<?> clazz = BiomeDecorator.class;
			for (Field f : clazz.getDeclaredFields()) {
				if (!f.getType().isPrimitive()) {
					continue;
				}
				try {
					Field local = BiomeDecoratorPort.class.getDeclaredField(f
							.getName());
					local.setAccessible(true);
					f.setAccessible(true);
					if (!f.getName().equals("a") && f.getType().isPrimitive()
							&& f.getType().equals(local.getType())) {
						try {
							local.set(this, f.get(paramBiomeBase.ar));
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (this.world != null)
			throw new RuntimeException("Already decorating!!");
		this.world = paramWorld;
		this.random = paramRandom;
		this.baseX = baseX;
		this.baseZ = baseZ;

		genDecorations(paramBiomeBase);

		this.world = null;
		this.random = null;
	}

	protected void genDecorations(BiomeBase paramBiomeBase) {
		int i1, i2, i3;
		genOres();
		for (i1 = 0; i1 < this.F; i1++) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			this.sandGen.a(this.world, this.random, i2, this.world.i(i2, i3),
					i3);
		}

		for (i1 = 0; i1 < this.G; i1++) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			this.clayGen.a(this.world, this.random, i2, this.world.i(i2, i3),
					i3);
		}

		for (i1 = 0; i1 < this.E; i1++) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			this.gravelGen.a(this.world, this.random, i2, this.world.i(i2, i3),
					i3);
		}

		i1 = this.x;
		if (this.random.nextInt(10) == 0)
			i1++;
		int i4;
		Object localObject;
		for (i2 = 0; i2 < i1; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			localObject = paramBiomeBase.a(this.random);
			((WorldGenTreeAbstract) localObject).a(1.0D, 1.0D, 1.0D);
			for (int i5 : tops) {
				if (((WorldGenTreeAbstract) localObject).a(this.world,
						this.random, i3, i5, i4)) {
					((WorldGenTreeAbstract) localObject).b(this.world,
							this.random, i3, i5, i4);
				}
			}
		}

		for (i2 = 0; i2 < this.H; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				this.hugeMushroomGen.a(this.world, this.random, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.y; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5);
				localObject = paramBiomeBase.a(this.random, i3, i5, i4);
				BlockFlowers localBlockFlowers = BlockFlowers
						.e((String) localObject);
				if (localBlockFlowers.getMaterial() != Material.AIR) {
					this.yellowFlowerGen.a(localBlockFlowers,
							BlockFlowers.f((String) localObject));
					this.yellowFlowerGen.a(this.world, this.random, i3, i5, i4);
				}
			}
		}

		for (i2 = 0; i2 < this.z; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				localObject = paramBiomeBase.b(this.random);
				((WorldGenerator) localObject).a(this.world, this.random, i3,
						i5, i4);
			}
		}

		for (i2 = 0; i2 < this.A; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				new WorldGenDeadBush(Blocks.DEAD_BUSH).a(this.world,
						this.random, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.w; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				while ((i5 > 0) && (this.world.isEmpty(i3, i5 - 1, i4)))
					i5--;
				this.waterLilyGen.a(this.world, this.random, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.B; i2++) {
			if (this.random.nextInt(4) == 0) {
				i3 = this.baseX + this.random.nextInt(16) + 8;
				i4 = this.baseZ + this.random.nextInt(16) + 8;
				int[] tops = getHighestBlockYAt(i3, i4);
				for (int i5 : tops) {
					i5 = this.world.getHighestBlockYAt(i3, i4);
					this.brownMushroomGen
							.a(this.world, this.random, i3, i5, i4);
				}
			}

			if (this.random.nextInt(8) == 0) {
				i3 = this.baseX + this.random.nextInt(16) + 8;
				i4 = this.baseZ + this.random.nextInt(16) + 8;
				int[] tops = getHighestBlockYAt(i3, i4);
				for (int i5 : tops) {
					i5 = this.random.nextInt(i5 * 2);
					this.redMushroomGen.a(this.world, this.random, i3, i5, i4);
				}
			}
		}

		if (this.random.nextInt(4) == 0) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i4 = this.random.nextInt(i5 * 2);
				this.brownMushroomGen.a(this.world, this.random, i2, i4, i3);
			}
		}

		if (this.random.nextInt(8) == 0) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				this.redMushroomGen.a(this.world, this.random, i2, i5, i3);
			}
		}

		for (i2 = 0; i2 < this.C; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				this.reedGen.a(this.world, this.random, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < 10; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				this.reedGen.a(this.world, this.random, i3, i5, i4);
			}
		}

		if (this.random.nextInt(32) == 0) {
			i2 = this.baseX + this.random.nextInt(16) + 8;
			i3 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i4 = this.random.nextInt(i5 * 2);
				new WorldGenPumpkin().a(this.world, this.random, i2, i4, i3);
			}
		}

		for (i2 = 0; i2 < this.D; i2++) {
			i3 = this.baseX + this.random.nextInt(16) + 8;
			i4 = this.baseZ + this.random.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.random.nextInt(i5 * 2);
				this.cactusGen.a(this.world, this.random, i3, i5, i4);
			}
		}

		if (this.I) {
			for (i2 = 0; i2 < 50; i2++) {
				i3 = this.baseX + this.random.nextInt(16) + 8;
				i4 = this.random.nextInt(this.random.nextInt(248) + 8);
				int i5 = this.baseZ + this.random.nextInt(16) + 8;
				new WorldGenLiquids(Blocks.WATER).a(this.world, this.random,
						i3, i4, i5);
			}

			for (i2 = 0; i2 < 20; i2++) {
				i3 = this.baseX + this.random.nextInt(16) + 8;
				i4 = this.random.nextInt(this.random.nextInt(this.random
						.nextInt(240) + 8) + 8);
				int i5 = this.baseZ + this.random.nextInt(16) + 8;
				new WorldGenLiquids(Blocks.LAVA).a(this.world, this.random, i3,
						i4, i5);
			}
		}
	}

	protected void genInRange(int count, WorldGenerator gen, int minY, int maxY) {
		for (int i1 = 0; i1 < count; i1++) {
			int x = this.baseX + this.random.nextInt(16);
			int y = this.random.nextInt(maxY - minY) + minY;
			int z = this.baseZ + this.random.nextInt(16);
			gen.a(this.world, this.random, x, y, z);
		}
	}

	protected void genInVariedRange(int count,
			WorldGenerator paramWorldGenerator, int median, int variance) {
		for (int i1 = 0; i1 < count; i1++) {
			int x = this.baseX + this.random.nextInt(16);
			int y = this.random.nextInt(variance)
					+ this.random.nextInt(variance) + (median - variance);
			int z = this.baseZ + this.random.nextInt(16);
			paramWorldGenerator.a(this.world, this.random, x, y, z);
		}
	}

	protected void genOres() {
		genInRange(20, this.dirtGen, 0, 256);
		genInRange(10, this.gravelMineGen, 0, 256);
		genInRange(20, this.coalGen, 0, 128);
		genInRange(20, this.ironGen, 0, 64);
		genInRange(2, this.goldGen, 0, 32);
		genInRange(8, this.redstoneGen, 0, 16);
		genInRange(1, this.diamondGen, 0, 16);
		genInVariedRange(1, this.lapisGen, 16, 16);
	}

	protected int[] getHighestBlockYAt(int x, int z) {
		ArrayList<Integer> blocks = new ArrayList<Integer>();
		int y = world.getHighestBlockYAt(x, z);
		if (y <= 0) {
			return new int[0];
		}
		blocks.add(y);
		y--;
		for (; y >= 0; y--) {
			if (ServerCrossover.isTop(world.getChunkAtWorldCoords(x, z)
					.getData(x & 15, y, z & 15))
					&& world.getChunkAtWorldCoords(x, z).getData(x & 15, y + 1,
							z & 15) == 0
					&& world.getChunkAtWorldCoords(x, z).getData(x & 15, y + 3,
							z & 15) == 0) {
				if (y <= 0) {
					break;
				}
				blocks.add(y);
				y -= 10;
			}
		}
		int[] results = new int[blocks.size()];
		for (int i = 0; i < blocks.size(); i++) {
			results[i] = blocks.get(i).intValue();
		}
		return results;
	}
}
