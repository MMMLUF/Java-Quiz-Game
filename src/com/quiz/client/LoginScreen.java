package com.quiz.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LoginScreen extends JFrame {
    private JTextField txtNickname;
    private JComboBox<String> comboCategory;
    private JButton btnStart;

    public LoginScreen() {
        setTitle("온라인 퀴즈 게임");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1, 10, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel namePanel = new JPanel(new BorderLayout(10, 0));
        namePanel.add(new JLabel("닉네임: "), BorderLayout.WEST);
        txtNickname = new JTextField();
        namePanel.add(txtNickname, BorderLayout.CENTER);

        JPanel catePanel = new JPanel(new BorderLayout(10, 0));
        catePanel.add(new JLabel("카테고리: "), BorderLayout.WEST);
        String[] categories = {"Java 코드", "SKKU 퀴즈", "일반상식", "넌센스"};
        comboCategory = new JComboBox<>(categories);
        catePanel.add(comboCategory, BorderLayout.CENTER);

        btnStart = new JButton("게임 시작 🎯");
        
        mainPanel.add(namePanel);
        mainPanel.add(catePanel);
        mainPanel.add(btnStart);

        add(mainPanel, BorderLayout.CENTER);

     // LoginScreen.java 내의 btnStart 이벤트 리스너 부분 보정 코드입니다.
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

                // [제안서 반영] 서버 다운 및 연결 실패 시 대응 전략 (재연결 유도 루프)
                boolean connected = false;
                Socket socket = null;
                
                while (!connected) {
                    try {
                        socket = new Socket("localhost", 9999);
                        connected = true; // 연결 성공 시 루프 탈출
                    } catch (Exception ex) {
                        // [PPT 명세] 사용자에게 알림 다이얼로그 제공 및 재시도 여부 확인
                        int choice = JOptionPane.showConfirmDialog(LoginScreen.this, 
                                "서버와 연결할 수 없습니다. 다시 연결을 시도하시겠습니까?", 
                                "🌐 연결 실패 (ConnectException)", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                        
                        if (choice == JOptionPane.NO_OPTION) {
                            return; // 재시도를 포기하면 함수 종료 (게임 진행 안 함)
                        }
                        // YES를 누르면 while 루프에 의해 자동으로 재연결을 시도합니다.
                    }
                }

                // 연결에 성공하여 무사히 루프를 탈출한 경우에만 아래 로직이 수행됩니다.
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