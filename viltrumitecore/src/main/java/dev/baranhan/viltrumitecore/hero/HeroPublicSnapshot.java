package dev.baranhan.viltrumitecore.hero;

import java.util.Arrays;

/**
 * Immutable server-produced public hero snapshot. Serialized into one packed
 * synced string so every tracker (and late trackers) sees the same atomic view.
 * Owner-private data (heart carrier ids) travels separately.
 */
public record HeroPublicSnapshot(
   HeroId heroId,
   int actionId,
   int actionElapsed,
   int actionLength,
   int hearts,
   boolean lionActive,
   int lionWindowLeft,
   int lionWindowMax,
   boolean lionOverheat,
   boolean madness,
   int madnessTicksLeft,
   int ritualTicks,
   int controlTargetId,
   int[] cooldowns,
   boolean actionBusy
) {
   public static final int COOLDOWN_COUNT = 6;
   public static final HeroPublicSnapshot EMPTY = new HeroPublicSnapshot(
      HeroId.HUMAN, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1, new int[COOLDOWN_COUNT], false
   );

   public String encode() {
      StringBuilder builder = new StringBuilder(96);
      builder.append(this.heroId.key()).append(';');
      builder.append(this.actionId).append(';');
      builder.append(this.actionElapsed).append(';');
      builder.append(this.actionLength).append(';');
      builder.append(this.hearts).append(';');
      builder.append(this.lionActive ? 1 : 0).append(';');
      builder.append(this.lionWindowLeft).append(';');
      builder.append(this.lionWindowMax).append(';');
      builder.append(this.lionOverheat ? 1 : 0).append(';');
      builder.append(this.madness ? 1 : 0).append(';');
      builder.append(this.madnessTicksLeft).append(';');
      builder.append(this.ritualTicks).append(';');
      builder.append(this.controlTargetId);

      for (int i = 0; i < COOLDOWN_COUNT; i++) {
         builder.append(';').append(i < this.cooldowns.length ? this.cooldowns[i] : 0);
      }

      builder.append(';').append(this.actionBusy ? 1 : 0);
      return builder.toString();
   }

   public static HeroPublicSnapshot decode(String encoded) {
      try {
         String[] parts = encoded.split(";");
         if (parts.length < 14) {
            return EMPTY;
         }

         HeroId heroId = HeroId.fromKey(parts[0]);
         if (heroId == null) {
            return EMPTY;
         }

         int[] cooldowns = new int[COOLDOWN_COUNT];

         for (int i = 0; i < COOLDOWN_COUNT; i++) {
            int index = 13 + i;
            cooldowns[i] = index < parts.length ? Integer.parseInt(parts[index]) : 0;
         }

         return new HeroPublicSnapshot(
            heroId,
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]),
            "1".equals(parts[5]),
            Integer.parseInt(parts[6]),
            Integer.parseInt(parts[7]),
            "1".equals(parts[8]),
            "1".equals(parts[9]),
            Integer.parseInt(parts[10]),
            Integer.parseInt(parts[11]),
            Integer.parseInt(parts[12]),
            cooldowns,
            parts.length > 19 && "1".equals(parts[19])
         );
      } catch (NumberFormatException exception) {
         return EMPTY;
      }
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      } else if (!(other instanceof HeroPublicSnapshot snapshot)) {
         return false;
      } else {
         return this.heroId == snapshot.heroId
            && this.actionId == snapshot.actionId
            && this.actionElapsed == snapshot.actionElapsed
            && this.actionLength == snapshot.actionLength
            && this.hearts == snapshot.hearts
            && this.lionActive == snapshot.lionActive
            && this.lionWindowLeft == snapshot.lionWindowLeft
            && this.lionWindowMax == snapshot.lionWindowMax
            && this.lionOverheat == snapshot.lionOverheat
            && this.madness == snapshot.madness
            && this.madnessTicksLeft == snapshot.madnessTicksLeft
            && this.ritualTicks == snapshot.ritualTicks
            && this.controlTargetId == snapshot.controlTargetId
            && Arrays.equals(this.cooldowns, snapshot.cooldowns)
            && this.actionBusy == snapshot.actionBusy;
      }
   }

   @Override
   public int hashCode() {
      int result = this.heroId.hashCode();
      result = 31 * result + this.actionId;
      result = 31 * result + this.actionElapsed;
      result = 31 * result + this.actionLength;
      result = 31 * result + this.hearts;
      result = 31 * result + Boolean.hashCode(this.lionActive);
      result = 31 * result + this.lionWindowLeft;
      result = 31 * result + this.lionWindowMax;
      result = 31 * result + Boolean.hashCode(this.lionOverheat);
      result = 31 * result + Boolean.hashCode(this.madness);
      result = 31 * result + this.madnessTicksLeft;
      result = 31 * result + this.ritualTicks;
      result = 31 * result + this.controlTargetId;
      result = 31 * result + Arrays.hashCode(this.cooldowns);
      result = 31 * result + Boolean.hashCode(this.actionBusy);
      return result;
   }
}
