<div align="center">

# UnlimitedNameTags

Custom stacked nametags for Paper servers: text, item, and block display rows with placeholders, animations, visibility rules, and a developer API.

[![GitHub Release](https://img.shields.io/github/release/alexdev03/unlimitednametags.svg)](https://github.com/alexdev03/UnlimitedNameTags/releases)
[![CodeFactor](https://www.codefactor.io/repository/github/alexdev03/unlimitednametags/badge)](https://www.codefactor.io/repository/github/alexdev03/unlimitednametags)
[![BuiltByBit](https://img.shields.io/badge/BuiltByBit-resource-lightblue?style=for-the-badge)](https://builtbybit.com/resources/unlimitednametags.46172/)
[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/W4Fu8fqCKs)

<img alt="UnlimitedNameTags in action" src="https://i.imgur.com/w7zlGaO.gif" width="640">

</div>

## Requirements

- Paper 1.21.4+ or a compatible Paper fork such as Purpur or Folia.
- PacketEvents installed on the server.
- Spigot and other non-Paper servers are not supported.

## Features

- Multiple stacked `displayGroups` per player.
- TEXT rows with structured lines and optional per-line `when` conditions.
- ITEM and BLOCK rows with configurable material, scale, offset, billboard, glow, and animations.
- Player head ITEM rows using the owner's skin, custom Base64/URL textures, or country flag texture keys.
- Inline `%flag%` / `%flags%` images in TEXT rows, with Cambodia as the fallback.
- PlaceholderAPI, MiniPlaceholders, vanish integrations, Geyser support notes, and hook support for common cosmetic plugins.
- Config migration through `configVersion` and `SettingsYamlMigrator`.

## Install

1. Put PacketEvents and `UnlimitedNametags.jar` in `plugins/`.
2. Restart the server.
3. Edit `plugins/UnlimitedNameTags/settings.yml`.

## Build

```bash
./gradlew :paper:shadowJar
```

On Windows:

```bat
gradlew.bat :paper:shadowJar
```

The server jar is written to `target/UnlimitedNametags.jar`.

## API

Use the artifact that matches the API surface you need:

```kotlin
dependencies {
    compileOnly("io.github.alexdev03:unlimitednametags-api-paper:2.0.0")
    // compileOnly("io.github.alexdev03:unlimitednametags-api:2.0.0")
}
```

- `api-paper`: Paper/Bukkit types, `UNTPaperAPI`, `Player` overloads, `Formatter`.
- `api`: UUID and Adventure-only interfaces.
- `common`: shared config and value types used by the API.

Publishing notes are in [MAVEN_CENTRAL_PUBLISHING.md](MAVEN_CENTRAL_PUBLISHING.md). Breaking changes are tracked in [CHANGELOG.md](CHANGELOG.md).

## Minimal Config Example

```yaml
displayGroups:
  - lines:
      - text: "%luckperms_prefix%%player_name%"
      - text: "&a%player_ping%ms"
        when: "%player_ping% < 70"
    scale: 1.0
    yOffset: 1.0
```

## Inline country flags

Put `%flag%` or its alias `%flags%` directly in any TEXT line:

```yaml
- text: '%flags% &fTeam: &8[%zelteams_team_name_colored%&8]'
```

Both placeholders insert a flag image before the team text. With no country lookup
or an unsupported country, the flag defaults to Cambodia.
Use `behavior.format: LEGACY` or `UNIVERSAL` for the `&` color codes in this example.

Inline images use Minecraft's native [player head text components](https://jd.advntr.dev/api/latest/net/kyori/adventure/text/object/PlayerHeadObjectContents.html),
supported by Minecraft **1.21.9+** clients. No resource pack or separate ITEM row is required.

To select the flag from the owner's IP, install PlaceholderAPI and its
[Geolocation expansion](https://api.extendedclip.com/expansions/geolocation/) with
`/papi ecloud download Geolocation` and `/papi reload`. The plugin reads
`%geolocation_countryCode%` automatically. Textures for all **249 ISO country/territory codes**, plus
Kosovo (`XK`), are bundled in the jar; no texture configuration is needed.
Missing or invalid country lookup still falls back to Cambodia. Vietnam (`VN`) is included.
The obsolete `headTextures` section is removed on startup/reload after backing up the settings file.
The Cambodia texture is from [MC-Heads](https://mc-heads.com/skulls/4672d426-53d9-49e8-b84c-41af7764fe77).

The bundled [texture catalogue](common/src/main/resources/country-flags.properties) is based on
[Minecraft Heads](https://minecraft-heads.com/) via its [CSV mirror](https://github.com/TheLuca98/MinecraftHeads).
Territories without dedicated heads use these flags: `AI`, `VG`, `KY`, `FK`, `IM`, `MS`, `PN`,
`GS`, `SH`, `TC` → `GB`; `BV`, `SJ` → `NO`; `BQ` → `NL`; `HM` → `AU`; `MF` → `FR`;
`TK` → `NZ`; `UM` → `US`. Afghanistan uses the catalogue's older flag design.

If a VPN reconnect still shows Cambodia, run `/papi parse me %geolocation_countryCode%` in game.
For a Vietnam connection it must return `VN`. An unchanged placeholder means the expansion is
missing; `API Down` or `invalid identifier` means the lookup failed. If it returns `KH`, check
the VPN connection and proxy IP forwarding. UnlimitedNameTags clears its placeholder cache on quit;
[Geolocation owns the IP lookup and its own cache](https://github.com/PlaceholderAPI/Geolocation-Expansion/blob/master/src/main/java/me/itsnathang/placeholders/Geolocation.java).

## Head texture ITEM rows

Add these rows under `nameTags.default.displayGroups` in `settings.yml`:

```yaml
nameTags:
  default:
    displayGroups:
      - displayType: ITEM
        headTexture: owner
        scale: 0.5
        yOffset: 0.8
      - displayType: ITEM
        headTexture: "flag_%geolocation_countryCode%"
        scale: 0.5
        yOffset: 1.3
```

`headTexture` accepts `owner`, a Base64 `textures` property, a Minecraft texture URL,
or a built-in flag key (`flag_<ISO country code>`, such as `flag_KH`, `flag_VN`, or `flag_US`).
It expands placeholders for the nametag owner.
Omitting `itemMaterial` selects `PLAYER_HEAD` when a texture is configured; explicit
materials must be `PLAYER_HEAD`. The existing `itemDisplayMode` defaults to `HEAD`.
Nexo items keep precedence and ignore `headTexture`.

Country detection uses the player's IP through [PlaceholderAPI's Geolocation expansion](https://api.extendedclip.com/expansions/geolocation/).
With PlaceholderAPI installed, run `/papi ecloud download Geolocation` and `/papi reload`.
The expansion handles the IP lookup; UnlimitedNameTags selects the built-in texture.
All ISO country/territory codes and Kosovo are included in the bundled catalogue, with the
territory/legacy exceptions described above. The US texture comes from [Minecraft Heads](https://minecraft-heads.com/custom-heads/head/70017-united-states-of-america).
Other GeoIP plugins can supply a country-code placeholder in the same way.
Unresolved placeholders, missing texture keys, and invalid textures clear the row;
compact stacking leaves no gap for it. IP geolocation reflects the connecting IP,
including VPN/proxy addresses, rather than a player's nationality. Apply config changes with `/unt reload`.

For Java integrations, use `Settings.DisplayGroup.builder().displayType(NametagDisplayType.ITEM).headTexture("owner").build()`.
Both previous `DisplayGroup` constructors remain available for compiled integrations.

## Support

Use [Discord](https://discord.gg/W4Fu8fqCKs) for questions and support.
