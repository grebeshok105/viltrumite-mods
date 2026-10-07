package dev.baranhan.viltrumitecore.hero.fx;

import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.HeroFxS2CPacket;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Server API for one-shot hero world FX. Every player who tracks the source
 * (and the source itself) receives the packet; the client side is
 * {@code client.render.vfx.HeroImpactFx}. Gameplay never waits for FX: resolve
 * damage and blocks first, then call one of these.
 */
public final class HeroFx {
   private HeroFx() {
   }

   /** Visible flying shards from origin to each end point (flat xyz triples, max 64), with an optional pass-by whiz. */
   public static void shards(ServerPlayer source, Vec3 origin, float power, @Nullable BlockState material, float[] ends, @Nullable SoundEvent whiz) {
      send(source, HeroFxS2CPacket.SHARDS, origin, power, material, ends, whiz);
   }

   /** Expanding ground ring + dust + camera shake (landings). Power 0..1+. */
   public static void shockwave(ServerPlayer source, Vec3 origin, float power, @Nullable BlockState material, float radius) {
      send(source, HeroFxS2CPacket.SHOCKWAVE, origin, power, material, new float[]{radius, 0.0F, 0.0F}, null);
   }

   /** Heavy impact: double ring, white flash, lots of dust, strong shake. */
   public static void slam(ServerPlayer source, Vec3 origin, @Nullable BlockState material, float radius) {
      send(source, HeroFxS2CPacket.SLAM, origin, 1.0F, material, new float[]{radius, 0.0F, 0.0F}, null);
   }

   /** A thin bright blade streak from origin to end, with an optional pass-by whiz. */
   public static void blade(ServerPlayer source, Vec3 origin, Vec3 end, @Nullable SoundEvent whiz) {
      send(source, HeroFxS2CPacket.BLADE, origin, 1.0F, null, new float[]{(float)end.x, (float)end.y, (float)end.z}, whiz);
   }

   /** Ground puff ring under a launch (super jump, take-off). */
   public static void launch(ServerPlayer source, Vec3 feet, float power, @Nullable BlockState material) {
      send(source, HeroFxS2CPacket.LAUNCH, feet, power, material, null, null);
   }

   /** Short white flash + medium shake (a hit that sends someone flying). */
   public static void flash(ServerPlayer source, Vec3 at) {
      send(source, HeroFxS2CPacket.FLASH, at, 1.0F, null, null, null);
   }

   private static void send(ServerPlayer source, byte kind, Vec3 origin, float power, @Nullable BlockState material, @Nullable float[] points, @Nullable SoundEvent sound) {
      int stateId = material == null ? 0 : Block.getId(material);
      int soundId = sound == null ? -1 : BuiltInRegistries.SOUND_EVENT.getId(sound);
      CoreMessages.sendToTrackingAndSelf(new HeroFxS2CPacket(kind, origin, power, stateId, points, soundId), source);
   }
}
