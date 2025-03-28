import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class PasswordManager extends JFrame {
    private static final String PASSWORDS_FILE = "passwords.dat";
    private static final String KEY_FILE = "crypt.key";
    private SecretKey secretKey;
    private JTextField siteField;
    private JPasswordField passwordField;
    private JTextArea passwordListArea;
    private Map<String, String> passwords;

    public PasswordManager() {
        setTitle("PWManager");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        loadOrGenerateKey();
        loadPasswords();

        add(new JLabel("Site:"));
        siteField = new JTextField(30);
        add(siteField);
        
        add(new JLabel("Password:"));
        passwordField = new JPasswordField(30);
        add(passwordField);
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> savePassword());
        add(saveButton);
        
        JButton generateButton = new JButton("Generate Password");
        generateButton.addActionListener(e -> generatePassword());
        add(generateButton);
        
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deletePassword());
        add(deleteButton);
        
        JButton viewButton = new JButton("View Passwords");
        viewButton.addActionListener(e -> viewPasswords());
        add(viewButton);
        
        passwordListArea = new JTextArea(20, 40);
        passwordListArea.setEditable(false);
        add(new JScrollPane(passwordListArea));
        
        setVisible(true);
    }

    @SuppressWarnings("resource")
    private void loadOrGenerateKey() {
        try {
            File keyFile = new File(KEY_FILE);
            if (keyFile.exists()) {
                byte[] keyBytes = new byte[(int) keyFile.length()];
                new FileInputStream(keyFile).read(keyBytes);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                secretKey = keyGen.generateKey();
                try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
                    fos.write(secretKey.getEncoded());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPasswords() {
        passwords = new HashMap<>();
        File file = new File(PASSWORDS_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            passwords = (Map<String, String>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePasswords() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PASSWORDS_FILE))) {
            oos.writeObject(passwords);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePassword() {
        String site = siteField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (site.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter site and password");
            return;
        }
        try {
            passwords.put(site, encrypt(password));
            savePasswords();
            JOptionPane.showMessageDialog(this, "Password saved");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append((char) (random.nextInt(94) + 33));
        }
        passwordField.setText(password.toString());
    }

    private void deletePassword() {
        String site = siteField.getText().trim();
        if (passwords.containsKey(site)) {
            passwords.remove(site);
            savePasswords();
            JOptionPane.showMessageDialog(this, "Password deleted");
        } else {
            JOptionPane.showMessageDialog(this, "Password not found");
        }
    }

    private void viewPasswords() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : passwords.entrySet()) {
            try {
                sb.append(entry.getKey()).append(" : ").append(decrypt(entry.getValue())).append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        passwordListArea.setText(sb.toString());
    }

    private String encrypt(String str) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes()));
    }

    private String decrypt(String str) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(str)));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PasswordManager::new);
    }
}
