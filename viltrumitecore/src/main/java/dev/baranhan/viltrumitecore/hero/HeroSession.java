package dev.baranhan.viltrumitecore.hero;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * Immutable persistent hero-session identity: which hero the player is, the
 * session UUID (regenerated only on an actual hero change) and whether the
 * one-per-session hero totem was already consumed.
 */
public record HeroSession(HeroId heroId, UUID sessionId, boolean totemConsumed) {
   public static final String NBT_KEY = "HeroData";

   public HeroSession consumeTotem() {
      return new HeroSession(this.heroId, this.sessionId, true);
   }

   public void save(CompoundTag nbt) {
      CompoundTag data = new CompoundTag();
      data.putString("Id", this.heroId.key());
      data.putUUID("SessionId", this.sessionId);
      data.putBoolean("TotemConsumed", this.totemConsumed);
      nbt.put(NBT_KEY, data);
   }

   /**
    * Restore the session from NBT. A valid HeroData id always wins; without it
    * the caller's legacy identity is migrated into a fresh session.
    */
   public static HeroSession load(CompoundTag nbt, HeroId legacyIdentity) {
      if (nbt.contains(NBT_KEY)) {
         CompoundTag data = nbt.getCompound(NBT_KEY);
         HeroId id = HeroId.fromKey(data.getString("Id"));
         if (id != null) {
            UUID sessionId = data.hasUUID("SessionId") ? data.getUUID("SessionId") : UUID.randomUUID();
            return new HeroSession(id, sessionId, data.getBoolean("TotemConsumed"));
         }
      }

      return new HeroSession(legacyIdentity, UUID.randomUUID(), false);
   }
}
