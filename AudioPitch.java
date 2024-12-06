import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
public class AudioPitch {
    private JFrame frame;
    private JButton chooseButton, playButton, pauseButton, stopButton;
    private JSlider pitchSlider, tempoSlider; // Added tempo slider
    private File audioFile;
    private Clip audioClip; // For WAV playback
    private MediaPlayer javafxPlayer; // For MP3 playback with JavaFX
    private boolean isPaused = false;
    private long pausePosition = 0;

    private JFXPanel jfxPanel; // For embedding JavaFX into Swing

    public AudioPitch() {
        // Initialize JavaFX panel
        jfxPanel = new JFXPanel(); // Initializes JavaFX
        Platform.runLater(() -> {
            // JavaFX initialization will happen here
        });

        // Create the frame
        frame = new JFrame("Audio Player");
        frame.setSize(400, 300);  // Adjusted size for the slider
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        // Create buttons
        chooseButton = new JButton("Choose Audio");
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");

        // Create pitch slider
        pitchSlider = new JSlider(JSlider.HORIZONTAL, -12, 12, 0); // Range for pitch (-12 to 12 semitones)
        pitchSlider.setMajorTickSpacing(6);
        pitchSlider.setMinorTickSpacing(1);
        pitchSlider.setPaintTicks(true);
        pitchSlider.setPaintLabels(true);

        // Create tempo slider (Range: 0.5x to 2x speed)
        tempoSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100); // Tempo from 0.5x to 2x
        tempoSlider.setMajorTickSpacing(50);
        tempoSlider.setMinorTickSpacing(10);
        tempoSlider.setPaintTicks(true);
        tempoSlider.setPaintLabels(true);

        // Add buttons and sliders to the frame
        frame.add(chooseButton);
        frame.add(playButton);
        frame.add(pauseButton);
        frame.add(stopButton);
        frame.add(new JLabel("Pitch Shift"));
        frame.add(pitchSlider);
        frame.add(new JLabel("Tempo"));
        frame.add(tempoSlider);

        // Set button actions
        chooseButton.addActionListener(new ChooseAudioListener());
        playButton.addActionListener(new PlayAudioListener());
        pauseButton.addActionListener(new PauseAudioListener());
        stopButton.addActionListener(new StopAudioListener());

        // Add ChangeListener to the pitch and tempo sliders
        pitchSlider.addChangeListener(e -> adjustAudio());
        tempoSlider.addChangeListener(e -> adjustAudio());

        // Disable play, pause, and stop buttons until an audio file is chosen
        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        // Show the frame
        frame.setVisible(true);
    }

    // Listener for the "Choose Audio" button
    private class ChooseAudioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select an Audio File");
            int result = fileChooser.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                audioFile = fileChooser.getSelectedFile();
                JOptionPane.showMessageDialog(frame, "Selected file: " + audioFile.getName());
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }
        }
    }

    // Listener for the "Play" button
    private class PlayAudioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (audioFile == null) {
                JOptionPane.showMessageDialog(frame, "Please choose an audio file first!");
                return;
            }

            String fileName = audioFile.getName().toLowerCase();
            if (fileName.endsWith(".wav")) {
                playWav();
            } else if (fileName.endsWith(".mp3")) {
                playMp3();
            } else {
                JOptionPane.showMessageDialog(frame, "Unsupported file format!");
            }
        }
    }

    // Play WAV file
    private void playWav() {
        try {
            if (audioClip == null || !audioClip.isOpen()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);
            }

            if (isPaused) {
                audioClip.setMicrosecondPosition(pausePosition); // Resume from pause position
            } else {
                audioClip.setFramePosition(0); // Start from the beginning
            }

            // Adjust pitch and tempo
            setPitchAndTempo(audioClip, pitchSlider.getValue(), tempoSlider.getValue());

            audioClip.start();
            isPaused = false;
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error playing WAV audio: " + ex.getMessage());
        }
    }

    // Play MP3 file
    private void playMp3() {
        try {
            // JavaFX MP3 Playback
            Platform.runLater(() -> {
                try {
                    URI fileUri = audioFile.toURI();
                    Media media = new Media(fileUri.toString());
                    javafxPlayer = new MediaPlayer(media);
                    javafxPlayer.play();

                    // Adjust pitch and tempo
                    javafxPlayer.setRate(getPitchRate(pitchSlider.getValue()) * (tempoSlider.getValue() / 100.0));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error initializing JavaFX MP3 playback: " + ex.getMessage());
                }
            });

            playButton.setEnabled(false);
            stopButton.setEnabled(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error initializing MP3 playback: " + ex.getMessage());
        }
    }

    // Adjust pitch and tempo for WAV
    private void setPitchAndTempo(Clip clip, int pitchShift, int tempo) {
        if (clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
            FloatControl sampleRateControl = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE);
            float currentSampleRate = sampleRateControl.getValue();

            // Adjust pitch by changing sample rate based on pitch shift
            float newSampleRate = currentSampleRate * (float) Math.pow(2, pitchShift / 12.0);

            // Adjust tempo by changing sample rate based on tempo slider
            newSampleRate *= (tempo / 100.0); // Adjust tempo (50 -> 0.5x speed, 200 -> 2x speed)

            sampleRateControl.setValue(newSampleRate);
        }
    }

    // Get pitch adjustment rate for MP3
    private double getPitchRate(int pitchShift) {
        return Math.pow(2, pitchShift / 12.0);
    }

    // Adjust audio for both pitch and tempo changes
    private void adjustAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            // Adjust pitch and tempo for WAV
            setPitchAndTempo(audioClip, pitchSlider.getValue(), tempoSlider.getValue());
        }

        if (javafxPlayer != null && javafxPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            // Adjust pitch and tempo for MP3
            javafxPlayer.setRate(getPitchRate(pitchSlider.getValue()) * (tempoSlider.getValue() / 100.0));
        }
    }

    // Pause audio
    private class PauseAudioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (audioClip != null && audioClip.isRunning()) {
                pausePosition = audioClip.getMicrosecondPosition();
                audioClip.stop();
                isPaused = true;
                playButton.setEnabled(true);
                stopButton.setEnabled(true);
            }

            if (javafxPlayer != null && javafxPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                javafxPlayer.pause();
            }
        }
    }

    // Stop audio
    private class StopAudioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (audioClip != null) {
                audioClip.stop();
                audioClip.close();
                isPaused = false;
                pausePosition = 0;
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }

            if (javafxPlayer != null) {
                javafxPlayer.stop();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AudioPitch::new);
    }
}
