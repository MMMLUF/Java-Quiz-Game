package com.quiz.client;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 게임 종료 후 최종 점수와 명예의 전당 랭킹을 표시하는 결과 화면.
 * 랭킹 등록, 다시 하기, 게임 종료 기능을 제공한다.
 */
public class ResultScreen extends JFrame {

    private String nickname;
    private int score;

    private JTable tableLeaderboard;
    private DefaultTableModel tableModel;
    private JButton btnRegister;
    private JButton btnExit;

    /**
     * @param nickname 플레이어 닉네임
     * @param score    이번 게임에서 획득한 최종 점수
     */
    public ResultScreen(String nickname, int score) {
        this.nickname = nickname;
        this.score = score;

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

        tableModel.addRow(new Object[]{"-", "등록 버튼을 눌러 랭킹에 기록하세요", "-"});

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.quiz.server.HallOfFameDAO dao = new com.quiz.server.HallOfFameDAO();
                dao.createTable();
                dao.insertScore(nickname, score);
                refreshLeaderboard();
                btnRegister.setEnabled(false);
                JOptionPane.showMessageDialog(ResultScreen.this, "랭킹에 등록되었습니다!");
            }
        });

        btnReplay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new LoginScreen().setVisible(true);
            }
        });

        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        refreshLeaderboard();
    }

    /**
     * SQLite DB에서 상위 10명의 랭킹을 조회하여 테이블을 갱신한다.
     * DB 연결 실패 시 사용자에게 안내 메시지를 표시하고 랭킹 기능을 비활성화한다.
     */
    private void refreshLeaderboard() {
        tableModel.setRowCount(0);

        // 스키마 정의는 서버 측 HallOfFameDAO 하나로 일원화한다.
        new com.quiz.server.HallOfFameDAO().createTable();

        String dbUrl     = "jdbc:sqlite:ranking.db";
        String selectSql = "SELECT username, score FROM RANKING ORDER BY score DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                int rank = 1;
                while (rs.next()) {
                    tableModel.addRow(new Object[]{rank + "위", rs.getString("username"), rs.getInt("score") + "점"});
                    rank++;
                }
            }

            if (tableModel.getRowCount() == 0) {
                tableModel.addRow(new Object[]{"-", "아직 등록된 기록이 없습니다", "-"});
            }

        } catch (java.sql.SQLException se) {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"-", "데이터베이스 연결 실패", "-"});
            btnRegister.setEnabled(false);
            btnRegister.setText("랭킹 기능 비활성화");
            JOptionPane.showMessageDialog(ResultScreen.this,
                    "DB 연결에 실패하여 랭킹 기능이 비활성화됩니다.",
                    "DB 오류", JOptionPane.WARNING_MESSAGE);
            se.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
