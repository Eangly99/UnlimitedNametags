package org.alexdev.unlimitednametags.platform;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BukkitNametagMaterialBridgeTest {
    private static final String URL = "https://textures.minecraft.net/texture/46c9923bebd9ad90a80a0731c3f3b9db729b0785015e18e3ec07e4e91099be06";

    @Test
    void resolvesTextureUrlsBase64AndCountryAliasesAndRejectsInvalidSources() {
        final String value = encode("{\"textures\":{\"SKIN\":{\"url\":\"" + URL + "\"}}}");
        assertEquals(value, BukkitNametagMaterialBridge.textureValue(URL, Map.of()));
        assertEquals(value, BukkitNametagMaterialBridge.textureValue(" " + value + " ", Map.of()));
        assertEquals(value, BukkitNametagMaterialBridge.textureValue("flag_US", Map.of("flag_US", URL)));
        assertEquals(value, BukkitNametagMaterialBridge.textureValue("flag_US", Map.of("flag_US", value)));
        for (String invalid : new String[]{"", "not-base64", "flag_XX", "%geolocation_countryCode%",
                encode("{}"), encode("[]"), encode("null"), encode("{\"textures\":{\"SKIN\":{\"url\":42}}}"),
                "https://example.com/texture/abc", URL + "?redirect=1", URL + "/extra",
                encode("{\"textures\":{\"SKIN\":{\"url\":\"https://example.com/skin\"}}}")}) {
            assertNull(BukkitNametagMaterialBridge.textureValue(invalid, Map.of()), invalid);
        }
        final Map<String, String> unset = new HashMap<>();
        unset.put("flag_US", null);
        assertNull(BukkitNametagMaterialBridge.textureValue("flag_US", unset));
    }

    private static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
