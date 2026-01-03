import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    private boolean succeeded = false;
    private String userId;
    private String role = "member";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    public LoginDialog(Frame parent, Library lib) {
        super(parent, "Login", true);

        JPanel mainPanel = new JPanel(new BorderLayout(8,8));
        mainPanel.setBorder(new EmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbUser = new JLabel("User ID:");
        cs.gridx = 0; cs.gridy = 0; cs.gridwidth = 1; cs.weightx = 0.0;
        form.add(lbUser, cs);

        JTextField tfUser = new JTextField(20);
        cs.gridx = 1; cs.gridy = 0; cs.gridwidth = 2; cs.weightx = 1.0;
        form.add(tfUser, cs);

        JLabel lbPass = new JLabel("Password:");
        cs.gridx = 0; cs.gridy = 1; cs.gridwidth = 1; cs.weightx = 0.0;
        form.add(lbPass, cs);

        JPasswordField pf = new JPasswordField(20);
        cs.gridx = 1; cs.gridy = 1; cs.gridwidth = 2; cs.weightx = 1.0;
        form.add(pf, cs);

        mainPanel.add(form, BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLogin = new JButton("Login");
        JButton btnCancel = new JButton("Cancel");

        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String id = tfUser.getText().trim();
                String pass = new String(pf.getPassword());
                boolean ok = false;
                // admin hard-coded credentials
                if (ADMIN_USER.equals(id) && ADMIN_PASS.equals(pass)) {
                    role = "manager"; ok = true;
                } else {
                    role = "member";
                    if (id.length() > 0) {
                        try {
                            if (lib instanceof SqlLibrary) ok = ((SqlLibrary) lib).memberExists(id);
                            else ok = lib.memberExists(id);
                        } catch (Exception ex) { ok = false; }
                    }
                    if (!ok) JOptionPane.showMessageDialog(LoginDialog.this, "Invalid member ID", "Login", JOptionPane.ERROR_MESSAGE);
                }

                if (ok) { succeeded = true; userId = id; dispose(); }
                else tfUser.requestFocus();
            }
        });

        btnCancel.addActionListener(e -> { succeeded = false; dispose(); });
        bp.add(btnLogin); bp.add(btnCancel);

        mainPanel.add(bp, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
        setResizable(false); setLocationRelativeTo(parent);
    }

    public boolean isSucceeded() { return succeeded; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
}
