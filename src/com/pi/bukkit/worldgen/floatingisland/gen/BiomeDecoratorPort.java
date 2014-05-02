package com.pi.bukkit.worldgen.floatingisland.gen;

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
import net.minecraft.server.WorldGenSand;
import net.minecraft.server.WorldGenTreeAbstract;
import net.minecraft.server.WorldGenWaterLily;
import net.minecraft.server.WorldGenerator;

import com.pi.bukkit.ServerCrossover;

public class BiomeDecoratorPort {
	protected World a;
	protected Random b;
	protected int c;
	protected int d;
	protected WorldGenerator e = new WorldGenClay(4);
	protected WorldGenerator f = new WorldGenSand(Blocks.SAND, 7);
	protected WorldGenerator g = new WorldGenSand(Blocks.GRAVEL, 6);
	protected WorldGenerator h = new WorldGenMinable(Blocks.DIRT, 32);
	protected WorldGenerator i = new WorldGenMinable(Blocks.GRAVEL, 32);
	protected WorldGenerator j = new WorldGenMinable(Blocks.COAL_ORE, 16);
	protected WorldGenerator k = new WorldGenMinable(Blocks.IRON_ORE, 8);
	protected WorldGenerator l = new WorldGenMinable(Blocks.GOLD_ORE, 8);
	protected WorldGenerator m = new WorldGenMinable(Blocks.REDSTONE_ORE, 7);
	protected WorldGenerator n = new WorldGenMinable(Blocks.DIAMOND_ORE, 7);
	protected WorldGenerator o = new WorldGenMinable(Blocks.LAPIS_ORE, 6);
	protected WorldGenFlowers p = new WorldGenFlowers(Blocks.YELLOW_FLOWER);
	protected WorldGenerator q = new WorldGenFlowers(Blocks.BROWN_MUSHROOM);
	protected WorldGenerator r = new WorldGenFlowers(Blocks.RED_MUSHROOM);
	protected WorldGenerator s = new WorldGenHugeMushroom();
	protected WorldGenerator t = new WorldGenReed();
	protected WorldGenerator u = new WorldGenCactus();
	protected WorldGenerator v = new WorldGenWaterLily();
	protected int w;
	protected int x;
	protected int y = 2;
	protected int z = 1;
	protected int A;
	protected int B;
	protected int C;
	protected int D;
	protected int E = 1;
	protected int F = 3;
	protected int G = 1;
	protected int H;
	public boolean I = true;

	public void a(World paramWorld, Random paramRandom,
			BiomeBase paramBiomeBase, int paramInt1, int paramInt2) {
		// Duplicate fields
		try {
			Class<?> clazz = BiomeDecorator.class;
			for (Field f : clazz.getDeclaredFields()) {
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
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (this.a != null)
			throw new RuntimeException("Already decorating!!");
		this.a = paramWorld;
		this.b = paramRandom;
		this.c = paramInt1;
		this.d = paramInt2;

		a(paramBiomeBase);

		this.a = null;
		this.b = null;
	}

	protected void a(BiomeBase paramBiomeBase) {
		int i1, i2, i3;
		a();
		for (i1 = 0; i1 < this.F; i1++) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			this.f.a(this.a, this.b, i2, this.a.i(i2, i3), i3);
		}

		for (i1 = 0; i1 < this.G; i1++) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			this.e.a(this.a, this.b, i2, this.a.i(i2, i3), i3);
		}

		for (i1 = 0; i1 < this.E; i1++) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			this.g.a(this.a, this.b, i2, this.a.i(i2, i3), i3);
		}

		i1 = this.x;
		if (this.b.nextInt(10) == 0)
			i1++;
		int i4;
		Object localObject;
		for (i2 = 0; i2 < i1; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			localObject = paramBiomeBase.a(this.b);
			((WorldGenTreeAbstract) localObject).a(1.0D, 1.0D, 1.0D);
			for (int i5 : tops) {
				if (((WorldGenTreeAbstract) localObject).a(this.a, this.b, i3,
						i5, i4)) {
					((WorldGenTreeAbstract) localObject).b(this.a, this.b, i3,
							i5, i4);
				}
			}
		}

