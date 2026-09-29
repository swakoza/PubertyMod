package com.swakoza.pubertymod.main;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.sound.OggAudioStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class CustomHurtSoundManager {
    public static final String DEFAULT_SOUND_FILE = "default.ogg";
    private static final String DEFAULT_SOUND_RESOURCE = "/assets/swakozas_puberty_mod/sounds/female_damage.ogg";
    private static final Map<UUID, ActiveSound> ACTIVE_SOUNDS = new ConcurrentHashMap<>();
    private static final float HEARING_DISTANCE = 16.0F;

    private CustomHurtSoundManager() {}

    public static Path getHurtSoundsDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("pubertymod").resolve("hurt_sounds");
    }

    public static void ensureSoundDirectory() {
        Path directory = getHurtSoundsDirectory();
        try {
            Files.createDirectories(directory);
            Path defaultSound = directory.resolve(DEFAULT_SOUND_FILE);
            if (!Files.exists(defaultSound)) {
                try (InputStream stream = CustomHurtSoundManager.class.getResourceAsStream(DEFAULT_SOUND_RESOURCE)) {
                    if (stream != null) {
                        Files.copy(stream, defaultSound);
                    }
                }
            }
        } catch (IOException e) {
            SwakozaPubertyMod.LOGGER.warn("Failed to prepare custom hurt sound directory", e);
        }
    }

    public static List<String> listSoundFiles() {
        ensureSoundDirectory();
        try (Stream<Path> files = Files.list(getHurtSoundsDirectory())) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            SwakozaPubertyMod.LOGGER.warn("Failed to list custom hurt sounds", e);
            return List.of();
        }
    }

    public static boolean isSupportedOggVorbis(String fileName) {
        if (!isSoundFilePresent(fileName)) {
            return false;
        }

        Path sound = getHurtSoundsDirectory().resolve(fileName).normalize();
        try (InputStream input = Files.newInputStream(sound)) {
            byte[] header = input.readNBytes(128);
            return new String(header, java.nio.charset.StandardCharsets.ISO_8859_1).contains("vorbis");
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean playRandom(PlayerEntity source, List<String> fileNames, float volume, boolean overlay) {
        if (fileNames == null || fileNames.isEmpty()) {
            return false;
        }

        List<String> existing = new ArrayList<>();
        for (String fileName : fileNames) {
            if (isSupportedOggVorbis(fileName)) {
                existing.add(fileName);
            }
        }
        if (existing.isEmpty()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world != com.swakoza.pubertymod.compat.EntityCompat.getWorld(source)) {
            return true;
        }
        double distance = Math.sqrt(source.squaredDistanceTo(client.player));
        if (distance >= HEARING_DISTANCE) {
            return true;
        }
        float attenuation = (float) (1.0 - distance / HEARING_DISTANCE);
        String fileName = existing.get(ThreadLocalRandom.current().nextInt(existing.size()));
        return play(source.getUuid(), fileName, volume * attenuation * attenuation, overlay);
    }

    private static boolean isSoundFilePresent(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        Path sound = getHurtSoundsDirectory().resolve(fileName).normalize();
        return sound.startsWith(getHurtSoundsDirectory()) && Files.isRegularFile(sound);
    }

    private static boolean play(UUID playerId, String fileName, float volume, boolean overlay) {
        Path sound = getHurtSoundsDirectory().resolve(fileName).normalize();
        ActiveSound activeSound = new ActiveSound();
        if (!overlay && playerId != null) {
            ActiveSound previousSound = ACTIVE_SOUNDS.put(playerId, activeSound);
            if (previousSound != null) {
                previousSound.stop();
            }
        }
        Thread thread = new Thread(() -> {
            try {
                playBlocking(sound, volume, activeSound);
            } finally {
                if (!overlay && playerId != null) {
                    ACTIVE_SOUNDS.remove(playerId, activeSound);
                }
            }
        }, "PubertyMod-CustomHurtSound");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private static void playBlocking(Path sound, float volume, ActiveSound activeSound) {
        try (InputStream input = Files.newInputStream(sound);
             OggAudioStream stream = new OggAudioStream(input)) {
            AudioFormat format = stream.getFormat();
            ByteBuffer buffer = stream.readAll();
            buffer.rewind();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            applyVolume(data, format, volume);

            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                activeSound.setLine(line);
                if (activeSound.isStopped()) {
                    return;
                }
                line.open(format);
                line.start();
                int offset = 0;
                while (!activeSound.isStopped() && offset < data.length) {
                    int written = line.write(data, offset, Math.min(4096, data.length - offset));
                    if (written <= 0) {
                        break;
                    }
                    offset += written;
                }
                if (!activeSound.isStopped()) {
                    line.drain();
                }
            }
        } catch (Exception e) {
            SwakozaPubertyMod.LOGGER.warn("Failed to play custom hurt sound {}", sound, e);
        }
    }

    private static void applyVolume(byte[] data, AudioFormat format, float volume) {
        float clampedVolume = Math.max(0.0F, Math.min(1.5F, volume));
        if (Math.abs(clampedVolume - 1.0F) < 0.001F || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()) || format.getSampleSizeInBits() != 16) {
            return;
        }

        boolean bigEndian = format.isBigEndian();
        for (int i = 0; i + 1 < data.length; i += 2) {
            int low = bigEndian ? data[i + 1] & 0xFF : data[i] & 0xFF;
            int high = bigEndian ? data[i] : data[i + 1];
            short sample = (short) ((high << 8) | low);
            short scaled = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * clampedVolume)));
            if (bigEndian) {
                data[i] = (byte) ((scaled >> 8) & 0xFF);
                data[i + 1] = (byte) (scaled & 0xFF);
            } else {
                data[i] = (byte) (scaled & 0xFF);
                data[i + 1] = (byte) ((scaled >> 8) & 0xFF);
            }
        }
    }

    private static final class ActiveSound {
        private volatile SourceDataLine line;
        private volatile boolean stopped;

        void setLine(SourceDataLine line) {
            this.line = line;
            if (stopped) {
                stop();
            }
        }

        boolean isStopped() {
            return stopped;
        }

        void stop() {
            stopped = true;
            SourceDataLine currentLine = line;
            if (currentLine != null) {
                currentLine.stop();
                currentLine.close();
            }
        }
    }
}
