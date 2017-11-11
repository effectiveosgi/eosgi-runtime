package com.effectiveosgi.rt.config.impl;

import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Functions for operating on pairs of values, inspired by <a href="https://hackage.haskell.org/package/base-4.10.0.0/docs/Control-Arrow.html">Haskell Arrows</a>
 */
public class Arrows {

	public static <A,B,C> Function<Entry<A,B>, Entry<C,B>> first(Function<A,C> function) {
		return entry -> new EntryImpl<>(function.apply(entry.getKey()), entry.getValue());
	}
	
	public static <A,B,D> Function<Entry<A,B>, Entry<A,D>> second(Function<B,D> function) {
		return entry -> new EntryImpl<>(entry.getKey(), function.apply(entry.getValue()));
	}
	
	public static <A,B,C,D> Function<Entry<A,B>, Entry<C, D>> combine(Function<A,C> first, Function<B,D> second) {
		return entry -> new EntryImpl<>(first.apply(entry.getKey()), second.apply(entry.getValue()));
	}
	
}
