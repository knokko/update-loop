package com.github.knokko.update;

class SlidingEntry {

    final int age;
    final long value;

    SlidingEntry(int age, long value) {
        this.age = age;
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SlidingEntry) {
            SlidingEntry entry = (SlidingEntry) other;
            return this.age == entry.age && this.value == entry.value;
        } else return false;
    }

    @Override
    public int hashCode() {
        return 3 * age - 31 * (int) value;
    }

    @Override
    public String toString() {
        return "(age=" + age + ",value=" + value + ")";
    }
}