		for (i2 = 0; i2 < this.H; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				this.s.a(this.a, this.b, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.y; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5);
				localObject = paramBiomeBase.a(this.b, i3, i5, i4);
				BlockFlowers localBlockFlowers = BlockFlowers
						.e((String) localObject);
				if (localBlockFlowers.getMaterial() != Material.AIR) {
					this.p.a(localBlockFlowers,
							BlockFlowers.f((String) localObject));
					this.p.a(this.a, this.b, i3, i5, i4);
				}
			}
		}

		for (i2 = 0; i2 < this.z; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				localObject = paramBiomeBase.b(this.b);
				((WorldGenerator) localObject).a(this.a, this.b, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.A; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				new WorldGenDeadBush(Blocks.DEAD_BUSH).a(this.a, this.b, i3,
						i5, i4);
			}
		}

		for (i2 = 0; i2 < this.w; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				while ((i5 > 0) && (this.a.isEmpty(i3, i5 - 1, i4)))
					i5--;
				this.v.a(this.a, this.b, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < this.B; i2++) {
			if (this.b.nextInt(4) == 0) {
				i3 = this.c + this.b.nextInt(16) + 8;
				i4 = this.d + this.b.nextInt(16) + 8;
				int[] tops = getHighestBlockYAt(i3, i4);
				for (int i5 : tops) {
					i5 = this.a.getHighestBlockYAt(i3, i4);
					this.q.a(this.a, this.b, i3, i5, i4);
				}
			}

			if (this.b.nextInt(8) == 0) {
				i3 = this.c + this.b.nextInt(16) + 8;
				i4 = this.d + this.b.nextInt(16) + 8;
				int[] tops = getHighestBlockYAt(i3, i4);
				for (int i5 : tops) {
					i5 = this.b.nextInt(i5 * 2);
					this.r.a(this.a, this.b, i3, i5, i4);
				}
			}
		}

		if (this.b.nextInt(4) == 0) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i4 = this.b.nextInt(i5 * 2);
				this.q.a(this.a, this.b, i2, i4, i3);
			}
		}

		if (this.b.nextInt(8) == 0) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				this.r.a(this.a, this.b, i2, i5, i3);
			}
		}

		for (i2 = 0; i2 < this.C; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				this.t.a(this.a, this.b, i3, i5, i4);
			}
		}

		for (i2 = 0; i2 < 10; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				this.t.a(this.a, this.b, i3, i5, i4);
			}
		}

		if (this.b.nextInt(32) == 0) {
			i2 = this.c + this.b.nextInt(16) + 8;
			i3 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i2, i3);
			for (int i5 : tops) {
				i4 = this.b.nextInt(i5 * 2);
				new WorldGenPumpkin().a(this.a, this.b, i2, i4, i3);
			}
		}

		for (i2 = 0; i2 < this.D; i2++) {
			i3 = this.c + this.b.nextInt(16) + 8;
			i4 = this.d + this.b.nextInt(16) + 8;
			int[] tops = getHighestBlockYAt(i3, i4);
			for (int i5 : tops) {
				i5 = this.b.nextInt(i5 * 2);
				this.u.a(this.a, this.b, i3, i5, i4);
			}
		}

		if (this.I) {
			for (i2 = 0; i2 < 50; i2++) {
				i3 = this.c + this.b.nextInt(16) + 8;
				i4 = this.b.nextInt(this.b.nextInt(248) + 8);
				int i5 = this.d + this.b.nextInt(16) + 8;
				new WorldGenLiquids(Blocks.WATER).a(this.a, this.b, i3, i4, i5);
			}

			for (i2 = 0; i2 < 20; i2++) {
				i3 = this.c + this.b.nextInt(16) + 8;
				i4 = this.b
						.nextInt(this.b.nextInt(this.b.nextInt(240) + 8) + 8);
				int i5 = this.d + this.b.nextInt(16) + 8;
				new WorldGenLiquids(Blocks.LAVA).a(this.a, this.b, i3, i4, i5);
			}
		}
	}

	protected void a(int paramInt1, WorldGenerator paramWorldGenerator,
			int paramInt2, int paramInt3) {
		for (int i1 = 0; i1 < paramInt1; i1++) {
			int i2 = this.c + this.b.nextInt(16);
			int i3 = this.b.nextInt(paramInt3 - paramInt2) + paramInt2;
			int i4 = this.d + this.b.nextInt(16);
			paramWorldGenerator.a(this.a, this.b, i2, i3, i4);
		}
	}

	protected void b(int paramInt1, WorldGenerator paramWorldGenerator,
			int paramInt2, int paramInt3) {
		for (int i1 = 0; i1 < paramInt1; i1++) {
			int i2 = this.c + this.b.nextInt(16);
			int i3 = this.b.nextInt(paramInt3) + this.b.nextInt(paramInt3)
					+ (paramInt2 - paramInt3);
			int i4 = this.d + this.b.nextInt(16);
			paramWorldGenerator.a(this.a, this.b, i2, i3, i4);
		}
	}

	protected void a() {
		a(20, this.h, 0, 256);
		a(10, this.i, 0, 256);
		a(20, this.j, 0, 128);
		a(20, this.k, 0, 64);
		a(2, this.l, 0, 32);
		a(8, this.m, 0, 16);
		a(1, this.n, 0, 16);
		b(1, this.o, 16, 16);
	}

	protected int[] getHighestBlockYAt(int x, int z) {
		ArrayList<Integer> blocks = new ArrayList<Integer>();
		int y = a.getHighestBlockYAt(x, z);
		if (y <= 0) {
			return new int[0];
		}
		blocks.add(y);
		y--;
		for (; y >= 0; y--) {
			if (ServerCrossover.isTop(a.getChunkAtWorldCoords(x, z).getData(
					x & 15, y, z & 15))
					&& a.getChunkAtWorldCoords(x, z).getData(x & 15, y + 1,
							z & 15) == 0
					&& a.getChunkAtWorldCoords(x, z).getData(x & 15, y + 3,
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
