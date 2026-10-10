package dev.baranhan.viltrumitecore.hero.ironman.mark;

import java.util.EnumMap;
import java.util.Map;

/**
 * All numbers of one mark (spec §13, plan stage 4 Task 1). Starting table,
 * tuned after the in-game check. Iron Man stays weaker than Homelander head-on
 * (spec §18): durability is the only extra life, armor only slows its loss.
 *
 * @param durability full durability (all damage goes here while it lasts)
 * @param armor armor attribute while worn; also slows durability loss ({@link MarkDamage})
 * @param toughness armor toughness attribute while worn
 * @param knockbackRes knockback resistance attribute while worn
 * @param flightSpeedMul flight speed factor (legacy flight, {@code HeroDefinition.flightSpeedScale})
 * @param sonicDrainMul factor on the sonic energy drain
 * @param weaponMul factor on all outgoing damage while worn
 * @param shieldMul shield strength: energy per absorbed hit is divided by it
 * @param missileMarks max missile marks
 * @param missileMul missile damage factor
 * @param unibeamCharge Unibeam charge ticks
 * @param recoilMul factor on the repulsor recoil and self push
 * @param silentFlight no thruster sound and flames (Mark 15)
 * @param stealth hidden from Homelander focus and the Iron Man scan (Mark 15)
 * @param parts parts that fly in from Veronica (legs → arms → chest → back → helmet)
 */
public record MarkSpec(
   MarkId id,
   float durability,
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
   int parts
) {
   private static final Map<MarkId, MarkSpec> TABLE = new EnumMap<>(MarkId.class);

   static {
      put(new MarkSpec(MarkId.MARK_7, 120.0F, 16.0, 4.0, 0.3, 1.0F, 1.0F, 1.0F, 1.0F, 4, 1.0F, 20, 1.0F, false, false, 9));
      put(new MarkSpec(MarkId.MARK_42, 100.0F, 15.0, 3.0, 0.3, 1.0F, 1.0F, 1.0F, 1.0F, 4, 1.0F, 20, 1.0F, false, false, 14));
      put(new MarkSpec(MarkId.MARK_15, 80.0F, 11.0, 2.0, 0.2, 1.0F, 1.0F, 1.0F, 1.0F, 4, 1.0F, 20, 1.0F, true, true, 7));
      put(new MarkSpec(MarkId.MARK_39, 100.0F, 14.0, 3.0, 0.25, 1.35F, 0.6F, 0.85F, 1.0F, 4, 1.0F, 20, 1.0F, false, false, 9));
      put(new MarkSpec(MarkId.MARK_17, 110.0F, 15.0, 3.0, 0.3, 1.0F, 1.0F, 1.0F, 1.0F, 4, 1.0F, 10, 1.0F, false, false, 9));
      put(new MarkSpec(MarkId.WAR_MACHINE_MK2, 150.0F, 18.0, 5.0, 0.5, 0.8F, 1.5F, 1.0F, 1.0F, 8, 1.4F, 20, 0.8F, false, false, 9));
      put(new MarkSpec(MarkId.IRON_HEART_MK3, 180.0F, 20.0, 6.0, 0.9, 0.8F, 1.0F, 1.0F, 1.5F, 4, 1.0F, 20, 0.2F, false, false, 9));
   }

   private static void put(MarkSpec spec) {
      TABLE.put(spec.id(), spec);
   }

   public static MarkSpec of(MarkId id) {
      return TABLE.get(id);
   }
}
