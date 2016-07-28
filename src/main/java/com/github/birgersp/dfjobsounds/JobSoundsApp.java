package com.github.birgersp.dfjobsounds;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class JobSoundsApp extends Thread {

    private static final String APP_NAME = "JobSounds - Dwarf Fortress sound utility";
    private static final String SCRIPT_NAME = "jobsounds";

    private static Clip[] getClips(SoundType type) throws Exception {

        CodeSource src = JobSoundsApp.class.getProtectionDomain().getCodeSource();
        List<String> fileNames = new ArrayList<>();

        if (src != null) {
            URL jar = src.getLocation();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry ze;

            while ((ze = zip.getNextEntry()) != null) {
                String entryName = ze.getName();
                if (entryName.startsWith("sounds/" + type.toString().toLowerCase()) && entryName.endsWith(".wav")) {
                    fileNames.add(entryName);
                }
            }
        }

        ArrayList<Clip> clips = new ArrayList<>();
        for (String fileName : fileNames) {
            InputStream stream = JobSoundsApp.class.getResourceAsStream("/" + fileName);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(stream));
            clips.add(AudioSystem.getClip());
            clips.get(clips.size() - 1).open(audioStream);
            System.out.println("Added " + type + " sound: " + fileName);
        }
        return clips.toArray(new Clip[clips.size()]);
    }

    public static void main(String[] args) {

        try {
            System.out.println(APP_NAME);
            String dfDir = args.length > 0 ? args[0] : System.getProperty("user.dir");
            System.out.println("Assumed Dwarf Fortress directory: \"" + dfDir + "\"");
            JobSoundsApp ls = new JobSoundsApp(dfDir);
            ls.installScript();
            ls.start();
            ls.runScript();
            ls.join();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private final String dfDir;
    private final HashMap<Integer, Long> dwarfSoundClocks;
    private boolean running;
    private Socket socket;
    private final HashMap<SoundType, Clip[]> soundClips;

    public JobSoundsApp(String dfDir) throws Exception {

        this.dfDir = dfDir;
        dwarfSoundClocks = new HashMap<>();
        soundClips = new HashMap<>();
        for (SoundType soundType : SoundType.values()) {
            soundClips.put(soundType, getClips(soundType));
        }
    }

    private void handleLine(String line) {

        String split[] = line.split(" ");
        if (split.length != 2) {
            return;
        }
        int dwarfId = Integer.parseInt(split[0]);
        int labourType = Integer.parseInt(split[1]);

        SoundType soundType = SoundType.fromId(labourType);
        long currentTime = System.currentTimeMillis();
        if (dwarfSoundClocks.containsKey(dwarfId)) {
            if (currentTime - dwarfSoundClocks.get(dwarfId) < soundType.getMinDelay()) {
                return;
            }
        }
        dwarfSoundClocks.put(dwarfId, currentTime);
        Clip[] clips = soundClips.get(soundType);
        int index = (int) (Math.random() * clips.length);
        Clip clip = clips[index];
        if (!clip.isRunning()) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void installScript() throws Exception {

        File scriptsDir = new File(dfDir + File.separator + "hack" + File.separator + "scripts");

        if (!scriptsDir.exists()) {
            throw new Exception(scriptsDir.toString() + " does not exist. Please make sure the specified Dwarf Fortress directory is correct.");
        }

        File destination = new File(scriptsDir.getAbsolutePath() + File.separator + SCRIPT_NAME + ".lua");
        InputStream in = JobSoundsApp.class.getResourceAsStream("/dfhack-scripts/" + SCRIPT_NAME + ".lua");

        if (!destination.exists()) {
            destination.createNewFile();
        }

        System.out.println("Installing script to: " + destination);
        Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void run() {

        running = true;
        int index = 0;
        byte buffer[] = new byte[64];
        try {
            ServerSocket server = new ServerSocket(56730);
            while (running) {
                try {
                    System.out.println("Connecting to DFHack...");
                    socket = server.accept();
                    System.out.println("Connected");
                    DataInputStream input = new DataInputStream(socket.getInputStream());

                    while (!socket.isClosed()) {
                        buffer[index] = input.readByte();
                        if (buffer[index] == '\n') {
                            handleLine(new String(buffer, 0, index, Charset.defaultCharset()));
                            index = 0;
                        } else {
                            index++;
                        }
                    }
                } catch (EOFException e) {
                    System.out.println("Connection lost");
                } finally {
                    socket.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        running = false;
    }

    private void runScript() throws Exception {

        String cmd = dfDir + File.separator + "dfhack-run " + SCRIPT_NAME;
        System.out.println("Executing: " + cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
    }

    private enum SoundType {

        DIG(600, 3, 8), BUILD(900, 68), CHOP(1000, 9);

        private final int minDelay;
        private final int[] ids;

        private SoundType(int minDelay, int... ids) {

            this.minDelay = minDelay;
            this.ids = ids;
        }

        public int getMinDelay() {
            return minDelay;
        }

        public static SoundType fromId(int id) {
            for (SoundType type : values()) {
                for (int typeId : type.ids) {
                    if (id == typeId) {
                        return type;
                    }
                }
            }
            return null;
        }
    }
}
