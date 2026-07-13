package com.okereke.rpgmood.farming;

import org.bukkit.Material;

import java.util.List;

/**
 * Model for a discoverable cooking recipe.
 * Defined in farming.yml under the recipes section.
 */
public record Recipe(
        String id,
        String name,
        List<Material> ingredients,
        Material result,
        String effect,
        int effectDuration,
        CropQuality minQuality,
        String description
) {

    /** Checks if the given list of materials matches this recipe's ingredients (order-independent). */
    public boolean matches(List<Material> offeredIngredients) {
        if (offeredIngredients.size() != ingredients.size()) {
            return false;
        }
        List<Material> sortedOffered = offeredIngredients.stream()
                .sorted()
                .toList();
        List<Material> sortedRequired = ingredients.stream()
                .sorted()
                .toList();
        return sortedOffered.equals(sortedRequired);
    }
}
