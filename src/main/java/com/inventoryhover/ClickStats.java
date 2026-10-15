package com.inventoryhover;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Recent click counts for the click chart, kept both per second and per game
 * tick, long enough for the longest chart window. Written on the client thread
 * and read on the Swing thread, so every access is synchronized.
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

	private static final long SECOND_MILLIS = 1_000;

	/** Longest history kept, in seconds and in ticks (the same two hours). */
	private static final int KEEP_SECONDS = 7200;
	private static final int KEEP_TICKS = 12000;

	/** Counts for one second or one tick. */
	static class Bucket
	{
		final long start;
		final int[] counts = new int[Kind.values().length];

		Bucket(long start)
		{
			this.start = start;
		}
	}

	private final Deque<Bucket> seconds = new ArrayDeque<>();
	private final Deque<Bucket> ticks = new ArrayDeque<>();

	/** Latest game tick count, set by the plugin every tick. */
	private int tick;

	synchronized void setTick(int tick)
	{
		this.tick = tick;
	}

	synchronized void add(Kind kind)
	{
		count(seconds, System.currentTimeMillis() / SECOND_MILLIS, kind);
		count(ticks, tick, kind);
		trim();
	}

	private static void count(Deque<Bucket> buckets, long start, Kind kind)
	{
		Bucket last = buckets.peekLast();
		if (last == null || last.start != start)
		{
			last = new Bucket(start);
			buckets.addLast(last);
		}
		last.counts[kind.ordinal()]++;
	}

	/** Counts per bar of {@code interval} seconds for the last {@code bars} bars, oldest first. */
	synchronized int[][] secondHistory(int interval, int bars)
	{
		trim();
		return history(seconds, System.currentTimeMillis() / SECOND_MILLIS, interval, bars);
	}

	/** Counts per bar of {@code interval} game ticks for the last {@code bars} bars, oldest first. */
	synchronized int[][] tickHistory(int interval, int bars)
	{
		trim();
		return history(ticks, tick, interval, bars);
	}

	/**
	 * Bars cover fixed slices of time, so a finished bar never changes; slicing
	 * by age instead would shift clicks between bars every frame.
	 */
	private static int[][] history(Deque<Bucket> buckets, long now, int interval, int bars)
	{
		long newest = now / interval;
		int[][] history = new int[bars][Kind.values().length];
		for (Bucket bucket : buckets)
		{
			long back = newest - bucket.start / interval;
			if (back < 0 || back >= bars)
			{
				continue;
			}
			int index = bars - 1 - (int) back;
			for (int k = 0; k < bucket.counts.length; k++)
			{
				history[index][k] += bucket.counts[k];
			}
		}
		return history;
	}

	synchronized void reset()
	{
		seconds.clear();
		ticks.clear();
	}

	private void trim()
	{
		long oldestSecond = System.currentTimeMillis() / SECOND_MILLIS - KEEP_SECONDS;
		while (!seconds.isEmpty() && seconds.peekFirst().start < oldestSecond)
		{
			seconds.removeFirst();
		}
		// Tick counts restart on login, so also drop anything from a later count
		while (!ticks.isEmpty() && (ticks.peekFirst().start < tick - KEEP_TICKS || ticks.peekFirst().start > tick))
		{
			ticks.removeFirst();
		}
	}
}
