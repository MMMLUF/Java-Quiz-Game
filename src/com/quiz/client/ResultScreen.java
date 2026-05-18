package com.quiz.client;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ResultScreen extends JFrame {
    private String nickname;
    private int score;
    
    // [구조 개선] 투박한 텍스트창 대신 격자 정렬이 완벽 보장되는 JTable 체제로 전환
    private JTable tableLeaderboard; 
    private DefaultTableModel tableModel;
    private JButton btnRegister;      
    private JButton btnExit;          

    public ResultScreen(String nickname, int score) {
        this.nickname = nickname;
        this.score = score;

        setTitle("퀴즈 게임 결과");
        setSize(450, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        
        // [보정] 윈도우 폰트에서 깨지던 🎮, 🏁 이모지를 안전하고 깔끔한 🏆로 통일
        JLabel lblGameOver = new JLabel("🏆 GAME OVER 🏆", JLabel.CENTER);
        lblGameOver.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        
        JLabel lblScore = new JLabel(nickname + "님의 최종 점수: " + score + "점", JLabel.CENTER);
        lblScore.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        lblScore.setForeground(new Color(0, 102, 204)); 

        topPanel.add(lblGameOver);
        topPanel.add(lblScore);
        add(topPanel, BorderLayout.NORTH);

        // [JTable 핵심 세팅] 컬럼 레이아웃 정의 및 수정 금지 처리
        String[] columnNames = {"순위", "닉네임", "최종 점수"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 유저가 테이블 내용을 임의로 더블클릭해서 수정하는 것을 차단
            }
        };
        
        tableLeaderboard = new JTable(tableModel);
        tableLeaderboard.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        tableLeaderboard.setRowHeight(32); // 행 높이를 널찍하게 주어 가시성 확보
        
        // 테이블 내부 텍스트들을 정중앙으로 칼정렬 시키는 렌더러 장착
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableLeaderboard.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableLeaderboard.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableLeaderboard.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableLeaderboard);
        scrollPane.setBorder(BorderFactory.createTitledBorder("🏆 명예의 전당 TOP 10 🏆"));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        
        btnRegister = new JButton("랭킹 등록하기 💾");
        btnRegister.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        btnExit = new JButton("게임 종료 🔌");
        btnExit.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        bottomPanel.add(btnRegister);
        bottomPanel.add(btnExit);
        add(bottomPanel, BorderLayout.SOUTH);

        // 초기 진입 시 안내 문구 행 추가
        tableModel.addRow(new Object[]{"-", "등록 버튼을 누르면", "순위가 반영됩니다."});

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.quiz.server.HallOfFameDAO dao = new com.quiz.server.HallOfFameDAO();
                dao.insertScore(nickname, score);
                
                refreshLeaderboard(); // 데이터베이스 연동 및 갱신
                
                btnRegister.setEnabled(false);
                JOptionPane.showMessageDialog(ResultScreen.this, "명예의 전당에 성공적으로 등록되었습니다!");
            }
        });

        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

 // SQLite DB에서 데이터를 긁어와 JTable 모델에 깔끔하게 바인딩하는 메서드
    private void refreshLeaderboard() {
        tableModel.setRowCount(0); // 기존 테이블 칸 비우기

        String dbUrl = "jdbc:sqlite:ranking.db";
        
        // 💡 [보정] 테이블이 없을 경우를 대비해 자동으로 뼈대를 생성하는 SQL 문을 선언합니다.
        String createTableSql = "CREATE TABLE IF NOT EXISTS RANKING (username TEXT, score INTEGER);";
        String selectSql = "SELECT username, score FROM RANKING ORDER BY score DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            
            // 1. ⚠️ 핵심 안전장치: 랭킹 테이블이 파괴되어 없다면 여기서 자동으로 창조합니다.
            stmt.execute(createTableSql);
            
            // 2. 이어서 안전하게 랭킹 데이터 조회 수행
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("username");
                    int s = rs.getInt("score");
                    tableModel.addRow(new Object[]{rank + "위", name, s + " 점"});
                    rank++;
                }
            }
            
            // 3. 만약 DB를 방금 밀어서 데이터가 하나도 없다면 텅 비워두는 대신 안내 문구 한 줄 출력
            if (tableModel.getRowCount() == 0) {
                tableModel.addRow(new Object[]{"-", "현재 등록된 랭킹 기록이 없습니다. 1등을 차지하세요!", "-"});
            }
            
        } catch (java.sql.SQLException se) {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"❌", "데이터베이스 연결 실패", "SQLException"});
            
            btnRegister.setEnabled(false); 
            btnRegister.setText("랭킹 기능 비활성화 🔒");
            
            JOptionPane.showMessageDialog(ResultScreen.this, 
                    "DB 연결에 실패하여 랭킹 등록 기능이 격리되었습니다.\n게임 결과는 정상적으로 마감됩니다.", 
                    "⚠️ DB 에러 (SQLException)", JOptionPane.WARNING_MESSAGE);
            se.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}