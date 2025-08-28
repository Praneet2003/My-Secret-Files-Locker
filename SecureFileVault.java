import java.awt.*;
import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class SecureFileVault {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { 
            try {
                UIManager.put("Panel.background", Color.BLACK);
                UIManager.put("OptionPane.background", Color.BLACK);
                UIManager.put("OptionPane.messageForeground", Color.WHITE);
                UIManager.put("Button.background", new Color(138, 43, 226));
                UIManager.put("Button.foreground", Color.WHITE);
                UIManager.put("Label.foreground", Color.WHITE);
                UIManager.put("TextField.background", Color.DARK_GRAY);
                UIManager.put("TextField.foreground", Color.WHITE);
                UIManager.put("PasswordField.background", Color.DARK_GRAY);
                UIManager.put("PasswordField.foreground", Color.WHITE);
                UIManager.put("List.background", Color.DARK_GRAY);
                UIManager.put("List.foreground", Color.WHITE);
                UIManager.put("ScrollPane.background", Color.BLACK);
                UIManager.put("TitledBorder.titleColor", Color.WHITE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginScreen();
        });
    }
}

class LoginScreen extends JFrame {
    private JPasswordField passwordField;

    public LoginScreen() {
        setTitle("My Secret Files Locker - Login");
        setSize(450, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon("D:/Java/Java/SUmmer traning LPU/icon.png").getImage());

        Color backgroundColor = Color.BLACK;
        Color buttonColor = new Color(138, 43, 226);
        Color textColor = Color.WHITE;

        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Logo image
        // ImageIcon logoIcon = new ImageIcon("D:/Java/Java/SUmmer traning LPU/icon.png");
        // Image logoImage = logoIcon.getImage().getScaledInstance(60, 100, Image.SCALE_SMOOTH);
        // JLabel logoLabel = new JLabel(new ImageIcon(logoImage), SwingConstants.CENTER);
        // panel.add(logoLabel);

        JLabel label = new JLabel("Enter Master Password:", SwingConstants.CENTER);
        label.setForeground(textColor);
        panel.add(label);

        passwordField = new JPasswordField(20);
        passwordField.setBackground(Color.DARK_GRAY);
        passwordField.setForeground(textColor);
        passwordField.setCaretColor(textColor);
        panel.add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(buttonColor);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.addActionListener(e -> {
            char[] input = passwordField.getPassword();
            if (new String(input).equals("admin")) {
                dispose();//disappers the loginwindow screen
                new Dashboard(new String(input));
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect password.");
            }
        });
        panel.add(loginBtn);

        add(panel);
        setVisible(true);
    }
}

class Dashboard extends JFrame{
    private String password;
    private DefaultListModel<File> lockedListModel = new DefaultListModel<>();
    private JList<File> lockedFileList = new JList<>(lockedListModel);
    private File vaultDir = new File("vault");
    private File metaFile = new File(vaultDir, ".meta");
    private Properties fileMap = new Properties();

    public Dashboard(String password) {
        this.password = password;
        setTitle("My Secret Files Locker");
        setSize(550, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setIconImage(new ImageIcon("D:/Java/Java/SUmmer traning LPU/icon.png").getImage());

        vaultDir.mkdir();
        loadMetadata();

        JButton encryptFileBtn = new JButton("Encrypt File");
        JButton encryptFolderBtn = new JButton("Encrypt Folder");
        JButton decryptBtn = new JButton("Unlock File");

        encryptFileBtn.addActionListener(e -> encryptSingleFile());
        encryptFolderBtn.addActionListener(e -> encryptFolder());
        decryptBtn.addActionListener(e -> unlockSelectedFile());

        JPanel topPanel = new JPanel();
        topPanel.add(encryptFileBtn);
        topPanel.add(encryptFolderBtn);
        topPanel.add(decryptBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(lockedFileList), BorderLayout.CENTER);

        loadLockedFiles();
        setVisible(true);
    }

    private void encryptSingleFile() { 
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            encryptFile(chooser.getSelectedFile());
        }
    }

    private void encryptFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        encryptFile(file);
                    }
                }
                JOptionPane.showMessageDialog(this, "All files in folder encrypted.");
            }
        }
    }

    private void encryptFile(File selectedFile) {
        File lockedFile = new File(vaultDir, selectedFile.getName() + ".locked");
        try {
            SecretKey key = generateKey(password);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            processFile(cipher, selectedFile, lockedFile);
            fileMap.setProperty(lockedFile.getName(), selectedFile.getAbsolutePath());
            saveMetadata();
            selectedFile.delete();
            lockedListModel.addElement(lockedFile);
            JOptionPane.showMessageDialog(this, "File encrypted and saved to vault.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Encryption failed.");
        }
    }

    private void unlockSelectedFile() {
        File lockedFile = lockedFileList.getSelectedValue();
        if (lockedFile == null) {
            JOptionPane.showMessageDialog(this, "No file selected.");
            return;
        }

        String originalPath = fileMap.getProperty(lockedFile.getName());
        if (originalPath == null) {
            JOptionPane.showMessageDialog(this, "Original path not found.");
            return;
        }

        File unlockedFile = new File(originalPath);
        try {
            SecretKey key = generateKey(password);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            processFile(cipher, lockedFile, unlockedFile);
            lockedFile.delete();
            fileMap.remove(lockedFile.getName());
            saveMetadata();
            lockedListModel.removeElement(lockedFile);
            JOptionPane.showMessageDialog(this, "File decrypted to: " + unlockedFile.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Decryption failed.");
        }
    }

    private void loadLockedFiles() {
        lockedListModel.clear();
        File[] files = vaultDir.listFiles((dir, name) -> name.endsWith(".locked"));
        if (files != null) {
            for (File f : files) {
                lockedListModel.addElement(f);
            }
        }
    }

    private void loadMetadata() {
        try {
            if (metaFile.exists()) {
                try (FileInputStream fis = new FileInputStream(metaFile)) {
                    fileMap.load(fis);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMetadata() {
        try (FileOutputStream fos = new FileOutputStream(metaFile)) {
            fileMap.store(fos, "Original file paths for decryption");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SecretKey generateKey(String password) throws Exception {
        byte[] key = password.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

    private void processFile(Cipher cipher, File input, File output) throws IOException, GeneralSecurityException {
        try (FileInputStream fis = new FileInputStream(input);
            FileOutputStream fos = new FileOutputStream(output);
            CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
        }
    }
}
