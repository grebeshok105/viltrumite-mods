package dev.baranhan.viltrumitecore.client.anim.animation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class Easing {
   private static final Map<String, DoubleUnaryOperator> REGISTRY = new HashMap<>();
   public static final DoubleUnaryOperator LINEAR = t -> t;
   public static final DoubleUnaryOperator STEP = t -> 0.0;

   private static void put(String name, DoubleUnaryOperator fn) {
      REGISTRY.put(name, fn);
   }

   private static double outBounce(double t) {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (t < 1.0 / d1) {
         return n1 * t * t;
      } else if (t < 2.0 / d1) {
         double var8;
         return n1 * (var8 = t - 1.5 / d1) * var8 + 0.75;
      } else {
         double var6;
         double var7;
         return t < 2.5 / d1 ? n1 * (var6 = t - 2.25 / d1) * var6 + 0.9375 : n1 * (var7 = t - 2.625 / d1) * var7 + 0.984375;
      }
   }

   public static DoubleUnaryOperator byName(String name) {
      return name == null ? LINEAR : REGISTRY.getOrDefault(name.toLowerCase(Locale.ROOT).replace("_", ""), LINEAR);
   }

   private Easing() {
   }

   static {
      put("linear", LINEAR);
      put("step", STEP);
      put("constant", STEP);
      put("easeinsine", t -> 1.0 - Math.cos(t * Math.PI / 2.0));
      put("easeoutsine", t -> Math.sin(t * Math.PI / 2.0));
      put("easeinoutsine", t -> -(Math.cos(Math.PI * t) - 1.0) / 2.0);
      put("easeinquad", t -> t * t);
      put("easeoutquad", t -> 1.0 - (1.0 - t) * (1.0 - t));
      put("easeinoutquad", t -> t < 0.5 ? 2.0 * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0);
      put("easeincubic", t -> t * t * t);
      put("easeoutcubic", t -> 1.0 - Math.pow(1.0 - t, 3.0));
      put("easeinoutcubic", t -> t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0);
      put("easeinquart", t -> t * t * t * t);
      put("easeoutquart", t -> 1.0 - Math.pow(1.0 - t, 4.0));
      put("easeinoutquart", t -> t < 0.5 ? 8.0 * t * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 4.0) / 2.0);
      put("easeinquint", t -> Math.pow(t, 5.0));
      put("easeoutquint", t -> 1.0 - Math.pow(1.0 - t, 5.0));
      put("easeinoutquint", t -> t < 0.5 ? 16.0 * Math.pow(t, 5.0) : 1.0 - Math.pow(-2.0 * t + 2.0, 5.0) / 2.0);
      put("easeinexpo", t -> t == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * t - 10.0));
      put("easeoutexpo", t -> t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t));
      put("easeinoutexpo", t -> {
         if (t == 0.0) {
            return 0.0;
         } else if (t == 1.0) {
            return 1.0;
         } else {
            return t < 0.5 ? Math.pow(2.0, 20.0 * t - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
         }
      });
      put("easeincirc", t -> 1.0 - Math.sqrt(1.0 - t * t));
      put("easeoutcirc", t -> Math.sqrt(1.0 - Math.pow(t - 1.0, 2.0)));
      put("easeinoutcirc", t -> t < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * t, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * t + 2.0, 2.0)) + 1.0) / 2.0);
      put("easeinback", t -> 2.70158 * t * t * t - 1.70158 * t * t);
      put("easeoutback", t -> 1.0 + 2.70158 * Math.pow(t - 1.0, 3.0) + 1.70158 * Math.pow(t - 1.0, 2.0));
      put(
         "easeinoutback",
         t -> {
            double c = 2.5949095;
            return t < 0.5
               ? Math.pow(2.0 * t, 2.0) * ((c + 1.0) * 2.0 * t - c) / 2.0
               : (Math.pow(2.0 * t - 2.0, 2.0) * ((c + 1.0) * (t * 2.0 - 2.0) + c) + 2.0) / 2.0;
         }
      );
      put("easeinelastic", t -> t != 0.0 && t != 1.0 ? -Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)) : t);
      put("easeoutelastic", t -> t != 0.0 && t != 1.0 ? Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0 : t);
      put("easeoutbounce", Easing::outBounce);
      put("easeinbounce", t -> 1.0 - outBounce(1.0 - t));
      put("easeinoutbounce", t -> t < 0.5 ? (1.0 - outBounce(1.0 - 2.0 * t)) / 2.0 : (1.0 + outBounce(2.0 * t - 1.0)) / 2.0);
   }
}
