package com.eternal130.mobends_tfcp_compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;

/**
 * Reflection helpers for Forge 1.7.10's {@link EventBus}.
 *
 * <p>Forge 1.7.10 has {@link EventBus#unregister(Object)} but it only accepts the exact target
 * instance that was registered. TFC+ registers its {@code PlayerRenderHandler} as a local variable
 * in {@code ClientProxy}, so we cannot reach it directly. We walk the bus's private
 * {@code listeners} map to find a target whose runtime class matches the one we want to remove,
 * then delegate to {@link EventBus#unregister(Object)} which correctly cleans up the
 * {@code ListenerList} registrations as well.</p>
 *
 * <p>Field layout (verified on Forge 10.13.4.1614 / FML commit 1.7.10):</p>
 * <pre>
 *   EventBus.listeners : ConcurrentHashMap&lt;Object, ArrayList&lt;IEventListener&gt;&gt;
 * </pre>
 * <p>Keys in that map are exactly the objects passed to {@link EventBus#register(Object)}.</p>
 */
public final class EventBusReflection {

    private EventBusReflection() {}

    /**
     * Remove every listener whose target's class is assignable to {@code targetClass}.
     *
     * @return number of targets removed
     */
    @SuppressWarnings("unchecked")
    public static int unregisterByTargetClass(EventBus bus, Class<?> targetClass) {
        if (bus == null || targetClass == null) {
            return 0;
        }

        Map<Object, ArrayList<IEventListener>> listeners = getListenersMap(bus);
        if (listeners == null) {
            return 0;
        }

        int removed = 0;
        // Snapshot keys first; listeners is a ConcurrentHashMap so iteration is weakly consistent,
        // but we want to be explicit about what we're removing.
        ArrayList<Object> targetsToRemove = new ArrayList<Object>();
        for (Object target : listeners.keySet()) {
            if (target != null && targetClass.isAssignableFrom(target.getClass())) {
                targetsToRemove.add(target);
            }
        }

        for (Object target : targetsToRemove) {
            // Forge's unregister walks the listeners list and removes each IEventListener from the
            // relevant Event ListenerList via ListenerList.unregisterAll. Delegating is safer than
            // mirroring that logic ourselves.
            try {
                bus.unregister(target);
                removed++;
            } catch (Throwable t) {
                MoBendsTFCPCompat.LOG.warn(
                    "MoBends TFCP Compat: bus.unregister failed for {}: {}",
                    target,
                    t.toString());
                listeners.remove(target);
                removed++;
            }
        }

        return removed;
    }

    /** Fetch the private {@code EventBus.listeners} field. */
    @SuppressWarnings("unchecked")
    private static Map<Object, ArrayList<IEventListener>> getListenersMap(EventBus bus) {
        // Primary path: direct field name lookup.
        try {
            Field f = EventBus.class.getDeclaredField("listeners");
            f.setAccessible(true);
            Object value = f.get(bus);
            if (value instanceof Map) {
                return (Map<Object, ArrayList<IEventListener>>) value;
            }
        } catch (NoSuchFieldException e) {
            // Fall through to heuristic.
        } catch (Throwable t) {
            MoBendsTFCPCompat.LOG.warn(
                "MoBends TFCP Compat: could not read EventBus.listeners by name: {}", t.toString());
        }

        // Heuristic: listeners is the only ConcurrentHashMap<Object, ArrayList<IEventListener>>
        // field on EventBus. This is robust against private-field renames in FML forks.
        try {
            for (Field f : EventBus.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(bus);
                if (v instanceof ConcurrentHashMap) {
                    return (Map<Object, ArrayList<IEventListener>>) v;
                }
            }
        } catch (Throwable t) {
            MoBendsTFCPCompat.LOG.warn(
                "MoBends TFCP Compat: heuristic listeners lookup failed: {}", t.toString());
        }
        return null;
    }
}
