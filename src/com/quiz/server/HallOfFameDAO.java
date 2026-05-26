package com.quiz.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 데이터베이스를 통해 명예의 전당(랭킹) 데이터를 관리하는 DAO 클래스.
 * DB 파일은 실행 디렉터리의 {@code ranking.db}에 자동 생성된다.
 */
public class HallOfFameDAO {

    private static final String DB_URL = "jdbc:sqlite:ranking.db";

    /**
     * 데이터베이스 연결을 생성하여 반환한다.
     *
     * @return 활성 DB 연결
     * @throws SQLException DB 연결 실패 시
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * RANKING 테이블이 없으면 새로 생성한다.
     * 이미 테이블이 존재하는 경우 아무런 변경도 일어나지 않는다.
     */
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS RANKING ("
                   + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                   + "username TEXT NOT NULL,"
                   + "score INTEGER NOT NULL"
                   + ");";

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("DB 테이블 초기화 완료 (또는 이미 존재함)");
        } catch (SQLException e) {
            System.out.println("테이블 생성 실패");
            e.printStackTrace();
        }
    }

    /**
     * 플레이어의 점수를 RANKING 테이블에 삽입한다.
     * SQL 인젝션 방지를 위해 {@link PreparedStatement}를 사용한다.
     *
     * @param username 플레이어 닉네임
     * @param score    최종 점수
     */
    public void insertScore(String username, int score) {
        String sql = "INSERT INTO RANKING(username, score) VALUES(?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            System.out.println("점수 등록 성공: [" + username + "] -> " + score + "점");
        } catch (SQLException e) {
            System.out.println("점수 등록 실패");
            e.printStackTrace();
        }
    }

    /**
     * 점수 상위 10명의 랭킹을 콘솔에 출력한다.
     */
    public void displayTop10() {
        String sql = "SELECT username, score FROM RANKING ORDER BY score DESC LIMIT 10";

        System.out.println("\n=========================================");
        System.out.println("           🏆 명예의 전당 TOP 10 🏆 ");
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
            System.out.println("랭킹 조회 실패");
            e.printStackTrace();
        }
    }
}
