package dev.baranhan.viltrumitecore.util;

public interface ViltrumiteAbilityUser {
   String getAbilityInSlot(int var1);

   void setAbilityInSlot(int var1, String var2);

   int getActivePage();

   void setActivePage(int var1);

   /**
    * Default abilities the current hero already gave this player (server side,
    * saved). A slot the player cleared is not refilled for an id in this set.
    * Null for a save from before the set existed.
    */
   @javax.annotation.Nullable
   java.util.Set<String> getOfferedAbilities();

   void setOfferedAbilities(@javax.annotation.Nullable java.util.Set<String> offered);
}
