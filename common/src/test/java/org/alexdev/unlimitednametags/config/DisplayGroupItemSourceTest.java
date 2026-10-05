package org.alexdev.unlimitednametags.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import de.exlll.configlib.YamlConfigurations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisplayGroupItemSourceTest {

    @Test
    void headTextureSurvivesCopiesAndYamlRoundTrip() throws Exception {
        final Settings.DisplayGroup head = Settings.DisplayGroup.builder()
                .displayType(NametagDisplayType.ITEM).headTexture("owner").build();
        assertEquals("PLAYER_HEAD", head.effectiveItemMaterial());
        assertEquals("STONE", Settings.DisplayGroup.builder().build().effectiveItemMaterial());
        for (Settings.DisplayGroup copy : List.of(Settings.DisplayGroup.builder(head).build(),
                head.withLines(List.of()), head.withBackground(null), head.withScale(2f),
                head.withWhen("true"), head.withBillboard(null), head.withYOffset(2f),
                head.withAnimation(null), head.withGlow(null), head.withGlowInterval(2))) {
            assertEquals("owner", copy.headTexture());
        }
        final Settings settings = new Settings();
        settings.getNameTags().put("default", new Settings.NameTag(List.of(head)));
        final var path = Files.createTempFile("unt-head-test-", ".yml");
        try {
            YamlConfigurations.save(path, Settings.class, settings, UntYamlConfiguration.PROPERTIES);
            final Settings loaded = YamlConfigurations.load(path, Settings.class, UntYamlConfiguration.PROPERTIES);
            assertEquals("owner", loaded.getNameTags().get("default").displayGroups().getFirst().headTexture());
            assertFalse(Files.readString(path).contains("headTextures:"));
            assertFalse(Files.readString(path).contains("FLAG_TEXTURES:"));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void obsoleteHeadTexturesAreRemovedWithBackup(@TempDir Path directory) throws Exception {
        final Path path = directory.resolve("settings.yml");
        final String original = "configVersion: " + SettingsConfigVersion.CURRENT
                + "\nheadTextures:\n  flag_KH: old-texture\nnameTags: {}\n";
        Files.writeString(path, original);
        SettingsYamlMigrator.migrateIfNeeded(path, Logger.getAnonymousLogger());
        assertFalse(Files.readString(path).contains("headTextures:"));
        try (var backups = Files.list(directory.resolve("migration-backups"))) {
            assertEquals(original, Files.readString(backups.findFirst().orElseThrow()));
        }
    }

    @Test
    void eachItemSourceRoundTripsThroughBuilderAndCopy() {
        final Settings.DisplayGroup cmd = Settings.DisplayGroup.builder()
                .displayType(NametagDisplayType.ITEM)
                .itemMaterial("PAPER")
                .customModelData(42)
                .build();
        final Settings.DisplayGroup model = Settings.DisplayGroup.builder()
                .displayType(NametagDisplayType.ITEM)
                .itemMaterial("PAPER")
                .itemModel("example:badge")
                .build();
        final Settings.DisplayGroup nexo = Settings.DisplayGroup.builder()
                .displayType(NametagDisplayType.ITEM)
                .nexoId("crown")
                .build();

        assertEquals(42, Settings.DisplayGroup.builder(cmd).build().customModelData());
        assertEquals("example:badge", Settings.DisplayGroup.builder(model).build().itemModel());
        assertEquals("crown", Settings.DisplayGroup.builder(nexo).build().nexoId());
        assertNull(nexo.blockMaterial());
    }

    @Test
    void previousDisplayGroupConstructorRemainsAvailable() {
        final Settings.DisplayGroup group = new Settings.DisplayGroup(
                java.util.List.of(), null, 1.0f, 0.0f, null, false, NametagDisplayType.ITEM,
                "PAPER", null, "HEAD", null, null, null, null, null);

        assertEquals("PAPER", group.itemMaterial());
        assertNull(group.customModelData());
        assertNull(group.itemModel());
        assertNull(group.nexoId());
        assertNull(group.headTexture());
        final Settings.DisplayGroup previous = new Settings.DisplayGroup(
                List.of(), null, 1f, 0f, null, false, NametagDisplayType.ITEM,
                "PAPER", 42, null, null, null, "HEAD", null, null, null, null, null);
        assertEquals(42, previous.customModelData());
        assertNull(previous.headTexture());
    }

    @Test
    void rejectsBothVanillaModelSourcesWithoutNexo() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Settings.DisplayGroup.builder()
                        .displayType(NametagDisplayType.ITEM)
                        .itemMaterial("PAPER")
                        .customModelData(42)
                        .itemModel("example:badge")
                        .build());

        assertEquals("ITEM display group cannot set both customModelData and itemModel", error.getMessage());
    }
}
