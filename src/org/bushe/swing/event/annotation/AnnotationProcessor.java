package org.bushe.swing.event.annotation;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.EventServiceExistsException;

/**
 * Enhances classes that use EventService Annotations.
 * <p>
 * This class makes the EventService annotations "come alive."  This can be used in code like so:
 * <pre>
 * public class MyAppController {
 *   public MyAppController {
 *       AnnotationProcessor.process(this);//this line can be avoided with a compile-time tool or an Aspect
 *   }
 *   @EventSubscriber
 *   public void onAppStartingEvent(AppStartingEvent appStartingEvent) {
 *      //do something
 *   }
 *   @EventSubscriber
 *   public void onAppClosingEvent(AppClosingEvent appClosingEvent) {
 *      //do something
 *   }
 * }
 * </pre>
 * <p>
 * This class can be leveraged in outside of source code in other ways in which Annoations are used:
 * <ul>
 * <li>In an Aspect-Oriented tool
 * <li>In a Swing Framework classloader that wants to load and understand events.
 * <li>In other Inversion of Control containers, such as Spring or PicoContainer.
 * <li>In the apt tool, though this does not generate code.
 * <li>In a Annotation Processing Tool plugin, when it becomes available.
 * </ul>
 * Support for these other methods are not yet implemented.
 */
public class AnnotationProcessor {
   public static void process(Object obj) {
      if (obj == null) {
         return;
      }
      Class cl = obj.getClass();
      Method[] methods = cl.getMethods();
      for (int i = 0; i < methods.length; i++) {
         Method method = methods[i];
         EventSubscriber classAnnotation = method.getAnnotation(EventSubscriber.class);
         if (classAnnotation != null) {
            process(classAnnotation, obj, method);
         }
         EventTopicSubscriber topicAnnotation = (EventTopicSubscriber) method.getAnnotation(EventTopicSubscriber.class);
         if (topicAnnotation != null) {
            process(topicAnnotation, obj, method);
         }
         EventTopicPatternSubscriber topicPatternAnnotation = (EventTopicPatternSubscriber) method.getAnnotation(EventTopicPatternSubscriber.class);
         if (topicPatternAnnotation != null) {
            process(topicPatternAnnotation, obj, method);
         }
      }
   }

   private static void process(EventTopicPatternSubscriber topicPatternAnnotation, Object obj, Method method) {
      //Check args
      String topicPattern = topicPatternAnnotation.topicPattern();
      if (topicPattern == null) {
         throw new IllegalArgumentException("Topic pattern cannot be null for EventTopicPatternSubscriber annotation");
      }

      //Get event service
      Class<? extends EventService> eventServiceClass = topicPatternAnnotation.autoCreateEventServiceClass();
      String eventServiceName = topicPatternAnnotation.eventServiceName();
      EventService eventService = getEventServiceFromAnnotation(eventServiceName, eventServiceClass);

      //Create proxy and subscribe
      Pattern pattern = Pattern.compile(topicPattern);
      ProxyTopicPatternSubscriber subscriber = new ProxyTopicPatternSubscriber(obj, method, topicPatternAnnotation.referenceStrength(),
              eventService, topicPattern, pattern);

      if (topicPatternAnnotation.referenceStrength() == ReferenceStrength.WEAK) {
//         eventService.subscribe(pattern, subscriber);
         eventService.subscribeStrongly(pattern, subscriber);
      } else {
         eventService.subscribeStrongly(pattern, subscriber);
      }
   }

   private static void process(EventTopicSubscriber topicAnnotation, Object obj, Method method) {
      //Check args
      String topic = topicAnnotation.topic();
      if (topic == null) {
         throw new IllegalArgumentException("Topic cannot be null for EventTopicSubscriber annotation");
      }

      //Get event service
      Class<? extends EventService> eventServiceClass = topicAnnotation.autoCreateEventServiceClass();
      String eventServiceName = topicAnnotation.eventServiceName();
      EventService eventService = getEventServiceFromAnnotation(eventServiceName, eventServiceClass);

      //Create proxy and subscribe
      ProxyTopicSubscriber subscriber = new ProxyTopicSubscriber(obj, method, topicAnnotation.referenceStrength(), eventService,  topic);

      if (topicAnnotation.referenceStrength() == ReferenceStrength.WEAK) {
//         eventService.subscribe(topic, subscriber);
         eventService.subscribeStrongly(topic, subscriber);
      } else {
         eventService.subscribeStrongly(topic, subscriber);
      }
   }

   private static void process(EventSubscriber annotation, Object obj, Method method) {
      //Check args
      Class eventClass = annotation.eventClass();
      if (eventClass == null) {
         throw new IllegalArgumentException("Event class cannot be null for EventSubscriber annotation");
      } else if (UseTheClassOfTheAnnotatedMethodsParameter.class.equals(eventClass)) {
         Class[] params = method.getParameterTypes();
         if (params.length < 1) {
            throw new RuntimeException("Expected annotated method to have one parameter.");
         } else {
            eventClass = params[0];
         }
      }

      //Get event service
      Class<? extends EventService> eventServiceClass = annotation.autoCreateEventServiceClass();
      String eventServiceName = annotation.eventServiceName();
      EventService eventService = getEventServiceFromAnnotation(eventServiceName, eventServiceClass);

      //Create proxy and subscribe
      ProxySubscriber subscriber = new ProxySubscriber(obj, method, annotation.referenceStrength(), eventService,  eventClass);
      if (annotation.exact()) {
         if (annotation.referenceStrength() == ReferenceStrength.WEAK) {
//            eventService.subscribeExactly(eventClass, subscriber);
            eventService.subscribeExactlyStrongly(eventClass, subscriber);
         } else {
            eventService.subscribeExactlyStrongly(eventClass, subscriber);
         }
      } else {
         if (annotation.referenceStrength() == ReferenceStrength.WEAK) {
//            eventService.subscribe(eventClass, subscriber);
            eventService.subscribeStrongly(eventClass, subscriber);
         } else {
            eventService.subscribeStrongly(eventClass, subscriber);
         }
      }
   }

   private static EventService getEventServiceFromAnnotation(String eventServiceName,
           Class<? extends EventService> eventServiceClass) {
      EventService eventService = EventServiceLocator.getEventService(eventServiceName);
      if (eventService == null) {
         if (EventServiceLocator.SERVICE_NAME_EVENT_BUS.equals(eventServiceName)) {
            //This may be the first time the EventBus is accessed.
            eventService = EventServiceLocator.getSwingEventService();
         } else {
            //The event service does not yet exist, create it
            try {
               eventService = eventServiceClass.newInstance();
            } catch (InstantiationException e) {
               throw new RuntimeException("Could not instance of create EventService class "+eventServiceClass, e);
            } catch (IllegalAccessException e) {
               throw new RuntimeException("Could not instance of create EventService class "+eventServiceClass, e);
            }
            try {
               EventServiceLocator.setEventService(eventServiceName, eventService);
            } catch (EventServiceExistsException e) {
               //ignore it, it's OK
               eventService = EventServiceLocator.getEventService(eventServiceName);
            }
         }
      }
      return eventService;
   }
}
