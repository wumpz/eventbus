package org.bushe.swing.event.annotation;

import java.util.Collection;
import java.util.List;

/** Test class for class-based subscriptions */
public class WeakClassAnnotatedEventSubscriber {
   static int timesColorChanged = 0;
   static String lastCall = null;
   static int timesCalled = 0;

   public static int getTimesCalled() {
      return timesCalled;
   }

   public static void setTimesCalled(int times) {
      timesCalled = times;
   }

   @EventSubscriber(eventClass = List.class, referenceStrength = ReferenceStrength.WEAK)
   public void doList(Collection collection) {
      timesCalled++;
   }
}
