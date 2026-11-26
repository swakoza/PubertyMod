![Puberty Banner](src/main/resources/assets/swakozas_puberty_mod/banner.png)
# Puberty

**Current version: 1.3**

The display name is **Puberty**. The Fabric ID, resource namespace and existing
configuration paths remain unchanged so installed settings and integrations continue to work.

This update adds waist and hip customization, movement-based breast physics, and a redesigned player list and editor. Presets can be managed from the editor alongside appearance, physics, and sound settings.

### Description
This mod adds the ability to visually customize other players' models by changing their gender.<br>
When the gender is changed to female or “other,” a customizable bust is added to the character model.<br>
You can also set custom damage sounds for players with the gender set to female or “other” if you wish.

## Default Controls

G - Open Puberty menu<br>
H - Opens the edit menu when you hover over a player 

## Player previews

Breast physics reacts to jumps and torso animations
from EmoteCraft/playerAnimator or Player Animation Library when those mods are installed.
The animation libraries are optional at runtime.
The editor locks the preview camera while showing actions and emotes of loaded players.
Players outside client tracking range use a static preview of their synced appearance;
their live actions are not available until the server tracks them again.
H selects a visible player within 32 blocks (walls block selection), and also opens
the hovered row in the player list when the search field is not focused.

## License

Puberty is licensed under MIT, a free and open-source license. For more information, please see the [license file](https://github.com/swakoza/PubertyMod/blob/main/LICENSE).

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

## Build from source

Install JDK 25 and set `JAVA_HOME` to its installation directory. The checked-in Gradle wrapper downloads Gradle and the build dependencies on the first run. The 1.21.x builds target Java 21, and the 26.x builds target Java 25.

```sh
git clone https://github.com/swakoza/PubertyMod.git
cd PubertyMod
./gradlew build copyVersionedJar -PtargetMinecraftVersion=1.21.11
```

On Windows, run `.\gradlew.bat build copyVersionedJar -PtargetMinecraftVersion=1.21.11` from the cloned repository. The installable JAR is written to `dist/1.21.11/Puberty-fabric-1.21.11-1.3.jar`. Use the JAR from `dist/<version>` in the Fabric `mods` directory with the corresponding Fabric API.

You can set `targetMinecraftVersion` to `1.21.1`, `1.21.4`, `1.21.6`, `1.21.10`, `1.21.11`, `26.1`, `26.1.2`, `26.2`, or `26.3`. To build all nine targets, run `./gradlew buildAllVersions` or `.\gradlew.bat buildAllVersions` on Windows. GitHub Actions also builds all nine targets and publishes the resulting JARs as workflow artifacts.

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
- 1.21.10: PAL 1.1.3+mc.1.21.9.
- 1.21.11: PAL 1.1.10+mc.1.21.11.
- 26.x: PAL 1.2.6+mc.26.1.

Animation libraries remain optional and are not bundled. When PAL's API is older
or outside the supported family, the mod uses the rendered torso pose and logs a
single compatibility message instead of invoking incompatible library methods.
The rendered pose cache expires after two ticks and is not written to player data.
Run `verifyBreastDeformation` and `verifyTorsoPose` to check attachment, lighting
and gravity projection for held forward and sideways bends on every target.
