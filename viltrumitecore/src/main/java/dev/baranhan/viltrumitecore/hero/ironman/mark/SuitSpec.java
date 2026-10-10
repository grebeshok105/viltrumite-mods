package dev.baranhan.viltrumitecore.hero.ironman.mark;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import javax.annotation.Nullable;

/**
 * The numbers every Iron Man system reads for the suit on Tony right now:
 * the nano defaults or the worn mark ({@link MarkSpec}) with its live
 * modifiers (Mark 42 lost parts). Systems never read nano constants directly
 * for these values.
 */
public record SuitSpec(
   @Nullable MarkId mark,
   double armor,
   double toughness,
   double knockbackRes,
   float flightSpeedMul,
   float sonicDrainMul,
   float weaponMul,
   float shieldMul,
   int missileMarks,
   float missileMul,
   int unibeamCharge,
   float recoilMul,
   boolean silentFlight,
   boolean stealth,
   boolean unibeamOnline,
   boolean helmetForcedOpen
) {
   public static final SuitSpec NANO = new SuitSpec(null, IronManRules.NANO_ARMOR, IronManRules.NANO_TOUGHNESS, IronManRules.NANO_KNOCKBACK_RES,
      1.0F, 1.0F, 1.0F, 1.0F, IronManRules.MISSILE_MARKS, 1.0F, IronManRules.UNIBEAM_CHARGE, 1.0F, false, false, true, false);

   /** Spec of a worn mark; {@code lostMask} = Mark 42 lost parts (0 for the others). */
   public static SuitSpec mark(MarkId id, int lostMask) {
      MarkSpec spec = MarkSpec.of(id);
      boolean modular = id == MarkId.MARK_42;
      float weapon = spec.weaponMul() * (modular ? Mark42Parts.weaponFactor(lostMask) : 1.0F);
      float flight = spec.flightSpeedMul() * (modular ? Mark42Parts.flightFactor(lostMask) : 1.0F);
      boolean unibeam = !modular || !Mark42Parts.chestLost(lostMask);
      boolean helmetOpen = modular && Mark42Parts.helmetLost(lostMask);
      return new SuitSpec(id, spec.armor(), spec.toughness(), spec.knockbackRes(), flight, spec.sonicDrainMul(), weapon, spec.shieldMul(),
         spec.missileMarks(), spec.missileMul(), spec.unibeamCharge(), spec.recoilMul(), spec.silentFlight(), spec.stealth(), unibeam, helmetOpen);
   }

   public boolean isMark() {
      return this.mark != null;
   }
}
