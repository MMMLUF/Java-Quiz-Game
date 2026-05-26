package com.quiz.client;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;

/**
 * 퀴즈 게임이 진행되는 메인 화면.
 * 서버로부터 문제·타이머·결과 신호를 수신하여 UI를 실시간으로 갱신하며,
 * 플레이어의 보기 선택을 서버로 전송한다.
 */
public class QuizScreen extends JFrame {

    /** 타이머 최대 초 (서버 TimerThread.limitSeconds와 동일해야 함) */
    private static final int TIMER_MAX_SECONDS    = 15;
    /** 피드백 배너 표시 지속 시간(ms) */
    private static final int FEEDBACK_DURATION_MS = 1800;
    /** 타이머가 이 값 이하이면 주의(황색) 색상으로 전환 */
    private static final int WARN_THRESHOLD       = 6;
    /** 타이머가 이 값 이하이면 위험(적색) 색상으로 전환 */
    private static final int DANGER_THRESHOLD     = 3;

    private JLabel lblStatus;
    private JLabel lblTimer;
    private JProgressBar progressBar;
    private JTextArea txtQuestion;
    private JLabel lblFeedback;
    private JButton[] btnOptions;

    private String nickname;
    private String category;
    private Color catColor;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    /** EDT와 ReceiveThread 양쪽에서 접근하므로 volatile 선언 */
    private volatile int selectedOption = -1;

