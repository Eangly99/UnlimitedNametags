package org.alexdev.unlimitednametags.platform;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.google.gson.JsonParser;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.alexdev.unlimitednametags.UnlimitedNameTags;
import org.alexdev.unlimitednametags.config.Settings;
import org.alexdev.unlimitednametags.hook.NexoHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class BukkitNametagMaterialBridge implements NametagMaterialBridge {

    private static final Pattern TEXTURE_URL = Pattern.compile("https?://textures\\.minecraft\\.net/texture/[0-9a-fA-F]{1,64}");

    private final UnlimitedNameTags plugin;

    public BukkitNametagMaterialBridge(@NotNull UnlimitedNameTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable Object resolveItemStack(@NotNull UUID ownerId, @NotNull String materialKey) {
        final org.bukkit.inventory.ItemStack item = resolveBukkitItem(ownerId, materialKey);
        return item != null ? SpigotConversionUtil.fromBukkitItemStack(item) : null;
    }

    @Override
    public @Nullable Object resolveItemStack(@NotNull UUID ownerId, @NotNull String materialKey,
            @Nullable Integer customModelData, @Nullable String itemModel, @Nullable String nexoId) {
        return resolveItemStack(ownerId, materialKey, customModelData, itemModel, nexoId, null);
    }

    @Override
    public @Nullable Object resolveItemStack(@NotNull UUID ownerId, @NotNull String materialKey,
            @Nullable Integer customModelData, @Nullable String itemModel, @Nullable String nexoId,
            @Nullable String headTexture) {
        if (nexoId != null && !nexoId.isBlank()) {
            if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                return null;
            }
            final org.bukkit.inventory.ItemStack nexoItem = plugin.getHook(NexoHook.class)
                    .map(hook -> hook.itemFromId(expand(ownerId, nexoId)))
                    .orElse(null);
            return nexoItem != null ? SpigotConversionUtil.fromBukkitItemStack(nexoItem) : null;
        }

        final org.bukkit.inventory.ItemStack item = resolveBukkitItem(ownerId, materialKey);
        if (item == null) {
            return null;
        }
        final ItemMeta meta = item.getItemMeta();
        if (headTexture != null && !headTexture.isBlank()) {
            if (item.getType() != Material.PLAYER_HEAD || !(meta instanceof SkullMeta skull)) {
                return null;
            }
            final String source = expand(ownerId, headTexture);
            final PlayerProfile profile;
            if (source.equalsIgnoreCase("owner")) {
                final Player owner = plugin.getPlayerListener().getPlayer(ownerId);
                if (owner == null) {
                    return null;
                }
                profile = owner.getPlayerProfile();
            } else {
                final String value = textureValue(source, Settings.FLAG_TEXTURES);
                if (value == null) {
                    return null;
                }
                profile = Bukkit.createProfileExact(UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)), "UNT");
                profile.setProperty(new ProfileProperty("textures", value));
            }
            skull.setPlayerProfile(profile);
        }
        if (itemModel != null && !itemModel.isBlank()) {
            final NamespacedKey key = NamespacedKey.fromString(expand(ownerId, itemModel));
            if (key == null) {
                return null;
            }
            meta.setItemModel(key);
        } else if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return SpigotConversionUtil.fromBukkitItemStack(item);
    }

    @Nullable
    public static String textureValue(@NotNull String source, @NotNull Map<String, String> textures) {
        final String resolved = textures.getOrDefault(source.trim(), source.trim());
        if (resolved == null) {
            return null;
        }
        final String value = resolved.trim();
        if (TEXTURE_URL.matcher(value).matches()) {
            return Base64.getEncoder().encodeToString(
                    ("{\"textures\":{\"SKIN\":{\"url\":\"" + value + "\"}}}").getBytes(StandardCharsets.UTF_8));
        }
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            final String url = new JsonParser().parse(json).getAsJsonObject().getAsJsonObject("textures")
                    .getAsJsonObject("SKIN").get("url").getAsString();
            return TEXTURE_URL.matcher(url).matches() ? value : null;
        } catch (RuntimeException invalidTexture) {
            return null;
        }
    }

    @Nullable
    private org.bukkit.inventory.ItemStack resolveBukkitItem(@NotNull UUID ownerId, @NotNull String materialKey) {
        final String expanded = expand(ownerId, materialKey);
        final org.bukkit.inventory.ItemStack registryItem = resolveItemFromRegistry(expanded);
        if (registryItem != null) {
            return registryItem;
        }
        final Material material = Material.matchMaterial(expanded, false);
        return material != null && material.isItem() ? new org.bukkit.inventory.ItemStack(material) : null;
    }

    @Override
    public @Nullable Object resolveBlockState(@NotNull UUID ownerId, @NotNull String materialKey) {
        final String expanded = expand(ownerId, materialKey);

        final org.bukkit.block.data.BlockData data = resolveBlockFromRegistry(expanded);
        if (data != null) {
            return SpigotConversionUtil.fromBukkitBlockData(data);
        }

        final Material material = Material.matchMaterial(expanded, false);
        if (material == null || !material.isBlock()) {
            return null;
        }
        return SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
    }

    public static org.bukkit.inventory.ItemStack resolveItemFromRegistry(String key) {
        try {
            final org.bukkit.NamespacedKey namespacedKey = org.bukkit.NamespacedKey.fromString(key);
            if (namespacedKey != null) {
                final org.bukkit.inventory.ItemType itemType = org.bukkit.Registry.ITEM.get(namespacedKey);
                if (itemType != null) {
                    return itemType.createItemStack();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static org.bukkit.block.data.BlockData resolveBlockFromRegistry(String key) {
        try {
            final org.bukkit.NamespacedKey namespacedKey = org.bukkit.NamespacedKey.fromString(key);
            if (namespacedKey != null) {
                final org.bukkit.block.BlockType blockType = org.bukkit.Registry.BLOCK.get(namespacedKey);
                if (blockType != null) {
                    return blockType.createBlockData();
                }
            }
        } catch (Throwable ignored) {}

        try {
            return org.bukkit.Bukkit.createBlockData(key);
        } catch (Throwable ignored) {}
        return null;
    }

    @NotNull
    private String expand(@NotNull UUID ownerId, @NotNull String raw) {
        final Player owner = plugin.getPlayerListener().getPlayer(ownerId);
        if (owner == null) {
            return raw.trim();
        }
        return plugin.getPlaceholderManager().expandForOwner(owner, raw).trim();
    }

    public static ItemStack requireItemStack(@NotNull Object resolved) {
        return (ItemStack) resolved;
    }

    public static WrappedBlockState requireBlockState(@NotNull Object resolved) {
        return (WrappedBlockState) resolved;
    }

    public static String defaultMaterialKey(@Nullable String raw, @NotNull String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw;
    }

    public static Material matchOrNull(@NotNull String expanded) {
        return Material.matchMaterial(expanded.toUpperCase(Locale.ROOT), false);
    }
}
