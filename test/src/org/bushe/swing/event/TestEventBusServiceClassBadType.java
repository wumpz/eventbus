package org.bushe.swing.event;

import junit.framework.TestCase;

/** The DefaultEventService is NOT Swing-safe!  But it's easier to test... */
public class TestEventBusServiceClassBadType extends TestCase {

   public TestEventBusServiceClassBadType(String name) {
      super(name);
   }

   protected void setUp() throws Exception {
      System.setProperty(EventServiceLocator.SWING_EVENT_SERVICE_CLASS, EventServiceLocator.class.getName());
   }

   protected void tearDown() throws Exception {
   }

   public void testConfigurableEventService() {
      try {
         EventBus.subscribe("foo", new EventTopicSubscriber() {
            public void onEvent(String topic, Object data) {
            }
         });
         fail("Expected runtime exception since the plugged in EventService is a bad one.");
      } catch (Throwable ex) {
         System.out.println("Got ex");
      }
   }
}
