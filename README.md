![Puberty Banner](src/main/resources/assets/swakozas_puberty_mod/banner.png)

# Puberty

Puberty is a Fabric fork of Wildfire's Gender Mod that lets you customize player models, movement physics, and hurt sounds. [Download Puberty 1.3](https://github.com/swakoza/PubertyMod/releases/tag/v1.3)

## Features

- **Appearance.** Choose Male, Female, or Other for each player. Male uses the standard Minecraft body. Female and Other can customize breast size, separation, height, depth, and rotation, along with waist width, waist height, and hip width.
- **Breast physics.** Adjust bounce, softness, damping, and motion range. Tune the response to steps, jumps, side-to-side movement, turns, and gravity. Set the two sides to move independently. Control breast visibility and movement under chest armor. Compatible versions of PlayerAnimator or Player Animation Library can feed torso animations into the physics.
- **Hurt sounds.** Enable the built-in female hurt sound for Female and Other and adjust its volume. Add Ogg Vorbis `.ogg` files to `config/pubertymod/hurt_sounds` and select them per player. If you select several files, the mod picks one at random. You can also allow successive custom sounds to overlap.
- **Presets.** Save, apply, overwrite, or delete a named set of appearance, physics, and sound settings.
- **Player browser.** Search online players by name, filter to nearby players, and see a 3D preview before editing.

## Controls and multiplayer

- **G** opens the player browser. Select yourself or another player and double-click the row to edit.
- **H** opens the editor for the player you are looking at.

Edits you make to other players affect your view. To share your own appearance and physics settings in multiplayer, install Puberty on the Fabric server and on the clients that should see them. Custom sound files are loaded from each client's local folder.

## Install

Install Fabric Loader and Fabric API for your Minecraft version, then put the matching Puberty JAR in `mods`.

| Minecraft versions | Java |
| --- | --- |
| 1.21.1, 1.21.4, 1.21.6, 1.21.10, 1.21.11 | 21 |
| 26.1, 26.1.2, 26.2, 26.3 | 25 |

## Build from source

Use JDK 25 and set `JAVA_HOME` to its installation directory.

```sh
git clone --branch v1.3 https://github.com/swakoza/PubertyMod.git
cd PubertyMod
./gradlew build copyVersionedJar -PtargetMinecraftVersion=1.21.11
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`. Choose any Minecraft version listed above. Run `./gradlew buildAllVersions` to build all nine targets. The JARs go in `dist/<minecraft-version>/`.

[MIT license](LICENSE)
