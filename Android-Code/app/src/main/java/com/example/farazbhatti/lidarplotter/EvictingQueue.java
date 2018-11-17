package com.example.farazbhatti.lidarplotter;

import android.support.v4.util.CircularArray;

public class EvictingQueue<T> {

    private int max;
    private CircularArray<T> array;

    public EvictingQueue(int max) {
        this.max = max;
        array = new CircularArray<>(max);
    }

    public void add(T elem) {
        if (array.size() == max)
            array.popLast();

        array.addFirst(elem);
    }

    public T pop() {
        return array.popLast();
    }

    public T get(int n) { return array.get(size()-n-1); }

    public int size() {
        return array.size();
    }
}