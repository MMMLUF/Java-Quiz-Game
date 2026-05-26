package com.quiz.client;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 게임 종료 후 최종 점수와 명예의 전당 랭킹을 표시하는 결과 화면.
 * 모든 DB 접근은 서버를 경유한다 (REGISTER / TOP10 / QUIT 프로토콜).
 */
public class ResultScreen extends JFrame {

    private final String nickname;
    private final int score;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private JTable tableLeaderboard;
    private DefaultTableModel tableModel;
    private JButton btnRegister;
    private JButton btnExit;

    /**
     * @param nickname 플레이어 닉네임
     * @param score    이번 게임에서 획득한 최종 점수
     * @param socket   서버와 연결된 소켓 (QuizScreen에서 인계받음)
     * @param in       서버에서 신호를 읽을 BufferedReader
     * @param out      서버로 신호를 전송할 PrintWriter
     */
    public ResultScreen(String nickname, int score, Socket socket, BufferedReader in, PrintWriter out) {
        this.nickname = nickname;
        this.score = score;
        this.socket = socket;
        this.in = in;
        this.out = out;

        setTitle("퀴즈 게임 결과");
        setSize(460, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Theme.BG);

        // 상단 점수 패널
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        topPanel.setBackground(Theme.PRIMARY);
        topPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel lblGameOver = new JLabel("GAME OVER", JLabel.CENTER);
        lblGameOver.setFont(Theme.TITLE);
        lblGameOver.setForeground(new Color(255, 255, 255, 180));

        JLabel lblScore = new JLabel(nickname + "  |  " + score + "점", JLabel.CENTER);
        lblScore.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        lblScore.setForeground(Color.WHITE);

        topPanel.add(lblGameOver);
        topPanel.add(lblScore);
        add(topPanel, BorderLayout.NORTH);

        // 리더보드 테이블
        String[] columnNames = {"순위", "닉네임", "점수"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableLeaderboard = new JTable(tableModel);
        tableLeaderboard.setFont(Theme.BODY);
        tableLeaderboard.setForeground(Theme.TEXT);
        tableLeaderboard.setRowHeight(36);
        tableLeaderboard.setShowHorizontalLines(true);
        tableLeaderboard.setGridColor(Theme.BORDER);
        tableLeaderboard.setBackground(Theme.SURFACE);

        JTableHeader tableHeader = tableLeaderboard.getTableHeader();
        tableHeader.setFont(Theme.SMALL);
        tableHeader.setBackground(new Color(241, 245, 249));
        tableHeader.setForeground(Theme.MUTED);
        tableHeader.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 3; i++) {
            tableLeaderboard.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        tableLeaderboard.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableLeaderboard.getColumnModel().getColumn(2).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(tableLeaderboard);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        scrollPane.setBackground(Theme.BG);
        add(scrollPane, BorderLayout.CENTER);

        // 버튼
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        bottomPanel.setBackground(Theme.BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 20, 16));

        btnRegister = new JButton("랭킹 등록");
        btnRegister.setFont(Theme.BUTTON);
        btnRegister.setBackground(Theme.PRIMARY);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setBorderPainted(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton btnReplay = new JButton("다시 하기");
        btnReplay.setFont(Theme.BUTTON);
        btnReplay.setBackground(Theme.SURFACE);
        btnReplay.setForeground(Theme.PRIMARY);
        btnReplay.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY));
        btnReplay.setFocusPainted(false);
        btnReplay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnExit = new JButton("게임 종료");
        btnExit.setFont(Theme.BUTTON);
        btnExit.setBackground(Theme.SURFACE);
        btnExit.setForeground(Theme.TEXT);
        btnExit.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        btnExit.setFocusPainted(false);
        btnExit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        bottomPanel.add(btnRegister);
        bottomPanel.add(btnReplay);
        bottomPanel.add(btnExit);
        add(bottomPanel, BorderLayout.SOUTH);

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1회 등록 정책: 누른 순간 즉시 비활성화하여 중복 요청 방지
                btnRegister.setEnabled(false);
                registerScore();
            }
        });

        btnReplay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitSession();
                dispose();
                new LoginScreen().setVisible(true);
            }
        });

        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitSession();
                System.exit(0);
            }
        });

        // 진입 즉시 Top 10 자동 조회
        requestTop10();
    }

    /**
     * 서버에 TOP10 명령을 전송하고 응답을 받아 테이블을 갱신한다.
     * 소켓 I/O는 EDT를 막지 않도록 별도 스레드에서 수행한다.
     */
    private void requestTop10() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.println("TOP10");

                    String header = in.readLine();
                    if (header == null || !"TOP10".equals(header.trim())) {
                        throw new IOException("예상치 못한 응답: " + header);
                    }

                    String countLine = in.readLine();
                    if (countLine == null) throw new IOException("응답 종료(count 누락)");
                    int count = Integer.parseInt(countLine.trim());

                    final Object[][] rows = new Object[count][];
                    for (int i = 0; i < count; i++) {
                        String row = in.readLine();
                        if (row == null) throw new IOException("응답 종료(행 " + i + ")");
                        String[] parts = row.split("\t", 2);
                        String name = parts.length >= 1 ? parts[0] : "";
                        String pts  = parts.length >= 2 ? parts[1] : "0";
                        rows[i] = new Object[]{ (i + 1) + "위", name, pts + "점" };
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tableModel.setRowCount(0);
                            for (Object[] r : rows) tableModel.addRow(r);
                            if (tableModel.getRowCount() == 0) {
                                tableModel.addRow(new Object[]{"-", "아직 등록된 기록이 없습니다", "-"});
                            }
                        }
                    });

                } catch (Exception ex) {
                    final String msg = ex.getMessage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tableModel.setRowCount(0);
                            tableModel.addRow(new Object[]{"-", "서버 연결 오류", "-"});
                            btnRegister.setEnabled(false);
                            btnRegister.setText("랭킹 기능 비활성화");
                            JOptionPane.showMessageDialog(ResultScreen.this,
                                    "서버와의 통신에 실패하여 랭킹 기능이 비활성화됩니다.\n" + (msg == null ? "" : msg),
                                    "DB 오류", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 서버에 REGISTER 명령을 전송하고 응답에 따라 모달과 테이블을 갱신한다.
     * 등록은 1회만 허용 (호출자가 미리 버튼을 disable함).
     */
    private void registerScore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.println("REGISTER");
                    String response = in.readLine();
                    if (response == null) throw new IOException("응답 종료");
                    response = response.trim();

                    if ("REGISTER_OK".equals(response)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(ResultScreen.this, "랭킹에 등록되었습니다!");
                            }
                        });
                        requestTop10();
                    } else if ("REGISTER_FAIL".equals(response)) {
                        String reasonLine = in.readLine();
                        final String reason = reasonLine == null ? "DB 오류" : reasonLine.trim();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(ResultScreen.this,
                                        "랭킹 등록에 실패했습니다.\n" + reason,
                                        "등록 실패", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    } else {
                        throw new IOException("알 수 없는 응답: " + response);
                    }

                } catch (Exception ex) {
                    final String msg = ex.getMessage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(ResultScreen.this,
                                    "서버와의 통신에 실패했습니다.\n" + (msg == null ? "" : msg),
                                    "통신 오류", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 서버에 QUIT 신호를 보내고 소켓을 닫는다. 실패는 무시한다.
     */
    private void quitSession() {
        try {
            out.println("QUIT");
            socket.close();
        } catch (Exception ignored) {
            // 종료 경로이므로 무시
        }
    }
}
