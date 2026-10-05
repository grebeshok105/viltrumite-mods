package dev.baranhan.viltrumitecore.client.anim.geo;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

public class GeoBone {
   public final String name;
   public final GeoBone parent;
   public final List<GeoBone> children = new ArrayList<>();
   public final List<GeoBone.Cube> cubes = new ArrayList<>();
   public final float pivotX;
   public final float pivotY;
   public final float pivotZ;
   public final float initRotX;
   public final float initRotY;
   public final float initRotZ;
   public float rotX;
   public float rotY;
   public float rotZ;
   public float posX;
   public float posY;
   public float posZ;
   public float scaleX = 1.0F;
   public float scaleY = 1.0F;
   public float scaleZ = 1.0F;
   public boolean hidden = false;

   public GeoBone(String name, GeoBone parent, float pivotX, float pivotY, float pivotZ, float initRotX, float initRotY, float initRotZ) {
      this.name = name;
      this.parent = parent;
      this.pivotX = pivotX;
      this.pivotY = pivotY;
      this.pivotZ = pivotZ;
      this.initRotX = initRotX;
      this.initRotY = initRotY;
      this.initRotZ = initRotZ;
      this.resetToDefault();
      if (parent != null) {
         parent.children.add(this);
      }
   }

   public void resetToDefault() {
      this.rotX = this.initRotX;
      this.rotY = this.initRotY;
      this.rotZ = this.initRotZ;
      this.posX = 0.0F;
      this.posY = 0.0F;
      this.posZ = 0.0F;
      this.scaleX = 1.0F;
      this.scaleY = 1.0F;
      this.scaleZ = 1.0F;
   }

   public boolean hasCubeRotationFree() {
      return this.cubes.isEmpty();
   }

   @Override
   public String toString() {
      return "GeoBone[" + this.name + ", cubes=" + this.cubes.size() + ", children=" + this.children.size() + "]";
   }

   public static record Cube(GeoBone.Quad[] quads, Vector3f pivot, Vector3f rotation) {
      public boolean hasRotation() {
         return this.rotation.x != 0.0F || this.rotation.y != 0.0F || this.rotation.z != 0.0F;
      }
   }

   public static record Quad(GeoBone.Vertex[] vertices, Vector3f normal) {
   }

   public static record Vertex(float x, float y, float z, float u, float v) {
   }
}
