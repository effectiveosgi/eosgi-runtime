package com.effectiveosgi.rt.config.impl;

import java.util.Map;

public final class EntryImpl<K,V> implements Map.Entry<K,V> {
    private final K key;
    private V value;
    public EntryImpl(K key, V value) {
        this.key = key; this.value = value;
    }
    @Override
    public K getKey() {
        return key;
    }
    @Override
    public V getValue() {
        return value;
    }
    @Override
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }
}