package login.java.form;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import conten.java.form.Dasboard;

import database.DBConnection;
import conten.java.form.Dasboard;
import database.DBConnection;

public class LoginDesign extends JFrame {
	JPanel p = new JPanel();

	private JLabel bgLabel;
	private JPanel loginForm;
	private ImageIcon originalBackground;

	private final int FORM_WIDTH = 380;
	private final int FORM_HEIGHT = 500;

	public LoginDesign(){

		this.setTitle("បេនកាហ្វេ- Ben Cafe");
		ImageIcon icon = new ImageIcon("image/BenCafeLogo.png");
		this.setIconImage(icon.getImage());
		this.setSize(1280, 800);
		this.setMinimumSize(new java.awt.Dimension(800, 600));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);

		originalBackground = new ImageIcon("image/Background.jpg");

		bgLabel = new JLabel();
		bgLabel.setLayout(null);
		bgLabel.setBounds(0, 0, 1280, 800);

		loginForm = new JPanel();
		loginForm.setLayout(null);
		loginForm.setBackground(new Color(245, 237, 224));
		loginForm.setOpaque(true);
		loginForm.setBounds(300, 150, FORM_WIDTH, FORM_HEIGHT);
		bgLabel.add(loginForm);

	      ImageIcon logo = new ImageIcon("image/BenCafeLogo.png");
	        Image logoImage = logo.getImage().getScaledInstance(200, 100, Image.SCALE_SMOOTH);
	        JLabel lblLogo = new JLabel(new ImageIcon(logoImage));
	        lblLogo.setBounds(150, 10, 80, 80);
	        loginForm.add(lblLogo);
	        
	        ImageIcon Icon_cup = new ImageIcon("image/Cup.png");
	        Image logoCupImage = Icon_cup.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
	        JLabel lblLogoCup = new JLabel(new ImageIcon(logoCupImage));
	        lblLogoCup.setBounds(10, 95, 200, 30);
	        loginForm.add(lblLogoCup);
	        
	        ImageIcon CoffeBean = new ImageIcon("image/Icon_CoffeeBean.png");
	        Image  LogoCoffeBean = CoffeBean.getImage().getScaledInstance(25, 28, Image.SCALE_SMOOTH);
	        JLabel LogoBean = new JLabel(new ImageIcon(LogoCoffeBean ));
	        LogoBean.setBounds(80, 160, 200, 30);
	        loginForm.add(LogoBean);
	        
	        JLabel lblTitle = new JLabel("Ben Cafe");
	        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
	        lblTitle.setForeground( new Color(102, 51, 0)); 
	        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
	        lblTitle.setBounds(90, 95, 200, 30);
	        loginForm.add(lblTitle);
	        
	        JLabel lblTitle1 = new JLabel("Welcome Back");
	        lblTitle1.setFont(new Font("Segoe UI", Font.BOLD, 20));
	        lblTitle1.setForeground(new Color(86, 46, 25)); 
	        lblTitle1.setHorizontalAlignment(SwingConstants.CENTER);
	        lblTitle1.setBounds(90, 120, 200, 30);
	        loginForm.add(lblTitle1);
	        
	        JLabel lblTitle2 = new JLabel("Please sign in to continue.");
	        lblTitle2.setBounds(110,140,200,30);
	        lblTitle2.setForeground(new Color(111, 78, 55));
	        loginForm.add(lblTitle2);
	        
	        JLabel lblTitle3 = new JLabel("© 2026 Ben Cafe | Version 1.0.0");
	        lblTitle3.setBounds(90,450,200,30);
	        lblTitle3.setForeground(new Color(111, 78, 55));
	        loginForm.add(lblTitle3);
	        
	        JLabel lblLine1 = new JLabel("__________");
	        lblLine1.setBounds(80,160,200,30);
	        lblLine1.setForeground(new Color(111,78,55));
	        loginForm.add(lblLine1);
	        
	        JLabel lblLine2 = new JLabel("__________");
	        lblLine2.setBounds(210,160,200,30);
	        lblLine2.setForeground(new Color(111,78,55));
	        loginForm.add(lblLine2);
	        
	        JLabel lblLine3 = new JLabel("________________________________");
	        lblLine3.setBounds(80,320,210,30);
	        lblLine3.setForeground(new Color(111,78,55));
	        loginForm.add(lblLine3);
	        
	        JLabel lblName = new JLabel("Username");
	        lblName.setBounds(80,190,200,30);
	        lblName.setForeground(new Color(60, 40, 25));
	        
