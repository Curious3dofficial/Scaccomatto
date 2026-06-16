import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager {
    private static boolean soundEnabled = true;
    private static final Set<SoundHandle> activeSounds = ConcurrentHashMap.newKeySet();

    // ── Handle returned by playSound() so callers can stop playback ───────
    public static class SoundHandle {
        private final Clip    clip;    // non-null for WAV
        private final Process proc;    // non-null for MP3

        SoundHandle(Clip clip) {
            this.clip = clip;
            this.proc = null;
            activeSounds.add(this);
        }

        SoundHandle(Process proc) {
            this.proc = proc;
            this.clip = null;
            activeSounds.add(this);
            Thread cleanup = new Thread(() -> {
                try {
                    proc.waitFor();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    activeSounds.remove(this);
                }
            }, "sound-process-cleanup");
            cleanup.setDaemon(true);
            cleanup.start();
        }

        /** Immediately stop and release the sound. Safe to call more than once. */
        public void stop() {
            if (clip != null) {
                try { clip.stop(); clip.close(); } catch (Exception ignored) {}
            }
            if (proc != null) {
                try { proc.destroy(); } catch (Exception ignored) {}
            }
            activeSounds.remove(this);
        }
    }

    public enum SoundType {
        MOVE_SELF("move-self.wav"),
        CAPTURE("capture.wav"),
        CASTLE("castle.wav"),
        PROMOTE("promote.wav"),
        CHECK("move-check.wav"),
        GAME_START("after pressing start game.wav"),
        GAME_END("game-end.wav"),
        ILLEGAL("illegal.wav"),
        TEN_SECONDS("tensecondsleftonclock.wav"),
        ROOK_CHECK("rookcheck.wav"),
        CHECKMATE_WHITE("Checkmateyay1.mp3"),
        CHECKMATE_BLACK("Checkmateyay2.mp3");

        private final String fileName;
        SoundType(String fileName) { this.fileName = fileName; }
        public String getFileName() { return fileName; }
    }

    /**
     * Play a sound and return a SoundHandle that can stop it.
     * Returns null if the sound file is not found or sound is disabled.
     */
    public static SoundHandle playSound(SoundType soundType) {
        if (!soundEnabled) return null;

        // We need to return the handle synchronously, so resolve the file
        // on the calling thread, then start playback on a background thread.
        String fileName = soundType.getFileName();
        File   soundFile = resolveFile(fileName);
        if (soundFile == null) return null;

        if (fileName.toLowerCase().endsWith(".mp3")) {
            // Start the native process and return its handle immediately
            Process proc = startMP3Process(soundFile);
            return (proc != null) ? new SoundHandle(proc) : null;
        } else {
            // WAV: open the Clip, start it, return the handle
            try {
                AudioInputStream ain = AudioSystem.getAudioInputStream(soundFile.toURI().toURL());
                Clip clip = AudioSystem.getClip();
                clip.open(ain);
                clip.start();
                SoundHandle handle = new SoundHandle(clip);
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        activeSounds.remove(handle);
                    }
                });
                return handle;
            } catch (Exception ignored) { return null; }
        }
    }

    /** Play a sound from src/assets/extras (or /assets/extras on classpath). */
    public static SoundHandle playExtraSound(String fileName) {
        if (!soundEnabled || fileName == null || fileName.trim().isEmpty()) return null;
        File soundFile = resolveFileFromExtra(fileName.trim());
        if (soundFile == null) return null;

        return playResolvedFile(fileName, soundFile);
    }

    /** Play a sound from src/assets/spell chess sounds (or /assets/spell chess sounds on classpath). */
    public static SoundHandle playSpellChessSound(String fileName) {
        return playSpellChessSound(fileName, 1.0f);
    }

    public static SoundHandle playSpellChessSound(String fileName, float volume) {
        return playSpellChessSound(fileName, volume, 0);
    }

    public static SoundHandle playSpellChessSound(String fileName, float volume, int fadeInMs) {
        if (!soundEnabled || fileName == null || fileName.trim().isEmpty()) return null;
        File soundFile = resolveFileFromSpellChessSounds(fileName.trim());
        if (soundFile == null) return null;

        return playResolvedFile(fileName, soundFile, volume, fadeInMs);
    }

    private static SoundHandle playResolvedFile(String fileName, File soundFile) {
        return playResolvedFile(fileName, soundFile, 1.0f);
    }

    private static SoundHandle playResolvedFile(String fileName, File soundFile, float volume) {
        return playResolvedFile(fileName, soundFile, volume, 0);
    }

    private static SoundHandle playResolvedFile(String fileName, File soundFile, float volume, int fadeInMs) {
        String lower = fileName.toLowerCase();
        float vol = Math.max(0f, Math.min(1f, volume));
        if (lower.endsWith(".mp3")) {
            Process proc = startMP3Process(soundFile, vol, fadeInMs);
            return (proc != null) ? new SoundHandle(proc) : null;
        } else {
            try {
                AudioInputStream ain = AudioSystem.getAudioInputStream(soundFile.toURI().toURL());
                Clip clip = AudioSystem.getClip();
                clip.open(ain);
                if (fadeInMs > 0) {
                    fadeClipIn(clip, vol, fadeInMs);
                } else {
                    setClipVolume(clip, vol);
                }
                clip.start();
                SoundHandle handle = new SoundHandle(clip);
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        activeSounds.remove(handle);
                    }
                });
                return handle;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /** Launch a video from assets/extras with the OS default app (non-blocking). */
    public static void playExtraVideo(String fileName) {
        if (!soundEnabled || fileName == null || fileName.trim().isEmpty()) return;
        File videoFile = resolveFileFromExtra(fileName.trim());
        if (videoFile == null) return;
        try {
            String abs = videoFile.getAbsolutePath().replace("'", "''");
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("powershell", "-Command",
                        "Start-Process -FilePath '" + abs + "'").start();
                return;
            }
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(videoFile);
            }
        } catch (Exception ignored) {
        }
    }

    /** Convenience stop — null-safe. */
    public static void stopSound(SoundHandle handle) {
        if (handle != null) handle.stop();
    }

    public static void stopAllSounds() {
        for (SoundHandle handle : activeSounds.toArray(new SoundHandle[0])) {
            handle.stop();
        }
        activeSounds.clear();
    }

    // ── File resolver ──────────────────────────────────────────────────────
    private static File resolveFile(String fileName) {
        String[] candidates = {
            "src/assets/sounds/" + fileName,
            "Scaccomatto/src/assets/sounds/" + fileName,
            "assets/sounds/" + fileName
        };
        for (String p : candidates) {
            File f = new File(p);
            if (f.exists()) return f;
        }
        try {
            URL res = SoundManager.class.getResource("/assets/sounds/" + fileName);
            if (res != null) {
                if (res.getProtocol().equals("file")) return new File(res.toURI());
                String ext = fileName.substring(fileName.lastIndexOf('.'));
                File tmp = File.createTempFile("chess_snd_", ext);
                tmp.deleteOnExit();
                try (java.io.InputStream in  = res.openStream();
                     java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                    in.transferTo(out);
                }
                return tmp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static File resolveFileFromExtra(String fileName) {
        String[] candidates = {
            "src/assets/extras/" + fileName,
            "Scaccomatto/src/assets/extras/" + fileName,
            "Scaccomatto_final/Scaccomatto/src/assets/extras/" + fileName,
            "assets/extras/" + fileName
        };
        for (String p : candidates) {
            File f = new File(p);
            if (f.exists()) return f;
        }
        try {
            URL res = SoundManager.class.getResource("/assets/extras/" + fileName);
            if (res != null) {
                if (res.getProtocol().equals("file")) return new File(res.toURI());
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".tmp";
                File tmp = File.createTempFile("chess_extra_", ext);
                tmp.deleteOnExit();
                try (java.io.InputStream in  = res.openStream();
                     java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                    in.transferTo(out);
                }
                return tmp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static File resolveFileFromSpellChessSounds(String fileName) {
        String[] candidates = {
            "src/assets/spell chess sounds/" + fileName,
            "Scaccomatto/src/assets/spell chess sounds/" + fileName,
            "Scaccomatto_final/Scaccomatto/src/assets/spell chess sounds/" + fileName,
            "assets/spell chess sounds/" + fileName
        };
        for (String p : candidates) {
            File f = new File(p);
            if (f.exists()) return f;
        }
        try {
            URL res = SoundManager.class.getResource("/assets/spell chess sounds/" + fileName);
            if (res != null) {
                if (res.getProtocol().equals("file")) return new File(res.toURI());
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".tmp";
                File tmp = File.createTempFile("chess_spell_snd_", ext);
                tmp.deleteOnExit();
                try (java.io.InputStream in  = res.openStream();
                     java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                    in.transferTo(out);
                }
                return tmp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── MP3 via OS native player ───────────────────────────────────────────
    private static void setClipVolume(Clip clip, float volume) {
        if (clip == null) return;
        float vol = Math.max(0f, Math.min(1f, volume));
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = (vol <= 0f) ? gain.getMinimum() : (float) (20.0 * Math.log10(vol));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), vol)));
            }
        } catch (Exception ignored) {
        }
    }

    private static void fadeClipIn(Clip clip, float targetVolume, int fadeInMs) {
        if (clip == null) return;
        float target = Math.max(0f, Math.min(1f, targetVolume));
        int duration = Math.max(1, fadeInMs);
        setClipVolume(clip, 0.01f);
        javax.swing.Timer fadeTimer = AnimationTiming.createUiTimer(null);
        long startedAtNanos = System.nanoTime();
        fadeTimer.addActionListener(e -> {
            float t = AnimationTiming.progress(startedAtNanos, duration);
            float eased = (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
            setClipVolume(clip, Math.max(0.01f, target * eased));
            if (t >= 1f) {
                setClipVolume(clip, target);
                fadeTimer.stop();
            }
        });
        fadeTimer.start();
    }

    private static Process startMP3Process(File file) {
        return startMP3Process(file, 1.0f);
    }

    private static Process startMP3Process(File file, float volume) {
        return startMP3Process(file, volume, 0);
    }

    private static Process startMP3Process(File file, float volume, int fadeInMs) {
        String abs = file.getAbsolutePath();
        String os  = System.getProperty("os.name", "").toLowerCase();
        String[] cmd;
        float vol = Math.max(0f, Math.min(1f, volume));
        float fadeSeconds = Math.max(0f, fadeInMs / 1000f);

        try {
            if (os.contains("win")) {
                String ps = String.format(
                    "Add-Type -AssemblyName presentationCore;" +
                    "$m=New-Object System.Windows.Media.MediaPlayer;" +
                    "$m.Open([uri]'%s');" +
                    "$target=%s;" +
                    "$fadeMs=%d;" +
                    "$m.Volume=if($fadeMs -gt 0){0.0}else{$target};" +
                    "$m.Play();" +
                    "if($fadeMs -gt 0){" +
                    "  $steps=16;" +
                    "  for($i=1;$i -le $steps;$i++){" +
                    "    $t=$i/$steps;" +
                    "    $m.Volume=$target*(0.5-0.5*[Math]::Cos([Math]::PI*$t));" +
                    "    Start-Sleep -Milliseconds ([int]($fadeMs/$steps));" +
                    "  }" +
                    "  $m.Volume=$target;" +
                    "}" +
                    "Start-Sleep 1;" +
                    "$d=$m.NaturalDuration;" +
                    "$secs=if($d.HasTimeSpan){[int]$d.TimeSpan.TotalSeconds+1}else{10};" +
                    "Start-Sleep $secs;" +
                    "$m.Close()",
                    abs.replace("\\", "/").replace("'", "\\'"),
                    String.format(java.util.Locale.US, "%.3f", vol),
                    Math.max(0, fadeInMs)
                );
                cmd = new String[]{"powershell", "-WindowStyle", "Hidden", "-Command", ps};
            } else if (os.contains("mac")) {
                cmd = new String[]{"afplay", "-v", String.format(java.util.Locale.US, "%.3f", vol), abs};
            } else {
                if (fadeInMs > 0 && cmdExists("ffplay")) {
                    cmd = new String[]{
                        "ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet",
                        "-volume", String.valueOf(Math.round(100 * vol)),
                        "-af", "afade=t=in:st=0:d=" + String.format(java.util.Locale.US, "%.3f", fadeSeconds),
                        abs
                    };
                }
                else if (cmdExists("mpg123"))  cmd = new String[]{"mpg123", "-q", "-f", String.valueOf(Math.round(32768 * vol)), abs};
                else if (cmdExists("ffplay"))  cmd = new String[]{"ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", "-volume", String.valueOf(Math.round(100 * vol)), abs};
                else if (cmdExists("cvlc"))    cmd = new String[]{"cvlc", "--intf", "dummy", "--play-and-exit", "--gain", String.format(java.util.Locale.US, "%.3f", vol), abs};
                else return null;
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            // drain stdout so the process never blocks on a full pipe
            new Thread(() -> {
                try { proc.getInputStream().transferTo(java.io.OutputStream.nullOutputStream()); }
                catch (Exception ignored) {}
            }).start();
            return proc;

        } catch (Exception ignored) { return null; }
    }

    private static boolean cmdExists(String cmd) {
        try { return new ProcessBuilder("which", cmd).start().waitFor() == 0; }
        catch (Exception e) { return false; }
    }

    // ── Misc ───────────────────────────────────────────────────────────────
    public static void setSoundEnabled(boolean enabled) { soundEnabled = enabled; }
    public static boolean isSoundEnabled()              { return soundEnabled; }

    public static void playMoveSound(boolean isCapture, boolean isCastle,
                                     boolean isPromotion, boolean isCheck) {
        if      (isCheck)      playSound(SoundType.CHECK);
        else if (isPromotion)  playSound(SoundType.PROMOTE);
        else if (isCastle)     playSound(SoundType.CASTLE);
        else if (isCapture)    playSound(SoundType.CAPTURE);
        else                   playSound(SoundType.MOVE_SELF);
    }
}
