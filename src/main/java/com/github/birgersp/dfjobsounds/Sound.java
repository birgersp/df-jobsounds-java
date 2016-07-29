package com.github.birgersp.dfjobsounds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound {

    private final byte[] data;

    public Sound(byte[] data) throws UnsupportedAudioFileException, IOException {
        this.data = data;
        AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
    }

    public void play(float volumeGain_db) throws LineUnavailableException {
        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            Clip clip = AudioSystem.getClip();
            clip.open(inputStream);
            if (volumeGain_db != 0) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volumeGain_db);
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (IOException | UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
    }

    public void play() throws LineUnavailableException {
        play(0f);
    }
}
