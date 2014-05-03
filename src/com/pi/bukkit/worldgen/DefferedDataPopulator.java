package com.pi.bukkit.worldgen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

public class DefferedDataPopulator extends BlockPopulator {
	public static interface SetBlockData {
		public void setData(Block b);
	}

	private class DefferedBlock {
		int x, y, z;
		short data;
		SetBlockData bkg;

		private DefferedBlock(int x, int y, int z, short data) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.data = data;
		}

		private DefferedBlock(int x, int y, int z, SetBlockData data) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.bkg = data;
		}
	}

	private ReentrantLock mapLock = new ReentrantLock();
	private Map<Point, List<DefferedBlock>> deffered = new HashMap<Point, List<DefferedBlock>>();

	@Override
	public void populate(World world, Random random, Chunk source) {
		List<DefferedBlock> lst = deffered.get(new Point(source.getX(), source
				.getZ()));
		if (lst != null) {
			List<DefferedBlock> res = new ArrayList<DefferedBlock>();
			res.addAll(lst);
			lst.clear();
			for (DefferedBlock b : res) {
				Block bb = source.getBlock(b.x, b.y, b.z);
				if (bb != null) {
					bb.setData((byte) b.data);
					if (b.bkg != null) {
						b.bkg.setData(bb);
					}
				}
			}
		}
	}

	public void addBlock(int chunkX, int chunkZ, int x, int y, int z, short data) {
		mapLock.lock();
		List<DefferedBlock> lst = deffered.get(new Point(chunkX, chunkZ));
		if (lst == null) {
			lst = new ArrayList<DefferedBlock>();
			deffered.put(new Point(chunkX, chunkZ), lst);
		}
		lst.add(new DefferedBlock(x, y, z, data));
		mapLock.unlock();
	}

	public void addBlock(int chunkX, int chunkZ, int x, int y, int z, SetBlockData data) {
		mapLock.lock();
		List<DefferedBlock> lst = deffered.get(new Point(chunkX, chunkZ));
		if (lst == null) {
			lst = new ArrayList<DefferedBlock>();
			deffered.put(new Point(chunkX, chunkZ), lst);
		}
		lst.add(new DefferedBlock(x, y, z, data));
		mapLock.unlock();
	}
}
