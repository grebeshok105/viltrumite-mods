package dev.baranhan.viltrumitecore.client.anim.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

public final class BakedGeoModel {
   public static boolean FLIP_U = true;
   public static boolean SWAP_EAST_WEST = true;
   private final List<GeoBone> topLevelBones;
   private final Map<String, GeoBone> bonesByName;
   private final float textureWidth;
   private final float textureHeight;

   private BakedGeoModel(List<GeoBone> topLevelBones, Map<String, GeoBone> bonesByName, float textureWidth, float textureHeight) {
      this.topLevelBones = Collections.unmodifiableList(topLevelBones);
      this.bonesByName = Collections.unmodifiableMap(bonesByName);
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
   }

   public List<GeoBone> topLevelBones() {
      return this.topLevelBones;
   }

   public GeoBone getBone(String name) {
      return this.bonesByName.get(name);
   }

   public Iterable<GeoBone> allBones() {
      return this.bonesByName.values();
   }

   public float textureWidth() {
      return this.textureWidth;
   }

   public float textureHeight() {
      return this.textureHeight;
   }

   public void resetBones() {
      for (GeoBone bone : this.bonesByName.values()) {
         bone.resetToDefault();
      }
   }

   public static BakedGeoModel parse(JsonObject root) {
      return parse(root, null, null);
   }

   public static BakedGeoModel parse(JsonObject root, Float overrideWidth, Float overrideHeight) {
      JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
      if (geometries != null && !geometries.isEmpty()) {
         JsonObject geo = geometries.get(0).getAsJsonObject();
         JsonObject desc = geo.getAsJsonObject("description");
         float texW = overrideWidth != null ? overrideWidth : (desc != null ? getFloat(desc, "texture_width", 16.0F) : 16.0F);
         float texH = overrideHeight != null ? overrideHeight : (desc != null ? getFloat(desc, "texture_height", 16.0F) : 16.0F);
         JsonArray bonesJson = geo.getAsJsonArray("bones");
         Map<String, GeoBone> byName = new LinkedHashMap<>();
         List<GeoBone> topLevel = new ArrayList<>();
         if (bonesJson != null) {
            List<JsonObject> pending = new ArrayList<>();

            for (JsonElement el : bonesJson) {
               pending.add(el.getAsJsonObject());
            }

            boolean progress = true;

            while (!pending.isEmpty() && progress) {
               progress = false;
               Iterator<JsonObject> it = pending.iterator();

               while (it.hasNext()) {
                  JsonObject boneJson = it.next();
                  String parentName = getString(boneJson, "parent", null);
                  if (parentName == null || byName.containsKey(parentName)) {
                     GeoBone parent = parentName == null ? null : byName.get(parentName);
                     GeoBone bone = buildBone(boneJson, parent, texW, texH);
                     byName.put(bone.name, bone);
                     if (parent == null) {
                        topLevel.add(bone);
                     }

                     it.remove();
                     progress = true;
                  }
               }
            }

            for (JsonObject orphan : pending) {
               GeoBone bone = buildBone(orphan, null, texW, texH);
               byName.put(bone.name, bone);
               topLevel.add(bone);
            }
         }

         return new BakedGeoModel(topLevel, byName, texW, texH);
      } else {
         throw new IllegalArgumentException("geo dosyasinda 'minecraft:geometry' yok (format_version 1.12.0 ve uzeri bir Blockbench ihraci gerekiyor)");
      }
   }

   private static GeoBone buildBone(JsonObject json, GeoBone parent, float texW, float texH) {
      String name = getString(json, "name", "bone");
      float[] pivot = getVec3(json, "pivot", 0.0F, 0.0F, 0.0F);
      float[] rot = getVec3(json, "rotation", 0.0F, 0.0F, 0.0F);
      GeoBone bone = new GeoBone(
         name,
         parent,
         -pivot[0],
         pivot[1],
         pivot[2],
         (float)Math.toRadians((double)(-rot[0])),
         (float)Math.toRadians((double)(-rot[1])),
         (float)Math.toRadians((double)rot[2])
      );
      JsonArray cubes = json.getAsJsonArray("cubes");
      if (cubes != null) {
         for (JsonElement el : cubes) {
            bone.cubes.add(buildCube(el.getAsJsonObject(), texW, texH));
         }
      }

      return bone;
   }