	        loginForm.add(lblName);
	        ImageIcon userIcon = new ImageIcon("image/Icon_User.png");
            Image userImg =userIcon.getImage().getScaledInstance(18,18,Image.SCALE_SMOOTH);
            IconTextField txtUser =new IconTextField(new ImageIcon(userImg),"Enter Username ");
            
             txtUser.setBounds(80,220,200,30);
             txtUser.setBackground(Color.WHITE);
              loginForm.add(txtUser);
              
	        JLabel lblPass = new JLabel("Password");
	        lblPass.setBounds(80,245,200,30);
	        lblPass.setForeground(new Color(60, 40, 25));
	        loginForm.add(lblPass);
	        
	        ImageIcon lockIcon = new ImageIcon("image/Icon_lock.png");
	        Image lockImg =lockIcon.getImage().getScaledInstance(18,18,Image.SCALE_SMOOTH);
	        IconPasswordField txtPass = new IconPasswordField(new ImageIcon(lockImg),"Enter Password");
	        txtPass.setBounds(80,270,200,30);
	        txtPass.setBackground(Color.WHITE);
	        loginForm.add(txtPass);
	        
	        ImageIcon openEye = new ImageIcon("image/Icon_Openeye.png");
	        Image openImg = openEye.getImage().getScaledInstance(18,18,Image.SCALE_SMOOTH);
	        ImageIcon closeEye = new ImageIcon("image/Icon_Closeeye.png");
	        Image closeImg = closeEye.getImage().getScaledInstance(18,18,Image.SCALE_SMOOTH);
	       
	        JButton eyeButton = new JButton(new ImageIcon(openImg));
	        eyeButton.setBounds(275,270,30,30);
	        eyeButton.setFocusPainted(false);
	        eyeButton.setBorderPainted(false);
	        eyeButton.setContentAreaFilled(false);
	        final boolean[] showing = {false};
	        eyeButton.addActionListener(e -> {
	        	if(showing[0]) {
	                txtPass.setEchoChar('*');
	                eyeButton.setIcon(new ImageIcon(openImg));
	                showing[0] = false;
	            } else {
	                txtPass.setEchoChar((char)0);
	                eyeButton.setIcon(new ImageIcon(closeImg));
	                showing[0] = true;
	            }
	        });
	        txtUser.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                txtPass.requestFocusInWindow();
	            }
	        });
	        loginForm.add(eyeButton);	    
	        loginForm.add(lblPass);
	        
	        JCheckBox chkRemember = new JCheckBox("Remember");
	        chkRemember.setForeground(new Color(102, 51, 0));
	        chkRemember.setBounds(80,300, 100, 30);
	        chkRemember.setFocusable(false);
	        chkRemember.setBackground(new Color(245, 237, 224));
	        loginForm.add(chkRemember);
	        
	        JLabel lblForgot = new JLabel("Forgot Password?");
	        lblForgot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
	        lblForgot.setForeground(Color.BLUE);
	        lblForgot.setBounds(180, 300, 150, 30);
	        loginForm.add(lblForgot);

	        lblForgot.setCursor(new Cursor(Cursor.HAND_CURSOR));
	        lblForgot.addMouseListener(new java.awt.event.MouseAdapter() {
	            public void mouseClicked(java.awt.event.MouseEvent evt) {
	                JOptionPane.showMessageDialog(loginForm, "Password reset process...");
	            }
	        });
     // Login 
	        JButton buttonLogin = new JButton("Login");
	        buttonLogin.setBounds(75,370,200,40);
	        buttonLogin.setFocusable(false);
	        buttonLogin.setFont(new Font("Segoe UI", Font.BOLD, 20));
	        buttonLogin.setBackground(new Color(111, 78, 55));
	        buttonLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

	        buttonLogin.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                String username = txtUser.getText().trim();
	                String password = new String(txtPass.getPassword());

	                boolean isValid = false;
	                String sql = "SELECT * FROM tblUser WHERE username=? AND password=? AND status='Active'";

	                try (java.sql.Connection conn = DBConnection.getConnection();
	                     java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

	                    stmt.setString(1, username);
	                    stmt.setString(2, password);

	                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
	                        if (rs.next()) {
	                            isValid = true;
	                        }
	                    }

	                } catch (java.sql.SQLException ex) {
	                    ex.printStackTrace();
	                    JOptionPane.showMessageDialog(LoginDesign.this,
	                            "Database connection error: " + ex.getMessage(),
	                            "Connection Error", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }

	                if (isValid) {
	                    JPanel panel = new JPanel();
	                    panel.setBackground(new Color(245, 237, 224));
	                    panel.setLayout(new BorderLayout(0, 10));
	                    panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));

	                    ImageIcon rawSuccessIcon = new ImageIcon("image/Icon_Success.png");
	                    Image scaledSuccessImg = rawSuccessIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
	                    JLabel lblIcon = new JLabel(new ImageIcon(scaledSuccessImg));
	                    lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
	                    panel.add(lblIcon, BorderLayout.NORTH);

	                    JLabel lbl = new JLabel("Login Successful!", SwingConstants.CENTER);
	                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
	                    lbl.setForeground(new Color(102, 51, 0));
	                    panel.add(lbl, BorderLayout.CENTER);

	                    JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE);
	                    JDialog dialog = optionPane.createDialog(LoginDesign.this, "Ben Cafe");
	                    dialog.getContentPane().setBackground(new Color(245, 237, 224));
	                    styleDialogButtons(dialog.getContentPane());
	                    dialog.setVisible(true);

	                    dispose();
	                    SwingUtilities.invokeLater(() -> new Dasboard().setVisible(true));

	                } else {
	                    JPanel errorPanel = new JPanel();
	                    errorPanel.setBackground(new Color(255, 230, 230));
	                    errorPanel.setLayout(new BorderLayout(0, 10));
	                    errorPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));

	                    ImageIcon rawErrorIcon = new ImageIcon("image/Icon_Error.png");
	                    Image scaledErrorImg = rawErrorIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
	                    JLabel lblIcon = new JLabel(new ImageIcon(scaledErrorImg));
	                    lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
	                    errorPanel.add(lblIcon, BorderLayout.NORTH);

	                    JLabel lblError = new JLabel("Invalid Username or Password!", SwingConstants.CENTER);
	                    lblError.setFont(new Font("Segoe UI", Font.BOLD, 16));
	                    lblError.setForeground(new Color(102, 51, 0));
	                    errorPanel.add(lblError, BorderLayout.CENTER);

	                    JOptionPane optionPane = new JOptionPane(errorPanel, JOptionPane.PLAIN_MESSAGE);
	                    JDialog dialog = optionPane.createDialog(LoginDesign.this, "Ben Cafe");
	                    dialog.getContentPane().setBackground(new Color(245, 237, 224));
	                    styleDialogButtons(dialog.getContentPane());
	                    dialog.setVisible(true);
	                }
	            }
	        });
