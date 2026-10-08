/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.resource.serialize;

import com.google.gson.*;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/** Public pack adapters matching vanilla's protected 26.4 cuboid transform deserializers. */
public final class TransformSerializers {
    private TransformSerializers() {}
    public static final JsonDeserializer<ItemTransform> ITEM=(json,type,context)->{
        JsonObject object=json.getAsJsonObject();
        Vector3f rotation=vector(object,"rotation",0),translation=vector(object,"translation",0).mul(1f/16f),scale=vector(object,"scale",1);
        clamp(translation,5);clamp(scale,4);
        return new ItemTransform(rotation,translation,scale);
    };
    public static final JsonDeserializer<ItemTransforms> ITEMS=(json,type,context)->{
        JsonObject object=json.getAsJsonObject();
        ItemTransform right=transform(object,context,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform left=transform(object,context,ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        if(left==ItemTransform.NO_TRANSFORM)left=right;
        ItemTransform firstRight=transform(object,context,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform firstLeft=transform(object,context,ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        if(firstLeft==ItemTransform.NO_TRANSFORM)firstLeft=firstRight;
        return new ItemTransforms(left,right,firstLeft,firstRight,transform(object,context,ItemDisplayContext.HEAD),
                transform(object,context,ItemDisplayContext.GUI),transform(object,context,ItemDisplayContext.GROUND),
                transform(object,context,ItemDisplayContext.FIXED),transform(object,context,ItemDisplayContext.ON_SHELF));
    };
    private static ItemTransform transform(JsonObject object,JsonDeserializationContext context,ItemDisplayContext display) {
        String name=display.getSerializedName();return object.has(name)?context.deserialize(object.get(name),ItemTransform.class):ItemTransform.NO_TRANSFORM;
    }
    private static Vector3f vector(JsonObject object,String name,float fallback) {
        if(!object.has(name))return new Vector3f(fallback);
        JsonArray array=object.getAsJsonArray(name);
        if(array.size()!=3)throw new JsonParseException("Expected 3 "+name+" values, found "+array.size());
        return new Vector3f(array.get(0).getAsFloat(),array.get(1).getAsFloat(),array.get(2).getAsFloat());
    }
    private static void clamp(Vector3f vector,float max) {
        vector.set(Math.clamp(vector.x,-max,max),Math.clamp(vector.y,-max,max),Math.clamp(vector.z,-max,max));
    }
}
