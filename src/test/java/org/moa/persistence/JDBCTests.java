package org.moa.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JDBCTests {
	@BeforeAll
	public static void setup() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("JDBC 드라이버 연결이 된다.")
	public void testConnection() {
		String url = "jdbc:mysql://localhost:3306/moa";
		try (Connection con = DriverManager.getConnection(url, "root", System.getenv("MOA_DB_PASSWORD"))) {
			log.info(con);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
