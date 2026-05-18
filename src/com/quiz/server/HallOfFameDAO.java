package com.quiz.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HallOfFameDAO {
    // SQLite DB 파일 경로 (프로젝트 폴더 바로 밑에 ranking.db 파일이 생깁니다)
    private static final String DB_URL = "jdbc:sqlite:ranking.db";

    // 1. DB 연결 메서드
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // 2. 테이블 초기화 메서드 (테이블이 없으면 자동으로 만듭니다) 
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS RANKING ("
                   + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                   + "username TEXT NOT NULL,"
                   + "score INTEGER NOT NULL"
                   + ");";
        
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ DB 테이블 초기화 완료 (또는 이미 존재함)");
        } catch (SQLException e) {
            System.out.println("❌ 테이블 생성 실패");
            e.printStackTrace();
        }
    }

    // 3. 점수 등록 메서드 (PreparedStatement를 사용하여 SQL 인젝션 방지) [cite: 214, 215, 237]
    public void insertScore(String username, int score) {
        String sql = "INSERT INTO RANKING(username, score) VALUES(?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            System.out.println("💾 점수 등록 성공: [" + username + "] -> " + score + "점");
        } catch (SQLException e) {
            System.out.println("❌ 점수 등록 실패 [cite: 256]");
            e.printStackTrace();
        }
    }

    // 4. 명예의 전당 TOP 10 조회 메서드 [cite: 188, 218, 238]
    public void displayTop10() {
        String sql = "SELECT username, score FROM RANKING ORDER BY score DESC LIMIT 10";

        System.out.println("\n=========================================");
        System.out.println("🏆 명예의 전당 TOP 10 🏆 [cite: 187, 188]");
        System.out.println("=========================================");

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int rank = 1;
            boolean hasData = false;
            
            while (rs.next()) {
                hasData = true;
                String name = rs.getString("username");
                int score = rs.getInt("score");
                System.out.println("  " + rank + "위 | " + name + " \t- " + score + "점");
                rank++;
            }
            
            if (!hasData) {
                System.out.println("  아직 등록된 랭킹이 없습니다. 첫 주인공이 되어보세요!");
            }
            System.out.println("-----------------------------------------");
            
        } catch (SQLException e) {
            System.out.println("❌ 랭킹 조회 실패 [cite: 256]");
            e.printStackTrace();
        }
    }

    // 5. 테스트를 위한 메인 메서드
    public static void main(String[] args) {
        HallOfFameDAO dao = new HallOfFameDAO();
        
        // 1단계: DB와 테이블 만들기
        dao.createTable();
        
        // 2단계: 가짜 플레이어 데이터 집어넣기 (테스트용)
        System.out.println("\n--- 랭킹 등록 테스트 ---");
        dao.insertScore("김민성", 850);
        dao.insertScore("우찬희", 920);
        dao.insertScore("유환휘", 740);
        dao.insertScore("JavaKing", 990);
        
        // 3단계: 점수 높은 순으로 Top 10 출력하기 [cite: 238]
        dao.displayTop10();
    }
}