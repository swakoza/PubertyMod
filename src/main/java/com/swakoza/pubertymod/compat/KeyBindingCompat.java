package com.swakoza.pubertymod.compat;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public final class KeyBindingCompat {
    private KeyBindingCompat() {}

    public static KeyBinding create(String translationKey, InputUtil.Type type, int code, Identifier categoryId) {
        try {
            Class<?> categoryClass = findCategoryClass();
            Object category = createCategory(categoryClass, categoryId);
            Constructor<KeyBinding> constructor = KeyBinding.class.getConstructor(String.class, InputUtil.Type.class, int.class, categoryClass);
            return constructor.newInstance(translationKey, type, code, category);
        } catch (ReflectiveOperationException ignored) {
            try {
                Constructor<KeyBinding> constructor = KeyBinding.class.getConstructor(String.class, InputUtil.Type.class, int.class, String.class);
                return constructor.newInstance(translationKey, type, code, "key.categories." + categoryId.getNamespace() + "." + categoryId.getPath());
            } catch (ReflectiveOperationException legacyException) {
                throw new IllegalStateException("Unsupported KeyBinding constructor", legacyException);
            }
        }
    }

    private static Class<?> findCategoryClass() throws NoSuchMethodException {
        for (Class<?> nestedClass : KeyBinding.class.getDeclaredClasses()) {
            try {
                nestedClass.getConstructor(Identifier.class);
                return nestedClass;
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException("KeyBinding category class");
    }

    private static Object createCategory(Class<?> categoryClass, Identifier categoryId) throws ReflectiveOperationException {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != categoryClass
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != Identifier.class) {
                continue;
            }

            method.setAccessible(true);
            try {
                return method.invoke(null, categoryId);
            } catch (InvocationTargetException exception) {
                Object existingCategory = findRegisteredCategory(categoryClass, categoryId);
                if (existingCategory != null) {
                    return existingCategory;
                }
                throw exception;
            }
        }

        Object category = categoryClass.getConstructor(Identifier.class).newInstance(categoryId);
        registerCategoryIfNeeded(categoryClass, category);
        return category;
    }

    private static Object findRegisteredCategory(Class<?> categoryClass, Identifier categoryId) throws ReflectiveOperationException {
        for (Object category : getCategoryList(categoryClass)) {
            Method idMethod = findIdMethod(categoryClass);
            if (idMethod != null && categoryId.equals(idMethod.invoke(category))) {
                return category;
            }
        }
        return null;
    }

    private static void registerCategoryIfNeeded(Class<?> categoryClass, Object category) throws ReflectiveOperationException {
        List<?> categories = getCategoryList(categoryClass);
        if (!categories.contains(category)) {
            @SuppressWarnings("unchecked")
            List<Object> mutableCategories = (List<Object>) categories;
            mutableCategories.add(category);
        }
    }

    private static List<?> getCategoryList(Class<?> categoryClass) throws IllegalAccessException, NoSuchFieldException {
        for (Field field : categoryClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && List.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (List<?>) field.get(null);
            }
        }
        throw new NoSuchFieldException("KeyBinding category list");
    }

    private static Method findIdMethod(Class<?> categoryClass) {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == Identifier.class
                    && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
