package lama.sqlite3.gen;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import lama.Main.StateToReproduce;
import lama.Query;
import lama.QueryAdapter;
import lama.Randomly;
import lama.sqlite3.SQLite3Visitor;
import lama.sqlite3.ast.SQLite3Constant;
import lama.sqlite3.ast.SQLite3Expression;
import lama.sqlite3.schema.SQLite3Schema.Column;
import lama.sqlite3.schema.SQLite3Schema.Table;

public class SQLite3RowGenerator {

	public static Query insertRow(Table table, Connection con, StateToReproduce state) throws SQLException {
		SQLite3RowGenerator generator = new SQLite3RowGenerator();
		String query = generator.insertRow(table);
		return new QueryAdapter(query) { 
			public void execute(Connection con) throws SQLException {
				try {
					super.execute(con);
				} catch (SQLException e) {
					if (generator.mightFail && e.getMessage().startsWith("[SQLITE_CONSTRAINT]")) {
						return;
					} else if (e.getMessage().startsWith("[SQLITE_FULL]")) {
						return;
					} else if (e.getMessage().startsWith("[SQLITE_ERROR] SQL error or missing database (integer overflow)")) {
						return;
					} else if (e.getMessage().startsWith("[SQLITE_ERROR] SQL error or missing database (foreign key mismatch")) {
						return;
					} else if (e.getMessage().startsWith("[SQLITE_CONSTRAINT]  Abort due to constraint violation (FOREIGN KEY constraint failed)")) {
						return;
					} else {
						throw e;
					}
				}
			};
		};
	}

	private boolean mightFail;

	private String insertRow(Table table) {
		StringBuilder sb = new StringBuilder();
		// TODO: see
		// http://sqlite.1065341.n5.nabble.com/UPSERT-clause-does-not-work-with-quot-NOT-NULL-quot-constraint-td106957.html
		sb.append("INSERT ");
		if (Randomly.getBoolean()) { // FIXME Randomly.getBoolean()
			sb.append("OR IGNORE "); // TODO: try to generate REPLACE
		} else {
			mightFail = true;
			sb.append(Randomly.fromOptions("OR REPLACE ", "OR ABORT ", "OR FAIL ")); // "OR ROLLBACK ",
		}
		sb.append("INTO " + table.getName());
		if (Randomly.getBooleanWithSmallProbability()) {
			sb.append(" DEFAULT VALUES");
		} else {
			sb.append("(");
			List<Column> columns = appendColumnNames(table, sb);
			sb.append(")");
			sb.append(" VALUES ");
			int nrValues = 1 + Randomly.smallNumber();
			appendNrValues(sb, columns, nrValues);
		}
		sb.append(";");
		return sb.toString();
	}

	private static void appendNrValues(StringBuilder sb, List<Column> columns, int nrValues) {
		for (int i = 0; i < nrValues; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append("(");
			appendValue(sb, columns);
			sb.append(")");
		}
	}

	private static void appendValue(StringBuilder sb, List<Column> columns) {
		for (int i = 0; i < columns.size(); i++) {
			if (i != 0) {
				sb.append(", ");
			}
			SQLite3Expression literal;
			if (columns.get(i).isIntegerPrimaryKey()) {
				literal = SQLite3Constant.createIntConstant(Randomly.getInteger());
			} else {
//				literal = SQLite3ExpressionGenerator.getRandomExpression(new ArrayList<>(), false);
				literal = SQLite3ExpressionGenerator.getRandomLiteralValue(false);
			}
			SQLite3Visitor visitor = new SQLite3Visitor();
			visitor.visit(literal);
			sb.append(visitor.get());
		}
	}

	private static List<Column> appendColumnNames(Table table, StringBuilder sb) {
		List<Column> columns = table.getColumns();
		for (int i = 0; i < columns.size(); i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(columns.get(i).getName());
		}
		return columns;
	}

}