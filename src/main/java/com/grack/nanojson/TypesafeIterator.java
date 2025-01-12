package com.grack.nanojson;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

class TypesafeIterator<V, I> implements Iterator<V> {
  private final Iterator<I> backend;
  private final int count;
  private int idx;
  private final Class<?> cls;
  private final Function<I, ?> extractor;

  TypesafeIterator(Class<?> cls, Collection<I> backingArray, Function<I, ?> extractor) {
    this.count = (int) backingArray.stream().map(extractor).filter(cls::isInstance).count();
    this.backend = backingArray.iterator();
    this.cls = cls;
    this.extractor = extractor;
  }

  @Override
  public boolean hasNext() {
    return idx < count && backend.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public V next() {
    if (!hasNext()) throw new NoSuchElementException();
    I x;
    boolean found = false;
    do {
      x = backend.next();
      if (cls.isInstance(extractor.apply(x))) {
        found = true;
        break;
      }
    } while (backend.hasNext());
    idx++;
    if (found) return (V) x;
    throw new NoSuchElementException();
  }
}
