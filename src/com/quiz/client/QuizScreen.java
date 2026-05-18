package com.quiz.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;

public class QuizScreen extends JFrame {
    private JLabel lblStatus;   
    private JLabel lblTimer;    
    private JTextArea txtQuestion; 
    private JButton[] btnOptions;  

    private String nickname;
    private String category;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public QuizScreen(String nickname, String category, Socket socket) {
        this.nickname = nickname;
        this.category = category;
        this.socket = socket;

        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("온라인 퀴즈 게임 - 플레이 중");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout(20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        lblStatus = new JLabel("문제 로딩 중...");
        lblStatus.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblTimer = new JLabel("⏱ 제한시간: 15s");
        lblTimer.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        headerPanel.add(lblStatus, BorderLayout.WEST);
        headerPanel.add(lblTimer, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        txtQuestion = new JTextArea("\n서버로부터 문제를 받아오는 중입니다...");
        txtQuestion.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        txtQuestion.setEditable(false); 
        txtQuestion.setLineWrap(true);   
        txtQuestion.setBackground(new Color(245, 245, 245)); 
        txtQuestion.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(txtQuestion, BorderLayout.CENTER);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        
        btnOptions = new JButton[4];
        for (int i = 0; i < 4; i++) {
            btnOptions[i] = new JButton((i + 1) + "번 보기");
            btnOptions[i].setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            optionsPanel.add(btnOptions[i]);

            final int selectedNum = i + 1;
            btnOptions[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    out.println(selectedNum); 
                    
                    for (JButton btn : btnOptions) {
                        btn.setEnabled(false); 
                    }
                }
            });
        }
        add(optionsPanel, BorderLayout.SOUTH);

        new ReceiveThread().start();
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                String signal;
                while ((signal = in.readLine()) != null) {
                    signal = signal.trim(); 
                    
                    if ("NEXT_QUESTION".equals(signal)) {
                        String progress = in.readLine(); 
                        String questionText = in.readLine();
                        String optionsStr = in.readLine();
                        JSONArray optionsArray = new JSONArray(optionsStr);

                        lblStatus.setText("문제: " + progress + "  (" + nickname + "님 플레이 중)");
                        txtQuestion.setText("\n" + questionText);
                        for (int i = 0; i < 4; i++) {
                            btnOptions[i].setText(optionsArray.getString(i));
                            btnOptions[i].setEnabled(true); 
                        }
                        
                    } else if ("TIMER_UPDATE".equals(signal)) {
                        // [추가] 서버가 매초 보낸 초 정보를 받아서 라벨 텍스트를 실시간 변경
                        String seconds = in.readLine();
                        lblTimer.setText("⏱ 제한시간: " + seconds + "s");
                        
                    } else if ("TIME_OUT_SIGNAL".equals(signal)) {
                        // [추가] 0초 만료 신호를 받으면 유저 대신 정답 '0번'(타임아웃 코드)을 서버로 강제 전송
                        for (JButton btn : btnOptions) {
                            btn.setEnabled(false); // 버튼 차단
                        }
                        out.println(0); 
                        
                    } else if ("GAME_OVER".equals(signal)) {
                        // 1. 서버가 보낸 점수를 일단 글자(Str)로 받습니다.
                        String finalScoreStr = in.readLine(); 
                        
                        // 2. ⚠️ 핵심: 받아온 글자를 정수(int) 숫자로 명확하게 조립(파싱)해 줍니다.
                        int finalScore = Integer.parseInt(finalScoreStr.trim());
                        
                        // ✅ 성공: 이제 숫자(int)가 넘어갔으므로 에러가 사라집니다!
                        ResultScreen resultScreen = new ResultScreen(nickname, finalScore);
                        resultScreen.setVisible(true);
                        
                        dispose();
                        break;
                    } else {
                        JOptionPane.showMessageDialog(QuizScreen.this, signal, "채점 결과", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } catch (Exception e) {
                System.out.println("🔌 [CLIENT] 서버와 연결 끊김");
            }
        }
    }
}