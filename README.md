![Mod Banner](https://github.com/swakoza/Puberty-Mod/blob/main/src/main/resources/assets/swakozas_puberty_mod/banner.png?raw=true)
# Puberty Mod

**Current version: 1.2 beta**

This update adds configurable breast physics across supported Minecraft versions, improved player editing and previews, custom sound distance handling, and reusable presets for appearance, physics, and sounds.

### Description
This mod adds the ability to visually customize other players' models by changing their gender.<br>
When the gender is changed to female or “other,” a customizable bust is added to the character model.<br>
You can also set custom damage sounds for players with the gender set to female or “other” if you wish.

## Default Controls

G - Open Puberty Mod menu<br>
H - Opens the edit menu when you hover over a player 

## Minecraft 1.21.1

Build the Fabric test jar with `gradlew.bat build copyVersionedJar -PtargetMinecraftVersion=1.21.1`.
The jar is copied to `dist/1.21.1`. Breast physics reacts to jumps and torso animations
from EmoteCraft/playerAnimator or Player Animation Library when those mods are installed.
The animation libraries are optional at runtime.
The editor locks the preview camera while showing actions and emotes of loaded players.
Players outside client tracking range use a static preview of their synced appearance;
their live actions are not available until the server tracks them again.
H selects a visible player within 32 blocks (walls block selection), and also opens
the hovered row in the player list when the search field is not focused.

## License

Puberty-Mod is licensed under MIT, a free and open-source license. For more information, please see the [license file](https://github.com/swakoza/Puberty-Mod/blob/main/LICENSE).

## Stored player settings

Existing player files remain in `config/pubertymod/<uuid>.json`, presets in
`config/pubertymod/presets`, and custom sounds in `config/pubertymod/hurt_sounds`.
The legacy `config/SwakozaPubertyMod` directory is still supported for migration.
Player editor presets save Appearance, Physics, and Sounds. Existing breast-only presets
remain usable and only change the fields they contain.
Both a single string and a list under `custom_hurt_sound` are readable.
Loading player settings does not rewrite files. Saving retains unknown fields and
writes through a temporary file; malformed originals are preserved and reported in the log.
Run `verifyConfigurationStorage` to check persistence using temporary files.

Versioned builds are available through `build copyVersionedJar -PtargetMinecraftVersion=<version>`.
Release JARs omit Java debug tables while retaining reviewable bytecode and mod metadata.
Supported release targets: 1.21.1, 1.21.4, 1.21.6, 1.21.10, 1.21.11, 26.1 and 26.1.2.

## Physics on versioned builds

All release targets use an attached three-axis breast deformation. The rear face
stays fixed to the torso; skin, jacket, armor, glint and trim share the deformation.
The upper cap is omitted and all layers stay inside the torso at its back and top.
Frame interpolation and torso transformations use direct, remapped Minecraft calls.
Deferred renderers capture each breast's displacement before submitting geometry.

Optional animation API versions used to compile the adapters:

- 1.21.1: PlayerAnimator 2.0.4+1.21.1 and PAL 1.1.6+mc.1.21.1.
- 1.21.4: PlayerAnimator 2.0.5+1.21.4.
- 1.21.6: PlayerAnimator 2.0.2+1.21.7, published for both 1.21.6 and 1.21.7.
- 1.21.11: PAL 1.1.7+mc.1.21.11 (1.1.7 or later within the 1.1 API).
- 26.1 / 26.1.2: PAL 1.2.6+mc.26.1 (1.2.6 or later within the 1.2 API).

Animation libraries remain optional and are not bundled. When PAL's API is older
or outside the supported family, the mod uses the rendered torso pose and logs a
single compatibility message instead of invoking incompatible library methods.
The rendered pose cache expires after two ticks and is not written to player data.
Run `verifyBreastDeformation` and `verifyTorsoPose` to check attachment, lighting
and gravity projection for held forward and sideways bends on every target.
