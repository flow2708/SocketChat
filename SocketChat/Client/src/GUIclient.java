import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GUIclient {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField nicknameField;
    private JButton connectButton;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String nickname;
    private final String SERVER_ADDRESS = "localhost";
    private final int PORT = 8080;

    public GUIclient() {
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout(5, 5));

        // Set modern look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Connection panel
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        connectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        nicknameField = new JTextField(20);
        nicknameField.setText("User" + (int)(Math.random()*1000));
        nicknameField.setFont(new Font("Arial", Font.PLAIN, 14));

        connectButton = new JButton("Подключиться");
        styleButton(connectButton, new Color(70, 130, 180));

        connectionPanel.add(new JLabel("Имя пользователя:"));
        connectionPanel.add(nicknameField);
        connectionPanel.add(connectButton);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Сообщения"));

        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 16));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        sendButton = new JButton("Отправить");
        styleButton(sendButton, new Color(34, 139, 34));
        sendButton.setEnabled(false);

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        frame.add(connectionPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(messagePanel, BorderLayout.SOUTH);

        // Event handlers
        connectButton.addActionListener(e -> {
            nickname = nicknameField.getText().trim();
            if (!nickname.isEmpty()) {
                connectToServer();
            } else {
                JOptionPane.showMessageDialog(frame, "Введите имя пользователя!");
            }
        });

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        frame.setVisible(true);
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(color.darker());
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });
    }

    private void connectToServer() {
        new Thread(() -> {
            int attempts = 0;
            final int MAX_ATTEMPTS = 3;
            final int RETRY_DELAY = 1000;

            while (attempts < MAX_ATTEMPTS) {
                try {
                    socket = new Socket(SERVER_ADDRESS, PORT);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    // Успешное подключение
                    SwingUtilities.invokeLater(() -> {
                        connectButton.setEnabled(false);
                        nicknameField.setEnabled(false);
                        sendButton.setEnabled(true);
                        messageField.requestFocus();
                        appendToChat("Успешное подключение к серверу!");
                    });

                    out.write("Hello " + nickname + "\n");
                    out.flush();
                    new Thread(this::readMessages).start();
                    return;

                } catch (IOException e) {
                    attempts++;
                    final int currentAttempt = attempts;
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Попытка подключения " + currentAttempt + "/" + MAX_ATTEMPTS + " не удалась");
                        if(currentAttempt == MAX_ATTEMPTS) {
                            appendToChat("Не удалось подключиться после " + MAX_ATTEMPTS + " попыток");
                            connectButton.setEnabled(true);
                            nicknameField.setEnabled(true);
                        }
                    });

                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }).start();
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("stop")) {
                    disconnect();
                    break;
                }
                appendToChat(message);
            }
        } catch (IOException e) {
            appendToChat("Соединение прервано: " + e.getMessage());
            disconnect();
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                String dtime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                out.write("(" + dtime + ") " + nickname + ": " + message + "\n");
                out.flush();
                messageField.setText("");
            } catch (IOException e) {
                appendToChat("Ошибка отправки: " + e.getMessage());
                disconnect();
            }
        }
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            nicknameField.setEnabled(true);
            sendButton.setEnabled(false);
        });
    }

    private void appendToChat(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIclient::new);
    }
}