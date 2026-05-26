package com.quiz.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LoginScreen extends JFrame {
    private JTextField txtNickname;
    private JComboBox<String> comboCategory;
    private JButton btnStart;

    public LoginScreen() {
        setTitle("온라인 퀴즈 게임");
        setSize(420, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        JLabel lblTitle = new JLabel("온라인 퀴즈 게임", JLabel.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);

        JPanel card = new JPanel(new GridLayout(3, 1, 0, 16));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));

        JPanel nameRow = new JPanel(new BorderLayout(10, 0));
        nameRow.setBackground(Theme.SURFACE);
        JLabel lblNick = new JLabel("닉네임");
        lblNick.setFont(Theme.HEADING);
        lblNick.setForeground(Theme.TEXT);
        lblNick.setPreferredSize(new Dimension(72, 0));
        txtNickname = new JTextField();
        txtNickname.setFont(Theme.BODY);
        txtNickname.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        nameRow.add(lblNick, BorderLayout.WEST);
        nameRow.add(txtNickname, BorderLayout.CENTER);

        JPanel cateRow = new JPanel(new BorderLayout(10, 0));
        cateRow.setBackground(Theme.SURFACE);
        JLabel lblCate = new JLabel("카테고리");
        lblCate.setFont(Theme.HEADING);
        lblCate.setForeground(Theme.TEXT);
        lblCate.setPreferredSize(new Dimension(72, 0));
        String[] categories = {"Java 코드", "SKKU 퀴즈", "일반상식", "넌센스"};
        comboCategory = new JComboBox<>(categories);
        comboCategory.setFont(Theme.BODY);
        cateRow.add(lblCate, BorderLayout.WEST);
        cateRow.add(comboCategory, BorderLayout.CENTER);

        btnStart = new JButton("게임 시작");
        btnStart.setFont(Theme.BUTTON);
        btnStart.setBackground(Theme.PRIMARY);
        btnStart.setForeground(Color.WHITE);
        btnStart.setBorderPainted(false);
        btnStart.setFocusPainted(false);
        btnStart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.add(nameRow);
        card.add(cateRow);
        card.add(btnStart);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 30, 30, 30));
        wrapper.add(card, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nickname = txtNickname.getText().trim();
                String category = (String) comboCategory.getSelectedItem();

                if (nickname.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "닉네임을 입력해주세요!", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (nickname.length() < 2 || nickname.length() > 8) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "닉네임은 2자 이상 8자 이하로 제한됩니다!", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!nickname.matches("[가-힣a-zA-Z0-9]+")) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean connected = false;
                Socket socket = null;
                while (!connected) {
                    try {
                        socket = new Socket("localhost", 9999);
                        connected = true;
                    } catch (Exception ex) {
                        int choice = JOptionPane.showConfirmDialog(LoginScreen.this,
                                "서버와 연결할 수 없습니다. 다시 시도하시겠습니까?",
                                "연결 실패", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                        if (choice == JOptionPane.NO_OPTION) return;
                    }
                }

                try {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    out.println(nickname);
                    out.println(category);
                    QuizScreen quizScreen = new QuizScreen(nickname, category, socket);
                    quizScreen.setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginScreen().setVisible(true);
            }
        });
    }
}
