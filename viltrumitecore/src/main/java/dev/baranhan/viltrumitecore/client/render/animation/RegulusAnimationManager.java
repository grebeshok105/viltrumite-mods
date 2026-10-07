package dev.baranhan.viltrumitecore.client.render.animation;

import dev.baranhan.viltrumitecore.client.regulus.RegulusActionClock;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoser;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.item.EvangeliumItem;
import java.util.EnumMap;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Regulus animation state: one {@link RegulusActionClock} per player (synced
 * cast ticks, ticked on ClientTickEvent) and GrabAnimationManager-style
 * exponential weights per layer, separately for the third-person model and
 * each first-person hand so every view advances its weight once per frame.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class RegulusAnimationManager {
   public enum Layer {
      LION_STANCE(10.0F),
      LION_OVERHEAT(8.0F),
      MANIA_CHANNEL(12.0F),
      RITUAL_MAIN_HAND(9.0F),
      RITUAL_OFF_HAND(9.0F),
      MADNESS(6.0F),
      CAST_LIONS_HEART(18.0F),
      CAST_DEBRIS_KICK(18.0F),
      CAST_MANIA(18.0F),
      CAST_GREEDS_EMBRACE(18.0F),
      CAST_COUNTER(18.0F);

      final float rate;

      Layer(float rate) {
         this.rate = rate;
      }
   }

   private enum View {
      MODEL,
      BODY,
      FIRST_PERSON_MAIN,
      FIRST_PERSON_OFF
   }

   private static final WeakHashMap<LivingEntity, RegulusActionClock> CLOCKS = new WeakHashMap<>();
   private static final EnumMap<View, WeakHashMap<LivingEntity, EnumMap<Layer, WeightState>>> WEIGHTS = new EnumMap<>(View.class);
   private static ClientLevel lastLevel;

   static {
      for (View view : View.values()) {
         WEIGHTS.put(view, new WeakHashMap<>());
      }
   }

   private RegulusAnimationManager() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }
      ClientLevel level = Minecraft.getInstance().level;
      if (level != lastLevel) {
         lastLevel = level;
         CLOCKS.clear();
         WEIGHTS.values().forEach(WeakHashMap::clear);
      }
      if (level == null) {
         return;
      }
      for (Player player : level.players()) {
         HeroPublicSnapshot snapshot = RegulusPoser.regulus(player);
         if (snapshot == null) {
            if (CLOCKS.remove(player) != null) {
               WEIGHTS.values().forEach(map -> map.remove(player));
            }
            continue;
         }
         RegulusActionClock clock = CLOCKS.computeIfAbsent(player, k -> new RegulusActionClock());
         HeroAction action = HeroAction.byId(snapshot.actionId());
         if (action == null) {
            HeroAction last = HeroAction.byId(clock.actionId());
            RegulusPoseTiming.Timing timing = last == null ? null : RegulusPoseTiming.timing(last);
            clock.tick(-1, 0, timing == null ? 0 : timing.length(), timing == null ? 0 : timing.visualLength());
         } else {
            RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(action);
            clock.tick(action.ordinal(), snapshot.actionElapsed(), timing.length(), timing.visualLength());
         }
      }
   }

   /** Smoothed third-person weight of a continuous layer. */
   public static float weight(LivingEntity entity, Layer layer, boolean active) {
      return advance(View.MODEL, entity, layer, active);
   }

   public static float firstPersonWeight(LivingEntity entity, Layer layer, boolean active, boolean mainHand) {
      return advance(mainHand ? View.FIRST_PERSON_MAIN : View.FIRST_PERSON_OFF, entity, layer, active);
   }

   /** Third-person weight of a cast layer; 0 once the clock moved to another cast. */
   public static float castWeight(LivingEntity entity, HeroAction action) {
      return castWeight(View.MODEL, entity, action);
   }

   /** Weight for the whole-body (setupRotations) layer of a cast. */
   public static float bodyCastWeight(LivingEntity entity, HeroAction action) {
      return castWeight(View.BODY, entity, action);
   }

   public static float firstPersonCastWeight(LivingEntity entity, HeroAction action, boolean mainHand) {
      return castWeight(mainHand ? View.FIRST_PERSON_MAIN : View.FIRST_PERSON_OFF, entity, action);
   }

   /** Cast time in ticks (synced tick + partialTick), frozen after a cancel. */
   public static float castTime(LivingEntity entity, float partialTick) {
      RegulusActionClock clock = CLOCKS.get(entity);
      return clock == null ? 0.0F : clock.time(partialTick);
   }

   public static boolean castPlaying(LivingEntity entity, HeroAction action) {
      RegulusActionClock clock = CLOCKS.get(entity);
      return clock != null && clock.isPlaying(action.ordinal());
   }

   /** Hand holding the Evangelium during the ritual (main hand if both or neither). */
   public static InteractionHand bookHand(LivingEntity entity) {
      if (entity.isUsingItem() && entity.getItemInHand(entity.getUsedItemHand()).getItem() instanceof EvangeliumItem) {
         return entity.getUsedItemHand();
      }
      boolean main = entity.getMainHandItem().getItem() instanceof EvangeliumItem;
      boolean off = entity.getOffhandItem().getItem() instanceof EvangeliumItem;
      return off && !main ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   /** First-person: the empty off hand is drawn while a two-handed Regulus pose plays. */
   public static boolean wantsOffhand(Player player) {
      HeroPublicSnapshot snapshot = RegulusPoser.regulus(player);
      if (snapshot == null) {
         return false;
      }
      boolean ritualFreeHand = snapshot.ritualTicks() >= 0 && bookHand(player) == InteractionHand.MAIN_HAND;
      return ritualFreeHand || castPlaying(player, HeroAction.COUNTER) || castPlaying(player, HeroAction.DEBRIS_KICK);
   }

   private static float castWeight(View view, LivingEntity entity, HeroAction action) {
      RegulusActionClock clock = CLOCKS.get(entity);
      Layer layer = castLayer(action);
      if (clock == null || layer == null) {
         return 0.0F;
      }
      float weight = advance(view, entity, layer, clock.isPlaying(action.ordinal()));
      return clock.actionId() == action.ordinal() ? weight : 0.0F;
   }

   private static Layer castLayer(HeroAction action) {
      return switch (action) {
         case LIONS_HEART -> Layer.CAST_LIONS_HEART;
         case DEBRIS_KICK -> Layer.CAST_DEBRIS_KICK;
         case MANIA -> Layer.CAST_MANIA;
         case GREEDS_EMBRACE -> Layer.CAST_GREEDS_EMBRACE;
         case COUNTER -> Layer.CAST_COUNTER;
         default -> null;
      };
   }

   private static float advance(View view, LivingEntity entity, Layer layer, boolean active) {
      WeightState state = WEIGHTS.get(view).computeIfAbsent(entity, k -> new EnumMap<>(Layer.class)).computeIfAbsent(layer, k -> new WeightState());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         delta = Math.min(delta, 0.1F);
         state.lastTime = now;
         float smoothFactor = 1.0F - (float)Math.exp((double)(-layer.rate * delta));
         state.weight = Mth.lerp(smoothFactor, state.weight, active ? 1.0F : 0.0F);
         if (!active && state.weight < 0.001F) {
            state.weight = 0.0F;
         }
      }
      return state.weight;
   }

   private static final class WeightState {
      float weight;
      long lastTime = System.currentTimeMillis();
   }
}
