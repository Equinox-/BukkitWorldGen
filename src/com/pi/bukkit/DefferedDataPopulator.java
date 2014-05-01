package com.pi.bukkit;

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
	private class DefferedBlock {
		int x, y, z;
		short data;

		private DefferedBlock(int x, int y, int z, short data) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.data = data;
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
			lst.addAll(res);
			for (DefferedBlock b : res) {
				Block bb = source.getBlock(b.x, b.y, b.z);
				if (bb != null) {
					bb.setData((byte) b.data);
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
}
