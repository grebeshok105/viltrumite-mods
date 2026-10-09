package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSpec;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/**
 * The mark part of one Iron Man snapshot, decoded for the client visuals.
 * Pure: every decision of the mark skin, plates and flights reads this record.
 */
public record MarkState(@Nullable MarkId mark, boolean worn, int equipPhase, int parts, float durability, float maxDurability,
   boolean helmetClosed, boolean equipping, int elapsed, int length, @Nullable Vec3 launch) {

   public static MarkState of(HeroPublicSnapshot snapshot) {
      int flags = snapshot.heroFlags();
      MarkId mark = IronManVariant.mark(snapshot.variant());
      int phase = IronManFlags.get(flags, IronManFlags.Field.EQUIP_PHASE);
      boolean equipping = phase == IronManFlags.EQUIP_EQUIPPING && snapshot.actionId() == HeroAction.SUIT.ordinal() && snapshot.actionLength() > 0;
      float max = mark == null ? 0.0F : MarkSpec.of(mark).durability();
      return new MarkState(mark, IronManFlags.is(flags, IronManFlags.Field.SUIT_WORN), phase, IronManVariant.parts(snapshot.variant()),
         snapshot.resource2() / 10.0F, max, IronManFlags.is(flags, IronManFlags.Field.HELMET_CLOSED), equipping,
         snapshot.actionElapsed(), snapshot.actionLength(), snapshot.actionTarget());
   }

   /** A mark is on the body or is being put on, taken off or partly on. */
   public boolean markOn() {
      return this.mark != null && (this.worn || this.equipPhase != IronManFlags.EQUIP_NONE);
   }

   /** Whole mark worn: the mark skin covers the body and no plate is drawn. */
   public boolean full() {
      return this.mark != null && this.worn && this.equipPhase == IronManFlags.EQUIP_NONE && this.parts == SuitPart.fullMask(this.mark);
   }

   public boolean partPresent(int index) {
      return (this.parts >> index & 1) != 0;
   }

   public boolean delivery() {
      return this.length == EquipTimeline.DELIVERY;
   }

   /** Durability 0..1 of the worn mark. */
   public float durabilityFraction() {
      return this.maxDurability <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, this.durability / this.maxDurability));
   }
}
