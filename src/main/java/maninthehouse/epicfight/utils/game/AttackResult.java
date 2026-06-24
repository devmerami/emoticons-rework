package maninthehouse.epicfight.utils.game;

import java.util.List;

import net.minecraft.entity.Entity;

public class AttackResult
{
    private final List<Entity> entities;
    private int index;

    public AttackResult(Entity attacker, List<Entity> entities)
    {
        this.entities = entities;
    }

    public Entity getEntity()
    {
        return this.entities.get(this.index);
    }

    public boolean next()
    {
        this.index++;

        return this.index < this.entities.size();
    }
}
