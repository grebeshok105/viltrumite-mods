package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Player.class},
   priority = 1300
)
public abstract class PlayerAbilityMixin implements ViltrumiteAbilityUser {
   @Unique
   private static final EntityDataAccessor<Integer> ACTIVE_PAGE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
   @Unique
   private static final EntityDataAccessor<String> S_0 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_1 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_2 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_3 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_4 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_5 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_6 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_7 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_8 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_9 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_10 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_11 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_12 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_13 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_14 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_15 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_16 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> S_17 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final String OFFERED_KEY = "ViltrumiteOfferedAbilities";
   @Unique
   private java.util.Set<String> viltrumitecore$offeredAbilities;

   @Inject(
      method = {"defineSynchedData"},
      at = {@At("TAIL")}
   )
   protected void onInitAbilityTracker(CallbackInfo ci) {
      Player player = (Player)(Object)this;
      player.getEntityData().define(ACTIVE_PAGE, 0);
      player.getEntityData().define(S_0, "viltrumite:punch");
      player.getEntityData().define(S_1, "viltrumite:dash");
      player.getEntityData().define(S_2, "viltrumite:grab");
      player.getEntityData().define(S_3, "viltrumite:lock");
      player.getEntityData().define(S_4, "viltrumite:block");
      player.getEntityData().define(S_5, "viltrumite:speed");
      player.getEntityData().define(S_6, "");
      player.getEntityData().define(S_7, "");
      player.getEntityData().define(S_8, "");
      player.getEntityData().define(S_9, "");
      player.getEntityData().define(S_10, "");
      player.getEntityData().define(S_11, "");
      player.getEntityData().define(S_12, "");
      player.getEntityData().define(S_13, "");
      player.getEntityData().define(S_14, "");
      player.getEntityData().define(S_15, "");
      player.getEntityData().define(S_16, "");
      player.getEntityData().define(S_17, "");
   }

   @Override
   public int getActivePage() {
      return (Integer)((Player)(Object)this).getEntityData().get(ACTIVE_PAGE);
   }

   @Override
   public void setActivePage(int page) {
      ((Player)(Object)this).getEntityData().set(ACTIVE_PAGE, page);
   }

   @Override
   public String getAbilityInSlot(int slotIndex) {
      Player player = (Player)(Object)this;

      return switch (slotIndex) {
         case 0 -> (String)player.getEntityData().get(S_0);
         case 1 -> (String)player.getEntityData().get(S_1);
         case 2 -> (String)player.getEntityData().get(S_2);
         case 3 -> (String)player.getEntityData().get(S_3);
         case 4 -> (String)player.getEntityData().get(S_4);
         case 5 -> (String)player.getEntityData().get(S_5);
         case 6 -> (String)player.getEntityData().get(S_6);
         case 7 -> (String)player.getEntityData().get(S_7);
         case 8 -> (String)player.getEntityData().get(S_8);
         case 9 -> (String)player.getEntityData().get(S_9);
         case 10 -> (String)player.getEntityData().get(S_10);
         case 11 -> (String)player.getEntityData().get(S_11);
         case 12 -> (String)player.getEntityData().get(S_12);
         case 13 -> (String)player.getEntityData().get(S_13);
         case 14 -> (String)player.getEntityData().get(S_14);
         case 15 -> (String)player.getEntityData().get(S_15);
         case 16 -> (String)player.getEntityData().get(S_16);
         case 17 -> (String)player.getEntityData().get(S_17);
         default -> "";
      };
   }

   @Override
   public void setAbilityInSlot(int slotIndex, String abilityId) {
      Player player = (Player)(Object)this;
      String safeId = abilityId == null ? "" : abilityId;
      switch (slotIndex) {
         case 0:
            player.getEntityData().set(S_0, safeId);
            break;
         case 1:
            player.getEntityData().set(S_1, safeId);
            break;
         case 2:
            player.getEntityData().set(S_2, safeId);
            break;
         case 3:
            player.getEntityData().set(S_3, safeId);
            break;
         case 4:
            player.getEntityData().set(S_4, safeId);
            break;
         case 5:
            player.getEntityData().set(S_5, safeId);
            break;
         case 6:
            player.getEntityData().set(S_6, safeId);
            break;
         case 7:
            player.getEntityData().set(S_7, safeId);
            break;
         case 8:
            player.getEntityData().set(S_8, safeId);
            break;
         case 9:
            player.getEntityData().set(S_9, safeId);
            break;
         case 10:
            player.getEntityData().set(S_10, safeId);
            break;
         case 11:
            player.getEntityData().set(S_11, safeId);
            break;
         case 12:
            player.getEntityData().set(S_12, safeId);
            break;
         case 13:
            player.getEntityData().set(S_13, safeId);
            break;
         case 14:
            player.getEntityData().set(S_14, safeId);
            break;
         case 15:
            player.getEntityData().set(S_15, safeId);
            break;
         case 16:
            player.getEntityData().set(S_16, safeId);
            break;
         case 17:
            player.getEntityData().set(S_17, safeId);
      }
   }

   @Override
   public java.util.Set<String> getOfferedAbilities() {
      return this.viltrumitecore$offeredAbilities;
   }

   @Override
   public void setOfferedAbilities(java.util.Set<String> offered) {
      this.viltrumitecore$offeredAbilities = offered == null ? null : new java.util.LinkedHashSet<>(offered);
   }

   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void onWriteAbilityNbt(CompoundTag nbt, CallbackInfo ci) {
      nbt.putInt("ViltrumiteActivePage", this.getActivePage());

      for (int i = 0; i < 18; i++) {
         nbt.putString("ViltrumiteAbilitySlot_" + i, this.getAbilityInSlot(i));
      }

      if (this.viltrumitecore$offeredAbilities != null) {
         net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
         for (String id : this.viltrumitecore$offeredAbilities) {
            list.add(net.minecraft.nbt.StringTag.valueOf(id));
         }

         nbt.put(OFFERED_KEY, list);
      }
   }

   @Inject(
      method = {"readAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void onReadAbilityNbt(CompoundTag nbt, CallbackInfo ci) {
      if (nbt.contains("ViltrumiteActivePage")) {
         this.setActivePage(nbt.getInt("ViltrumiteActivePage"));
      }

      for (int i = 0; i < 18; i++) {
         String key = "ViltrumiteAbilitySlot_" + i;
         if (nbt.contains(key)) {
            this.setAbilityInSlot(i, nbt.getString(key));
         }
      }

      if (nbt.contains(OFFERED_KEY, net.minecraft.nbt.Tag.TAG_LIST)) {
         java.util.Set<String> offered = new java.util.LinkedHashSet<>();
         net.minecraft.nbt.ListTag list = nbt.getList(OFFERED_KEY, net.minecraft.nbt.Tag.TAG_STRING);
         for (int i = 0; i < list.size(); i++) {
            offered.add(list.getString(i));
         }

         this.viltrumitecore$offeredAbilities = offered;
      }
   }
}
