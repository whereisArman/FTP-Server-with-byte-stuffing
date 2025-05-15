import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class FileReceiverGUI extends JFrame {
    private JTextArea displayArea;
    private JButton startButton;

    public FileReceiverGUI() {
        setTitle("File Receiver with Byte De-stuffing");
        setSize(600, 400);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Center: Display Area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: Start Receiving Button
        startButton = new JButton("Start Receiving");
        add(startButton, BorderLayout.SOUTH);

        startButton.addActionListener(e -> new Thread(this::receiveFile).start());

        log("Waiting for file...");
    }

    // Main file receiving logic
    private void receiveFile() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            log("Waiting for connection...");
            try (Socket socket = serverSocket.accept();
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                log("Connection established. Receiving file...");

                // Receive file name length and the actual name
                int nameLength = in.readInt();
                byte[] nameBytes = new byte[nameLength];
                in.readFully(nameBytes);
                String fileName = new String(nameBytes);

                // Receive the byte-stuffed data
                int size = in.readInt();
                java.util.List<Byte> received = new ArrayList<>();  // Explicitly use java.util.List
                for (int i = 0; i < size; i++) {
                    received.add(in.readByte());
                }

                // De-stuffing process: Remove stuffing
                java.util.List<Byte> destuffed = new ArrayList<>();  // Explicitly use java.util.List
                boolean started = false;

                for (int i = 0; i < received.size(); i++) {
                    byte b = received.get(i);

                    // Start flag
                    if (b == 'F') {
                        if (!started) {
                            started = true;  // Skip first F (start flag)
                            continue;
                        } else {
                            break;  // End flag, stop processing
                        }
                    }

                    // Handle escape character
                    if (b == 'E') {
                        i++;  // Skip next byte
                        b = received.get(i);
                    }

                    // Add valid byte to destuffed list
                    destuffed.add(b);
                }

                // Write the received (de-stuffed) file
                try (FileOutputStream fos = new FileOutputStream("received_" + fileName)) {
                    for (byte b : destuffed) {
                        fos.write(b);
                    }
                }

                log("File received and saved as received_" + fileName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            log("Error: " + ex.getMessage());
        }
    }

    // Log messages to the display area
    private void log(String message) {
        displayArea.append(message + "\n");
    }

    // Start GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileReceiverGUI gui = new FileReceiverGUI();
            gui.setVisible(true);
        });
    }
}
