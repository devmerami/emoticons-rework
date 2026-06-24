package maninthehouse.epicfight.animation.types.attack;

import javax.annotation.Nullable;

import maninthehouse.epicfight.animation.types.AnimationProperty;
import maninthehouse.epicfight.capabilities.entity.LivingData;
import maninthehouse.epicfight.physics.Collider;
import maninthehouse.epicfight.utils.game.IExtendedDamageSource;
import net.minecraft.entity.Entity;

public class AADashAnimation extends AttackAnimation {
	private static final float DEFAULT_DASH_DISTANCE_MULTIPLIER = 2.25F;
	private final float dashDistanceMultiplier;

	public AADashAnimation(int id, float convertTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, String index, String path) {
		this(id, convertTime, antic, preDelay, contact, recovery, collider, index, path, DEFAULT_DASH_DISTANCE_MULTIPLIER);
	}

	public AADashAnimation(int id, float convertTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, String index, String path, float dashDistanceMultiplier) {
		super(id, convertTime, antic, preDelay, contact, recovery, false, collider, index, path);
		this.dashDistanceMultiplier = dashDistanceMultiplier;
		this.addProperty(AnimationProperty.DIRECTIONAL, true);
	}
	
	public AADashAnimation(int id, float convertTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, String index,
			String path, boolean noDirectionAttack) {
		this(id, convertTime, antic, preDelay, contact, recovery, collider, index, path, DEFAULT_DASH_DISTANCE_MULTIPLIER, noDirectionAttack);
	}

	public AADashAnimation(int id, float convertTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, String index,
			String path, float dashDistanceMultiplier, boolean noDirectionAttack) {
		super(id, convertTime, antic, preDelay, contact, recovery, false, collider, index, path);
		this.dashDistanceMultiplier = dashDistanceMultiplier;
	}
	
	@Override
	public IExtendedDamageSource getDamageSourceExt(LivingData<?> entitydata, Entity target) {
		IExtendedDamageSource extSource = super.getDamageSourceExt(entitydata, target);
		extSource.setImpact(extSource.getImpact() * 1.4F);
		
		return extSource;
	}

	@Override
	protected float getCoordMultiplier(LivingData<?> entitydata) {
		return this.dashDistanceMultiplier;
	}
}
