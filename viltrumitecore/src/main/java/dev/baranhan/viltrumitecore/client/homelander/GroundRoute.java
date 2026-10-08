package dev.baranhan.viltrumitecore.client.homelander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;

/**
 * Pure A* over standable foot positions (spec §6.4 route): 4 neighbours, a
 * step up or down of one block per move. Empty list = no route within the
 * node budget; the renderer then draws a dim dashed arc.
 */
public final class GroundRoute {
   private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   /** Can an entity stand with its feet in this block (floor below, room for the body). */
   public interface Walkable {
      boolean standable(int x, int y, int z);
   }

   private GroundRoute() {
   }

   public static List<BlockPos> find(BlockPos from, BlockPos to, Walkable walkable, int maxNodes) {
      if (!walkable.standable(from.getX(), from.getY(), from.getZ()) || !walkable.standable(to.getX(), to.getY(), to.getZ())) {
         return List.of();
      }

      long goal = to.asLong();
      Map<Long, Long> cameFrom = new HashMap<>();
      Map<Long, Integer> cost = new HashMap<>();
      PriorityQueue<long[]> open = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
      long start = from.asLong();
      cost.put(start, 0);
      open.add(new long[]{start, heuristic(from, to)});
      int expanded = 0;
      while (!open.isEmpty()) {
         long[] entry = open.poll();
         long node = entry[0];
         int g = cost.get(node);
         if (entry[1] > g + heuristic(BlockPos.of(node), to)) {
            continue; // stale queue entry
         }

         if (node == goal) {
            return rebuild(cameFrom, node);
         }

         if (++expanded > maxNodes) {
            return List.of();
         }

         BlockPos pos = BlockPos.of(node);
         for (int[] dir : DIRS) {
            for (int dy : new int[]{0, 1, -1}) {
               int x = pos.getX() + dir[0];
               int y = pos.getY() + dy;
               int z = pos.getZ() + dir[1];
               if (!walkable.standable(x, y, z)) {
                  continue;
               }

               long next = BlockPos.asLong(x, y, z);
               int nextCost = g + (dy == 0 ? 10 : 14);
               Integer known = cost.get(next);
               if (known == null || nextCost < known) {
                  cost.put(next, nextCost);
                  cameFrom.put(next, node);
                  open.add(new long[]{next, nextCost + heuristic(new BlockPos(x, y, z), to)});
               }

               break; // one standable height per column step
            }
         }
      }

      return List.of();
   }

   private static long heuristic(BlockPos a, BlockPos b) {
      return 10L * (Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ())) + 4L * Math.abs(a.getY() - b.getY());
   }

   private static List<BlockPos> rebuild(Map<Long, Long> cameFrom, long node) {
      List<BlockPos> path = new ArrayList<>();
      Long current = node;
      while (current != null) {
         path.add(BlockPos.of(current));
         current = cameFrom.get(current);
      }

      Collections.reverse(path);
      return path;
   }
}
