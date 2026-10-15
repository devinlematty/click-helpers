package com.inventoryhover;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Recent click counts split into fixed time buckets for the click chart. Written on the client thread and read on
 * the Swing thread, so every access is synchronized.
 */
class ClickStats
{
	enum Kind
	{
		/** A click the game accepted on a slot, prayer or spell. */
		USEFUL,
		/** A click on an empty slot or a gap. */
		MISSED,
		/** A click the game refused with a message. */
		REJECTED,
		/** A second click on the same slot in one tick. */
		DOUBLE
	}

	static final long BUCKET_MILLIS = 5_000;
	static final int BUCKETS = 24;

	static class Bucket
	{
		final long start;
		final int[] counts = new int[Kind.values().length];

		Bucket(long start)
		{
			this.start = start;
		}
	}

	private final Deque<Bucket> buckets = new ArrayDeque<>();

	synchronized void add(Kind kind)
	{
		long start = System.currentTimeMillis() / BUCKET_MILLIS * BUCKET_MILLIS;
		Bucket last = buckets.peekLast();
		if (last == null || last.start != start)
		{
			last = new Bucket(start);
			buckets.addLast(last);
		}
		last.counts[kind.ordinal()]++;
		trim();
	}

	/** Counts per bucket for the last {@link #BUCKETS} buckets, oldest first; quiet buckets are zeros. */
	synchronized int[][] history()
	{
		trim();
		long newest = System.currentTimeMillis() / BUCKET_MILLIS * BUCKET_MILLIS;
		int[][] history = new int[BUCKETS][Kind.values().length];
		for (Bucket bucket : buckets)
		{
			int index = BUCKETS - 1 - (int) ((newest - bucket.start) / BUCKET_MILLIS);
			if (index >= 0 && index < BUCKETS)
			{
				history[index] = bucket.counts.clone();
			}
		}
		return history;
	}

	synchronized void reset()
	{
		buckets.clear();
	}

	private void trim()
	{
		long oldest = System.currentTimeMillis() - BUCKET_MILLIS * BUCKETS;
		while (!buckets.isEmpty() && buckets.peekFirst().start < oldest)
		{
			buckets.removeFirst();
		}
	}
}
