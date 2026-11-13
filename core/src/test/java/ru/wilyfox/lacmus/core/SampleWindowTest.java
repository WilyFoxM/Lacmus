package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SampleWindowTest {
  @Test
  void emptyWindowIsUnknown() {
    SampleWindow window = new SampleWindow(4);
    assertTrue(Double.isNaN(window.last()));
    assertTrue(Double.isNaN(window.mean()));
    assertTrue(Double.isNaN(window.meanAbsDelta()));
  }

  @Test
  void oneSampleHasNoJitter() {
    SampleWindow window = new SampleWindow(4);
    window.add(40);
    assertEquals(40, window.last());
    assertEquals(40, window.mean());
    assertTrue(Double.isNaN(window.meanAbsDelta()));
  }

  @Test
  void jitterIsMeanAbsoluteDifferenceOfNeighbours() {
    SampleWindow window = new SampleWindow(8);
    for (double sample : new double[] {40, 44, 38, 38}) {
      window.add(sample);
    }
    // |44-40| + |38-44| + |38-38| = 10 over 3 steps.
    assertEquals(10.0 / 3, window.meanAbsDelta(), 1e-12);
    assertEquals(40, window.mean(), 1e-12);
  }

  @Test
  void overwritesOldestWhenFull() {
    SampleWindow window = new SampleWindow(3);
    for (int i = 1; i <= 5; i++) {
      window.add(i);
    }
    assertEquals(3, window.size());
    assertEquals(3, window.get(0));
    assertEquals(5, window.get(2));
    assertEquals(5, window.last());
    assertEquals(1, window.meanAbsDelta(), 1e-12);
  }

  @Test
  void clearForgetsSamples() {
    SampleWindow window = new SampleWindow(3);
    window.add(1);
    window.add(2);
    window.clear();
    assertEquals(0, window.size());
    window.add(7);
    assertEquals(7, window.last());
  }
}
