package com.effectiveosgi.rt.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class ControlOptionsTest {

	@Test
	public void testBreakArgsAtSentinel1() {
		final ControlOptions opts = ControlOptions.parseArgs("-?", "-d", "full", "--", "-X");
		assertArrayEquals(new String[] { "-X" }, opts.getRemainingArgs());
		assertTrue(opts.isHelp());
		assertEquals(InspectLevel.Full, opts.getInspectLevel().orElse(null));
	}

	@Test
	public void testBreakArgsAtSentinel2() {
		final ControlOptions opts = ControlOptions.parseArgs("-?", "--detail=full", "--", "-X");
		assertArrayEquals(new String[] { "-X" }, opts.getRemainingArgs());
		assertTrue(opts.isHelp());
		assertEquals(InspectLevel.Full, opts.getInspectLevel().orElse(null));
	}

	@Test
	public void testBreakArgsAtNonLeadingHyphen() {
		final ControlOptions opts = ControlOptions.parseArgs("-d", "basic", "lb", "-s");
		assertArrayEquals(new String[] { "lb", "-s" }, opts.getRemainingArgs());
	}

}
