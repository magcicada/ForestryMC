/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.core.utils;

import forestry.apiculture.ModuleApiculture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class DamageSourceForestry extends DamageSource {

	public DamageSourceForestry(String ident) {
		super(ident);
		if (ModuleApiculture.hiveDamagePierceArmor) setDamageBypassesArmor();
	}

	@Override
	public ITextComponent getDeathMessage(EntityLivingBase living) {
		EntityLivingBase other = living.getAttackingEntity();
		String ssp = "death." + this.damageType;
		String smp = ssp + ".player";

		if (other != null) {
			return new TextComponentTranslation(smp, living.getDisplayName(), other.getDisplayName());
		} else {
			return new TextComponentTranslation(ssp, living.getDisplayName());
		}
	}

}
