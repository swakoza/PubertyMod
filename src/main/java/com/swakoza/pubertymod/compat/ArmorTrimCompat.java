package com.swakoza.pubertymod.compat;

import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.Map;

public final class ArmorTrimCompat {
    private ArmorTrimCompat() {}

    public static Identifier getTextureId(ArmorTrim trim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey) {
        Identifier modern = getModernTextureId(trim, layerType, assetKey);
        if (modern != null) return modern;

        Object material = trim.material().value();
        String assetName = getString(material, "assetName");
        Object overrides = invoke(material, "overrideArmorAssets");
        if (overrides instanceof Map<?, ?> overrideMap) {
            Object overrideValue = overrideMap.get(assetKey);
            if (overrideValue instanceof String overrideAssetName) {
                assetName = overrideAssetName;
            }
        }
        if (assetName == null) return trim.pattern().value().assetId();
        String resolvedAssetName = assetName;
        return trim.pattern().value().assetId().withPath(path -> "trims/models/armor/" + path + "_" + resolvedAssetName);
    }

    private static Identifier getModernTextureId(ArmorTrim trim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey) {
        try {
            Method getTrimsDirectory = layerType.getClass().getMethod("getTrimsDirectory");
            Object trimsDirectory = getTrimsDirectory.invoke(layerType);
            Method getTextureId = trim.getClass().getMethod("getTextureId", String.class, RegistryKey.class);
            Object textureId = getTextureId.invoke(trim, trimsDirectory, assetKey);
            return textureId instanceof Identifier identifier ? identifier : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String getString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof String string ? string : null;
    }
}
