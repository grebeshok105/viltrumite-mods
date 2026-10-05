package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
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
   priority = 1100
)
public abstract class PlayerCosmeticsMixin implements ViltrumiteCosmeticsPlayer {
   @Unique
   private static final EntityDataAccessor<String> VILTRUMITE_SKIN = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> VILTRUMITE_CAPE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
   @Unique
   private static final EntityDataAccessor<String> VILTRUMITE_MODEL = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);

   @Inject(
      method = {"defineSynchedData"},
      at = {@At("TAIL")}
   )
   protected void onInitDataTracker(CallbackInfo ci) {
      Player player = (Player)this;
      player.getEntityData().define(VILTRUMITE_SKIN, "off");
      player.getEntityData().define(VILTRUMITE_CAPE, "off");
      player.getEntityData().define(VILTRUMITE_MODEL, "default");
   }

   @Override
   public String getViltrumiteSkin() {
      return (String)((Player)this).getEntityData().get(VILTRUMITE_SKIN);
   }

   @Override
   public void setViltrumiteSkin(String skinName) {
      ((Player)this).getEntityData().set(VILTRUMITE_SKIN, skinName);
   }

   @Override
   public String getViltrumiteCape() {
      return (String)((Player)this).getEntityData().get(VILTRUMITE_CAPE);
   }

   @Override
   public void setViltrumiteCape(String capeName) {
      ((Player)this).getEntityData().set(VILTRUMITE_CAPE, capeName);
   }

   @Override
   public String getViltrumiteModel() {
      return (String)((Player)this).getEntityData().get(VILTRUMITE_MODEL);
   }

   @Override
   public void setViltrumiteModel(String modelName) {
      ((Player)this).getEntityData().set(VILTRUMITE_MODEL, modelName);
   }

   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void onWriteCustomDataToNbt(CompoundTag nbt, CallbackInfo ci) {
      nbt.putString("ViltrumiteSkin", this.getViltrumiteSkin());
      nbt.putString("ViltrumiteCape", this.getViltrumiteCape());
      nbt.putString("ViltrumiteModel", this.getViltrumiteModel());
   }

   @Inject(
      method = {"readAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void onReadCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
      if (nbt.contains("ViltrumiteSkin")) {
         this.setViltrumiteSkin(nbt.getString("ViltrumiteSkin"));
      }

      if (nbt.contains("ViltrumiteCape")) {
         this.setViltrumiteCape(nbt.getString("ViltrumiteCape"));
      }

      if (nbt.contains("ViltrumiteModel")) {
         this.setViltrumiteModel(nbt.getString("ViltrumiteModel"));
      }
   }
}