   private static GeoBone.Cube buildCube(JsonObject json, float texW, float texH) {
      float[] origin = getVec3(json, "origin", 0.0F, 0.0F, 0.0F);
      float[] size = getVec3(json, "size", 0.0F, 0.0F, 0.0F);
      float inflate = getFloat(json, "inflate", 0.0F);
      boolean mirror = getBool(json, "mirror", false);
      float[] cubePivot = getVec3(json, "pivot", 0.0F, 0.0F, 0.0F);
      float[] cubeRot = getVec3(json, "rotation", 0.0F, 0.0F, 0.0F);
      float x1 = (-(origin[0] + size[0]) - inflate) / 16.0F;
      float x2 = (-origin[0] + inflate) / 16.0F;
      float y1 = (origin[1] - inflate) / 16.0F;
      float y2 = (origin[1] + size[1] + inflate) / 16.0F;
      float z1 = (origin[2] - inflate) / 16.0F;
      float z2 = (origin[2] + size[2] + inflate) / 16.0F;
      JsonElement uvEl = json.get("uv");
      float[] down;
      float[] up;
      float[] north;
      float[] south;
      float[] east;
      float[] west;
      if (uvEl != null && uvEl.isJsonObject()) {
         JsonObject uvObj = uvEl.getAsJsonObject();
         down = faceUv(uvObj, "down");
         up = faceUv(uvObj, "up");
         north = faceUv(uvObj, "north");
         south = faceUv(uvObj, "south");
         float[] bedrockEast = faceUv(uvObj, "east");
         float[] bedrockWest = faceUv(uvObj, "west");
         east = SWAP_EAST_WEST ? bedrockWest : bedrockEast;
         west = SWAP_EAST_WEST ? bedrockEast : bedrockWest;
      } else {
         float u = 0.0F;
         float v = 0.0F;
         if (uvEl != null && uvEl.isJsonArray()) {
            JsonArray arr = uvEl.getAsJsonArray();
            u = arr.get(0).getAsFloat();
            v = arr.get(1).getAsFloat();
         }

         float sx = size[0];
         float sy = size[1];
         float sz = size[2];
         down = new float[]{u + sz, v, u + sz + sx, v + sz};
         up = new float[]{u + sz + sx, v + sz, u + sz + sx + sx, v};
         float[] strip1 = new float[]{u, v + sz, u + sz, v + sz + sy};
         north = new float[]{u + sz, v + sz, u + sz + sx, v + sz + sy};
         float[] strip2 = new float[]{u + sz + sx, v + sz, u + sz + sx + sz, v + sz + sy};
         south = new float[]{u + sz + sx + sz, v + sz, u + sz + sx + sz + sx, v + sz + sy};
         east = SWAP_EAST_WEST ? strip1 : strip2;
         west = SWAP_EAST_WEST ? strip2 : strip1;
      }

      if (mirror) {
         flipU(down);
         flipU(up);
         flipU(north);
         flipU(south);
         flipU(east);
         flipU(west);
         float[] tmp = east;
         east = west;
         west = tmp;
      }

      List<GeoBone.Quad> quads = new ArrayList<>(6);
      addQuad(quads, texW, texH, north, new Vector3f(0.0F, 0.0F, -1.0F), x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1);
      addQuad(quads, texW, texH, south, new Vector3f(0.0F, 0.0F, 1.0F), x2, y2, z2, x1, y2, z2, x1, y1, z2, x2, y1, z2);
      addQuad(quads, texW, texH, west, new Vector3f(-1.0F, 0.0F, 0.0F), x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2);
      addQuad(quads, texW, texH, east, new Vector3f(1.0F, 0.0F, 0.0F), x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1);
      addQuad(quads, texW, texH, up, new Vector3f(0.0F, 1.0F, 0.0F), x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1);
      addQuad(quads, texW, texH, down, new Vector3f(0.0F, -1.0F, 0.0F), x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
      return new GeoBone.Cube(
         quads.toArray(new GeoBone.Quad[0]),
         new Vector3f(-cubePivot[0] / 16.0F, cubePivot[1] / 16.0F, cubePivot[2] / 16.0F),
         new Vector3f((float)Math.toRadians((double)(-cubeRot[0])), (float)Math.toRadians((double)(-cubeRot[1])), (float)Math.toRadians((double)cubeRot[2]))
      );
   }

   private static void addQuad(
      List<GeoBone.Quad> out,
      float texW,
      float texH,
      float[] uv,
      Vector3f normal,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz
   ) {
      if (uv != null) {
         float u0 = uv[0] / texW;
         float v0 = uv[1] / texH;
         float u1 = uv[2] / texW;
         float v1 = uv[3] / texH;
         if (FLIP_U) {
            float t = u0;
            u0 = u1;
            u1 = t;
         }

         out.add(
            new GeoBone.Quad(
               new GeoBone.Vertex[]{
                  new GeoBone.Vertex(ax, ay, az, u0, v0),
                  new GeoBone.Vertex(bx, by, bz, u1, v0),
                  new GeoBone.Vertex(cx, cy, cz, u1, v1),
                  new GeoBone.Vertex(dx, dy, dz, u0, v1)
               },
               normal
            )
         );
      }
   }

   private static float[] faceUv(JsonObject uvObj, String face) {
      JsonElement el = uvObj.get(face);
      if (el != null && el.isJsonObject()) {
         JsonObject f = el.getAsJsonObject();
         float[] uv = getVec2(f, "uv", 0.0F, 0.0F);
         float[] sz = getVec2(f, "uv_size", 0.0F, 0.0F);
         return new float[]{uv[0], uv[1], uv[0] + sz[0], uv[1] + sz[1]};
      } else {
         return null;
      }
   }

   private static void flipU(float[] rect) {
      if (rect != null) {
         float t = rect[0];
         rect[0] = rect[2];
         rect[2] = t;
      }
   }

   private static String getString(JsonObject o, String key, String def) {
      JsonElement e = o.get(key);
      return e != null && !e.isJsonNull() ? e.getAsString() : def;
   }

   private static float getFloat(JsonObject o, String key, float def) {
      JsonElement e = o.get(key);
      return e != null && !e.isJsonNull() ? e.getAsFloat() : def;
   }

   private static boolean getBool(JsonObject o, String key, boolean def) {
      JsonElement e = o.get(key);
      return e != null && !e.isJsonNull() ? e.getAsBoolean() : def;
   }

   private static float[] getVec3(JsonObject o, String key, float dx, float dy, float dz) {
      JsonElement e = o.get(key);
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
      } else {
         return new float[]{dx, dy, dz};
      }
   }

   private static float[] getVec2(JsonObject o, String key, float dx, float dy) {
      JsonElement e = o.get(key);
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
      } else {
         return new float[]{dx, dy};
      }
   }
}
