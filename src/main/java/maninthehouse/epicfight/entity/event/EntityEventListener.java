package maninthehouse.epicfight.entity.event;

import java.util.UUID;
import java.util.function.Function;

public class EntityEventListener
{
    public static final EntityEventListener EMPTY = new EntityEventListener();

    public void activateEvents(Event event)
    {}

    public enum Event
    {
        ON_ACTION_SERVER_EVENT
    }

    public static <T> Object makeEvent(UUID id, Function<T, Boolean> callback)
    {
        return null;
    }
}