    /**
     * @param nickname 플레이어 닉네임
     * @param category 선택한 퀴즈 카테고리
     * @param socket   서버와 연결된 소켓
     */
    public QuizScreen(String nickname, String category, Socket socket) {
        this.nickname = nickname;
        this.category = category;
        this.catColor  = Theme.categoryColor(category);
        this.socket = socket;

        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "서버 연결에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setTitle("온라인 퀴즈 게임");
        setSize(620, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(Theme.BG);

        // 헤더 + 프로그레스 바
        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setBackground(Theme.SURFACE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));
        lblStatus = new JLabel("문제 로딩 중...");
        lblStatus.setFont(Theme.HEADING);
        lblStatus.setForeground(Theme.TEXT);
        lblTimer = new JLabel(TIMER_MAX_SECONDS + "s");
        lblTimer.setFont(Theme.HEADING);
        lblTimer.setForeground(catColor);
        header.add(lblStatus, BorderLayout.WEST);
        header.add(lblTimer, BorderLayout.EAST);

        progressBar = new JProgressBar(0, TIMER_MAX_SECONDS);
        progressBar.setValue(TIMER_MAX_SECONDS);
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 7));
        progressBar.setUI(new BasicProgressBarUI());
        progressBar.setBackground(Theme.BORDER);
        progressBar.setForeground(catColor);

        topArea.add(header, BorderLayout.CENTER);
        topArea.add(progressBar, BorderLayout.SOUTH);
        add(topArea, BorderLayout.NORTH);

        // 문제 영역 + 피드백 배너
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Theme.SURFACE);

        txtQuestion = new JTextArea();
        txtQuestion.setFont(new Font("맑은 고딕", Font.PLAIN, 17));
        txtQuestion.setForeground(Theme.TEXT);
        txtQuestion.setEditable(false);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setBackground(Theme.SURFACE);
        txtQuestion.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));
        centerPanel.add(txtQuestion, BorderLayout.CENTER);

        lblFeedback = new JLabel("", JLabel.CENTER);
        lblFeedback.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lblFeedback.setOpaque(true);
        lblFeedback.setPreferredSize(new Dimension(0, 42));
        lblFeedback.setVisible(false);
        centerPanel.add(lblFeedback, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 보기 버튼
        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        optionsPanel.setBackground(Theme.BG);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        btnOptions = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final JButton btn = new JButton((i + 1) + "번 보기");
            btn.setFont(Theme.BUTTON);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            resetButtonStyle(btn);
            optionsPanel.add(btn);
            btnOptions[i] = btn;

            final int num = i + 1;
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedOption = num;
                    out.println(num);
                    for (JButton b : btnOptions) b.setEnabled(false);
                }
            });

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (btn.isEnabled()) hoverButtonStyle(btn);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (btn.isEnabled()) resetButtonStyle(btn);
                }
            });
        }
        add(optionsPanel, BorderLayout.SOUTH);

        new Thread(new ReceiveTask()).start();
    }

    /**
     * 버튼을 기본 스타일(흰 배경, 회색 테두리)로 초기화한다.
     *
     * @param btn 스타일을 적용할 버튼
     */
    private void resetButtonStyle(JButton btn) {
        btn.setBackground(Theme.SURFACE);
        btn.setForeground(Theme.TEXT);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
    }

    /**
     * 버튼에 카테고리 색상 기반의 hover 스타일을 적용한다.
     *
     * @param btn 스타일을 적용할 버튼
     */
    private void hoverButtonStyle(JButton btn) {
        int r = catColor.getRed(), g = catColor.getGreen(), b = catColor.getBlue();
        btn.setBackground(new Color(r, g, b, 18));
        btn.setForeground(catColor);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(catColor),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
    }

    /**
     * 버튼의 배경색·글자색·테두리색을 지정한 색으로 설정한다.
     *
     * @param btn    대상 버튼
     * @param bg     배경색
     * @param fg     글자색
     * @param border 테두리색
     */
    private void markButton(JButton btn, Color bg, Color fg, Color border) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
    }

    /**
     * 서버에서 수신한 채점 결과를 파싱하여 버튼 색상과 피드백 배너를 업데이트한다.
     * {@code data} 형식: {@code "correct {점수} {연속수}"} | {@code "wrong {정답번호}"} | {@code "timeout 0"}
     *
     * @param data 서버로부터 받은 결과 문자열
     */
    private void handleResult(String data) {
        String[] parts = data.split(" ");
        String type = parts[0];
        int value = Integer.parseInt(parts[1]);

        if ("correct".equals(type)) {
            int streak = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
            if (selectedOption > 0)
                markButton(btnOptions[selectedOption - 1],
                    new Color(220, 252, 231), Theme.SUCCESS, Theme.SUCCESS);
            String msg = "정답!  +" + value + "점" + (streak >= 2 ? "  " + streak + "연속!" : "");
            showFeedback(msg, Theme.SUCCESS);

        } else if ("wrong".equals(type)) {
            if (selectedOption > 0)
                markButton(btnOptions[selectedOption - 1],
                    new Color(254, 226, 226), Theme.DANGER, Theme.DANGER);
            markButton(btnOptions[value - 1],
                new Color(220, 252, 231), Theme.SUCCESS, Theme.SUCCESS);
            showFeedback("오답!  정답은 " + value + "번", Theme.DANGER);

        } else {
            showFeedback("시간 초과!", Theme.WARNING);
        }
    }

    /**
     * 지정한 메시지를 {@link #FEEDBACK_DURATION_MS}ms 동안 배너로 표시한다.
     *
     * @param message 표시할 텍스트
     * @param color   배너 배경색
     */
    private void showFeedback(String message, Color color) {
        lblFeedback.setText(message);
        lblFeedback.setBackground(color);
        lblFeedback.setForeground(Color.WHITE);
        lblFeedback.setVisible(true);

        javax.swing.Timer t = new javax.swing.Timer(FEEDBACK_DURATION_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lblFeedback.setVisible(false);
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    /**
     * 남은 시간에 따라 타이머 레이블과 프로그레스 바의 색상을 변경한다.
     * {@link #DANGER_THRESHOLD}초 이하: 적색 / {@link #WARN_THRESHOLD}초 이하: 황색 / 그 외: 카테고리색
     *
     * @param secs 현재 남은 시간(초)
     */
    private void updateTimerColor(int secs) {
        Color c = secs <= DANGER_THRESHOLD ? Theme.DANGER
                : secs <= WARN_THRESHOLD   ? Theme.WARNING
                : catColor;
        lblTimer.setForeground(c);
        progressBar.setForeground(c);
    }

    /**
     * 서버 신호를 수신하여 UI를 업데이트하는 작업.
     * EDT 외부 스레드에서 실행되며, Swing 업데이트는 {@link SwingUtilities#invokeLater}를 통해 처리한다.
     */
    private class ReceiveTask implements Runnable {

        @Override
        public void run() {
            boolean normalExit = false;
            try {
                String signal;
                while ((signal = in.readLine()) != null) {
                    final String s = signal.trim();

                    if ("NEXT_QUESTION".equals(s)) {
                        final String progress = in.readLine();
                        if (progress == null) break;
                        final String questionText = in.readLine();
                        if (questionText == null) break;
                        String optsLine = in.readLine();
                        if (optsLine == null) break;
                        JSONArray parsedOpts;
                        try {
                            parsedOpts = new JSONArray(optsLine);
                        } catch (Exception jsonEx) {
                            System.out.println("[CLIENT] 보기 파싱 실패: " + jsonEx.getMessage());
                            continue;
                        }
                        final JSONArray opts = parsedOpts;

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                selectedOption = -1;
                                lblStatus.setText("문제 " + progress + "  —  " + nickname);
                                txtQuestion.setText(questionText);
                                lblFeedback.setVisible(false);
                                progressBar.setValue(TIMER_MAX_SECONDS);
                                lblTimer.setText(TIMER_MAX_SECONDS + "s");
                                updateTimerColor(TIMER_MAX_SECONDS);
                                for (int i = 0; i < 4; i++) {
                                    btnOptions[i].setText(opts.getString(i));
                                    resetButtonStyle(btnOptions[i]);
                                    btnOptions[i].setEnabled(true);
                                }
                            }
                        });

                    } else if ("TIMER_UPDATE".equals(s)) {
                        String secsLine = in.readLine();
                        if (secsLine == null) break;
                        final int secs = Integer.parseInt(secsLine.trim());
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                lblTimer.setText(secs + "s");
                                progressBar.setValue(secs);
                                updateTimerColor(secs);
                            }
                        });

                    } else if ("TIME_OUT_SIGNAL".equals(s)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (JButton btn : btnOptions) btn.setEnabled(false);
                            }
                        });
                        out.println(0);

                    } else if ("RESULT".equals(s)) {
                        String resultLine = in.readLine();
                        if (resultLine == null) break;
                        final String resultData = resultLine.trim();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                handleResult(resultData);
                            }
                        });

                    } else if ("GAME_OVER".equals(s)) {
                        String scoreLine = in.readLine();
                        if (scoreLine == null) break;
                        final int finalScore = Integer.parseInt(scoreLine.trim());
                        normalExit = true;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                new ResultScreen(nickname, finalScore, socket, in, out).setVisible(true);
                                dispose();
                            }
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("[CLIENT] 서버와 연결 끊김: " + e.getMessage());
            }

            if (!normalExit) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisplayable()) {
                            JOptionPane.showMessageDialog(QuizScreen.this,
                                "서버와의 연결이 끊겼습니다.\n로그인 화면으로 돌아갑니다.",
                                "연결 오류", JOptionPane.ERROR_MESSAGE);
                            dispose();
                            new LoginScreen().setVisible(true);
                        }
                    }
                });
            }
        }
    }
}
