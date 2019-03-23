package com.effectiveosgi.rt.inspect.web.impl;

import java.util.Enumeration;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

public class EnumerationSpliterator<T> extends AbstractSpliterator<T> {
	
	private final Enumeration<? extends T> enumeration;

	public EnumerationSpliterator(Enumeration<? extends T> enumeration) {
		super(Long.MAX_VALUE, 0);
		this.enumeration = enumeration;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		final boolean result;
		if (enumeration.hasMoreElements()) {
			T element = enumeration.nextElement();
			action.accept(element);
			result = true;
		} else {
			result = false;
		}
		return result;
	}

}
