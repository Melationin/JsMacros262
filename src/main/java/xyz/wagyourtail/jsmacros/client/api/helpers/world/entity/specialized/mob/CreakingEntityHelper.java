package xyz.wagyourtail.jsmacros.client.api.helpers.world.entity.specialized.mob;

import net.minecraft.entity.mob.CreakingEntity;
import xyz.wagyourtail.jsmacros.client.api.helpers.world.BlockPosHelper;
import xyz.wagyourtail.jsmacros.client.api.helpers.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class CreakingEntityHelper extends EntityHelper<CreakingEntity> {

    public CreakingEntityHelper(CreakingEntity e) {
        super(e);
    }

    public boolean isActive() {
        return base.isActive();
    }

    public boolean isCrumbling() {
        return base.isCrumbling();
    }

    public BlockPosHelper getHomePos() {
        return new BlockPosHelper(base.getHomePos());
    }

    public boolean isUnrooted() {
        return base.isUnrooted();
    }

}
