package dev.baranhan.viltrumitecore.client.ironman.veronica;

import dev.baranhan.viltrumitecore.hero.ironman.veronica.VeronicaView;
import net.minecraft.client.Minecraft;

/** Client side of the Veronica suit menu (spec §12.3): the server sends the roster view, the screen sends the choice. */
public final class VeronicaClient {
   private VeronicaClient() {
   }

   public static void open(VeronicaView view) {
      Minecraft.getInstance().setScreen(new VeronicaScreen(view));
   }
}