//	        SwingUtilities.invokeLater(() -> {
//                Dasboard d = new Dasboard();
//                d.setVisible(true);
//            });
	        
	        loginForm.add(buttonLogin);

	        getRootPane().setDefaultButton(buttonLogin);

		this.setLayout(new BorderLayout());
		this.add(bgLabel, BorderLayout.CENTER);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateLayoutForSize(getContentPane().getWidth(), getContentPane().getHeight());
			}
		});

		this.setVisible(true);

		SwingUtilities.invokeLater(() ->
			updateLayoutForSize(getContentPane().getWidth(), getContentPane().getHeight())
		);
	}

	private void updateLayoutForSize(int width, int height) {
		if (width <= 0 || height <= 0) return;

		Image scaled = originalBackground.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
		bgLabel.setIcon(new ImageIcon(scaled));
		bgLabel.setBounds(0, 0, width, height);

		int formX = Math.max(10, (width - FORM_WIDTH) / 2);
		int formY = Math.max(10, (height - FORM_HEIGHT) / 2);
		loginForm.setBounds(formX, formY, FORM_WIDTH, FORM_HEIGHT);

		bgLabel.revalidate();
		bgLabel.repaint();
	}

	private void styleDialogButtons(java.awt.Container container) {
		for (java.awt.Component comp : container.getComponents()) {
			if (comp instanceof JButton) {
				JButton btn = (JButton) comp;
				btn.setBackground(new Color(111, 78, 55));
				btn.setForeground(Color.WHITE);
				btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
				btn.setFocusPainted(false);
				btn.setBorderPainted(false);
				btn.setOpaque(true);
				btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
				btn.setPreferredSize(new java.awt.Dimension(90, 32));
			} else if (comp instanceof java.awt.Container) {
				styleDialogButtons((java.awt.Container) comp);
			}
		}
	}

}