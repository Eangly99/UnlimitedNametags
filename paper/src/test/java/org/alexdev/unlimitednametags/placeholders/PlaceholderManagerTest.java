package org.alexdev.unlimitednametags.placeholders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.alexdev.unlimitednametags.config.Settings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderManagerTest {

    @Test
    void bothInlineAliasesKeepTeamFormattingAndUseCambodiaAsFallback() {
        final Component team = Component.text("Team: ", NamedTextColor.WHITE)
                .append(Component.text("[Blue]", NamedTextColor.DARK_GRAY));
        final Component line = Component.text("%flag% ").append(team).append(Component.text(" %flags%"));
        for (String country : new String[]{null, "", "KH", "XX", "%geolocation_countryCode%"}) {
            final Component result = PlaceholderManager.replaceFlagPlaceholders(line, country);
            assertTrue(result.contains(team), "team colors and text must survive replacement");
            final List<ObjectComponent> flags = flags(result);
            assertEquals(2, flags.size());
            for (ObjectComponent flag : flags) {
                final PlayerHeadObjectContents head = (PlayerHeadObjectContents) flag.contents();
                assertEquals(NamedTextColor.WHITE, flag.color());
                assertTrue(head.hat(), "Cambodia's white temple is on the skin's hat layer");
                assertTrue(textureJson(head).contains(Settings.CAMBODIA_FLAG_TEXTURE));
            }
        }
    }

    @Test
    void builtInCountryTextureIsSelectedForOwner() {
        final String us = Settings.FLAG_TEXTURES.get("flag_US");
        final Component line = Component.text("%flags% Team");
        final Component result = PlaceholderManager.replaceFlagPlaceholders(line, " us ");
        assertTrue(textureJson((PlayerHeadObjectContents) flags(result).getFirst().contents()).contains(us));
    }

    private static List<ObjectComponent> flags(Component text) {
        final List<ObjectComponent> result = new ArrayList<>();
        for (Component component : text.iterable(ComponentIteratorType.DEPTH_FIRST)) {
            if (component instanceof ObjectComponent object) result.add(object);
        }
        return result;
    }

    private static String textureJson(PlayerHeadObjectContents head) {
        return new String(Base64.getDecoder().decode(head.profileProperties().getFirst().value()), StandardCharsets.UTF_8);
    }

    @Test
    void relationalLegacyPrefixIsInsertedBeforeFollowingLiteralText() {
        final String result = PlaceholderManager.replaceRelationalPlaceholders(
                "%rel_prefix%Alice",
                placeholder -> {
                    assertEquals("%rel_prefix%", placeholder);
                    return "&c";
                }
        );

        assertEquals("&cAlice", result);
        assertTrue(PlaceholderManager.containsRelationalPlaceholders("Alice%rel_suffix%"));
    }
}
