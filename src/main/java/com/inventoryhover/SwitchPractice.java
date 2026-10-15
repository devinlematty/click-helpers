package com.inventoryhover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gear switch practice: lights up random empty inventory slots and times how
 * fast and how cleanly they are all clicked. Runs on the client thread; the
 * side panel only reads the immutable summaries.
 */
class SwitchPractice
{
	/** One finished round. */
	static class Round
	{
		/** From the targets lighting up to the first click. */
		final long reactMillis;
		/** From the first click to the last target clicked. */
		final long switchMillis;
		/** Game ticks the clicks were spread over, 1 when all landed in one tick. */
		final int ticks;
		final int hits;
		final int misses;
		/** Whether the time limit ran out before every target was clicked. */
		final boolean timedOut;

		Round(long reactMillis, long switchMillis, int ticks, int hits, int misses, boolean timedOut)
		{
			this.reactMillis = reactMillis;
			this.switchMillis = switchMillis;
			this.ticks = ticks;
			this.hits = hits;
			this.misses = misses;
			this.timedOut = timedOut;
		}
	}

	/** Averages over the rounds of one practice session, kept for the progress chart. */
	static class Summary
	{
		long time;
		int rounds;
		double reactMillis;
		double switchMillis;
		long bestSwitchMillis;
		double ticks;
		/** Finished rounds done within one tick, 0 to 100. */
		double oneTickRate;
		int timeouts;
		/** Hits as a share of all clicks, 0 to 100. */
		double accuracy;
	}

	enum Result
	{
		HIT,
		MISS,
		IGNORED
	}

	private final Random random = new Random();

	/** Target slot indexes; true once clicked. Insertion order is the click order in ordered mode. */
	private final Map<Integer, Boolean> targets = new LinkedHashMap<>();

	private final List<Round> rounds = new ArrayList<>();

	private long roundStart;
	private long firstClick;
	private int firstTick;
	private int misses;

	/** When the next round may start, in nanoTime. */
	private long nextRound;

	private long sessionStart;

	/** Last finished round, or null. */
	Round lastRound;

	/** Settings, set by the overlay each frame. */
	long roundDelay;
	long extraDelay;
	long timeLimit;
	boolean ordered;
	int roundsPerSession;

	boolean isActive()
	{
		return sessionStart != 0;
	}

	void start(long now)
	{
		reset();
		sessionStart = System.currentTimeMillis();
		nextRound = now;
	}

	/** Ends the session; returns its summary, or null when no round was finished. */
	Summary stop()
	{
		Summary summary = summary();
		reset();
		return summary;
	}

	private void reset()
	{
		targets.clear();
		rounds.clear();
		lastRound = null;
		firstClick = 0;
		misses = 0;
		sessionStart = 0;
	}

	/** Whether the set number of rounds is done, so the session should end. */
	boolean isComplete()
	{
		return isActive() && roundsPerSession > 0 && rounds.size() >= roundsPerSession;
	}

	/** Ends a round that ran out of time, then starts a new one once the pause is over. */
	void update(List<Integer> emptySlots, int count, long now, int tick)
	{
		if (!isActive() || isComplete())
		{
			return;
		}

		if (!targets.isEmpty())
		{
			if (timeLimit > 0 && now - roundStart >= timeLimit)
			{
				int hits = (int) targets.values().stream().filter(b -> b).count();
				misses += targets.size() - hits;
				finishRound(now, tick, hits, true);
			}
			return;
		}

		if (now < nextRound || emptySlots.isEmpty())
		{
			return;
		}

		List<Integer> pool = new ArrayList<>(emptySlots);
		Collections.shuffle(pool, random);
		for (int i = 0; i < Math.min(count, pool.size()); i++)
		{
			targets.put(pool.get(i), false);
		}
		roundStart = now;
		firstClick = 0;
		misses = 0;
	}

	/**
	 * Scores a click in the inventory: a hit on a target not clicked yet (the
	 * next one in ordered mode), a miss anywhere else. Clicks between rounds are
	 * ignored.
	 */
	Result click(int slot, long now, int tick)
	{
		if (!isActive() || targets.isEmpty())
		{
			return Result.IGNORED;
		}

		if (firstClick == 0)
		{
			firstClick = now;
			firstTick = tick;
		}

		Boolean clicked = targets.get(slot);
		if (clicked == null || clicked || (ordered && slot != nextTarget()))
		{
			misses++;
			return Result.MISS;
		}

		targets.put(slot, true);
		if (!targets.containsValue(false))
		{
			finishRound(now, tick, targets.size(), false);
		}
		return Result.HIT;
	}

	private void finishRound(long now, int tick, int hits, boolean timedOut)
	{
		long first = firstClick == 0 ? now : firstClick;
		Round round = new Round(
			(first - roundStart) / 1_000_000,
			(now - first) / 1_000_000,
			firstClick == 0 ? 0 : tick - firstTick + 1,
			hits,
			misses,
			timedOut);
		rounds.add(round);
		lastRound = round;
		targets.clear();
		nextRound = now + roundDelay + (long) (random.nextDouble() * extraDelay);
	}

	/** The slot to click next in ordered mode, or -1. */
	int nextTarget()
	{
		for (Map.Entry<Integer, Boolean> target : targets.entrySet())
		{
			if (!target.getValue())
			{
				return target.getKey();
			}
		}
		return -1;
	}

	/** Target slot indexes mapped to whether they are clicked, in light-up order. */
	Map<Integer, Boolean> targets()
	{
		return targets;
	}

	/** Share of the time limit left in the running round, 1 to 0; 1 with no limit or no round. */
	double timeLeft(long now)
	{
		if (timeLimit <= 0 || targets.isEmpty())
		{
			return 1;
		}
		return Math.max(0, 1 - (double) (now - roundStart) / timeLimit);
	}

	int roundCount()
	{
		return rounds.size();
	}

	/** Averages of this session so far, or null with no finished round. */
	Summary summary()
	{
		if (rounds.isEmpty())
		{
			return null;
		}

		Summary summary = new Summary();
		summary.time = sessionStart;
		summary.rounds = rounds.size();
		summary.bestSwitchMillis = Long.MAX_VALUE;
		int hits = 0;
		int missed = 0;
		int finished = 0;
		int oneTick = 0;
		for (Round round : rounds)
		{
			hits += round.hits;
			missed += round.misses;
			if (round.timedOut)
			{
				summary.timeouts++;
				continue;
			}
			finished++;
			summary.reactMillis += round.reactMillis;
			summary.switchMillis += round.switchMillis;
			summary.bestSwitchMillis = Math.min(summary.bestSwitchMillis, round.switchMillis);
			summary.ticks += round.ticks;
			if (round.ticks <= 1)
			{
				oneTick++;
			}
		}
		if (finished > 0)
		{
			summary.reactMillis /= finished;
			summary.switchMillis /= finished;
			summary.ticks /= finished;
			summary.oneTickRate = oneTick * 100.0 / finished;
		}
		else
		{
			summary.bestSwitchMillis = 0;
		}
		summary.accuracy = hits + missed == 0 ? 0 : hits * 100.0 / (hits + missed);
		return summary;
	}
}
