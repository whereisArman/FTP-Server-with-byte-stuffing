import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileSenderGUI extends JFrame {
    private JTextField filePathField;
    private JButton browseButton, sendButton, clearButton;
    private JTextArea logArea;

    public FileSenderGUI() {
        setTitle("File Sender with Byte Stuffing");
        setSize(600, 400);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Top Panel: File Selection
        JPanel topPanel = new JPanel(new FlowLayout());
        filePathField = new JTextField(30);
        browseButton = new JButton("Browse");
        sendButton = new JButton("Send");
        clearButton = new JButton("Clear Log");

        topPanel.add(new JLabel("Select File:"));
        topPanel.add(filePathField);
        topPanel.add(browseButton);
        topPanel.add(sendButton);
        topPanel.add(clearButton);

        // Center: Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Action Listeners
        browseButton.addActionListener(this::chooseFile);
        sendButton.addActionListener(e -> sendFile());
        clearButton.addActionListener(e -> logArea.setText(""));

        log("Welcome to File Sender");
    }

    // File chooser dialog
    private void chooseFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            filePathField.setText(selected.getAbsolutePath());
            log("Selected file: " + selected.getName());
            log("Size: " + selected.length() + " bytes");
        }
    }

    // Main sending logic
    private void sendFile() {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            log("Please select a file before sending.");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log("Error: File not found.");
            return;
        }

        try (
                Socket socket = new Socket("localhost", 5000);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(file)
        ) {
            byte[] fileData = fis.readAllBytes();
            String fileName = file.getName();
            byte[] nameBytes = fileName.getBytes();

            // Byte stuffing using flags: F = frame delimiter, E = escape
            List<Byte> stuffed = new ArrayList<>();
            stuffed.add((byte) 'F');  // Start flag

            for (byte b : fileData) {
                if (b == 'F' || b == 'E') {
                    stuffed.add((byte) 'E');
                }
                stuffed.add(b);
            }

            stuffed.add((byte) 'F');  // End flag

            // Send metadata
            long startTime = System.currentTimeMillis();
            out.writeInt(nameBytes.length);    // Send file name length
            out.write(nameBytes);              // Send file name
            out.writeInt(stuffed.size());      // Send data length

            for (byte b : stuffed) {
                out.writeByte(b);              // Send byte-stuffed content
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log("File sent: " + fileName);
            log("Original size: " + fileData.length + " bytes");
            log("Stuffed size: " + stuffed.size() + " bytes");
            log("Transfer time: " + duration + " ms");

        } catch (IOException ex) {
            ex.printStackTrace();
            log("Transmission error: " + ex.getMessage());
        }
    }

    // Append log messages to GUI
    private void log(String message) {
        logArea.append(message + "\n");
    }

    // Start GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileSenderGUI gui = new FileSenderGUI();
            gui.setVisible(true);
        });
    }
}