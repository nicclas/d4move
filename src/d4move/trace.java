package d4move;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Date;
import java.util.Vector;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class trace {

	public trace() {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		PrintWriter writerLocations = null;

		try {
			writerLocations = new PrintWriter("moveLatLon.js",
					"UTF-8");
		} catch (IOException ex) {
			Logger lgr = Logger.getLogger(process.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);
		}
		Properties props = new Properties();

		InputStream in = null;

		try {
			in = process.class.getResourceAsStream("/db.properties");
			props.load(in);

		} catch (IOException ex) {

			Logger lgr = Logger.getLogger(process.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);

		} finally {

			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				Logger lgr = Logger.getLogger(process.class.getName());
				lgr.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}

		String url = props.getProperty("db.url");
		String user = props.getProperty("db.user");
		String passwd = props.getProperty("db.passwd");

		Date lefttime = new Date();
		Date righttime = new Date();
		try {

			con = DriverManager.getConnection(url, user, passwd);
			pst = con
					.prepareStatement("SELECT time FROM senegal_set2_small ORDER BY time ASC LIMIT 1");
			rs = pst.executeQuery();

			while (rs.next()) {
				System.out.println(rs.getDate("time"));
				lefttime = rs.getDate("time");
			}

			pst = con
					.prepareStatement("SELECT time FROM senegal_set2_small ORDER BY time DESC LIMIT 1");
			rs = pst.executeQuery();

			while (rs.next()) {
				System.out.println(rs.getDate("time"));
				righttime = rs.getDate("time");
			}

			writerLocations.println("// From: " + lefttime);
			writerLocations.println("// To: " + righttime);

			int day = 0;
			int dayspan = 1 * 24 + 1;
			int offsetday = 0 * 24;
			int maxid = 1000;

			System.out.println("User data to write: " + maxid);

			int user_id = 0;

			String sql = "SELECT DISTINCT (user_id) FROM senegal_set2_small WHERE time BETWEEN (DATE('"
					+ lefttime
					+ "') + INTERVAL '"
					+ (day + offsetday)
					+ " hours') AND (DATE('"
					+ lefttime
					+ "') + INTERVAL '"
					+ (day + offsetday + dayspan)
					+ " hours') and user_id <= "
					+ maxid + " order by user_id";
			pst = con.prepareStatement(sql);
			rs = pst.executeQuery();

			Vector<Double>[][] vx = new Vector[dayspan + 1][maxid + 1];
			Vector<Double>[][] vy = new Vector[dayspan + 1][maxid + 1];
			for (int i = 0; i <= dayspan; i++) {
				for (int j = 1; j <= maxid; j++) {
					vx[i][j] = new Vector<Double>();
					vy[i][j] = new Vector<Double>();
				}
			}

			while (rs.next()) {
				user_id = rs.getInt("user_id");

				System.out.println("--- New user: " + user_id);

				int id = 0;
				Date time = new Date();
				double lon = 0;
				double lat = 0;
				int hour = -1;

				int prev_id = 0;
				double prev_lon = -999;
				double prev_lat = -999;
				int prev_hour = -1;

				sql = "SELECT trunc((EXTRACT(EPOCH FROM senegal_set2_small.time) - (EXTRACT(EPOCH FROM (DATE('"
						+ lefttime
						+ "') + INTERVAL '"
						+ offsetday
						+ " hours'))))/3600)::Integer AS td,"
						+ " senegal_ant_pos.lon, senegal_ant_pos.lat, senegal_set2_small.time,senegal_set2_small.user_id"
						+ " FROM senegal_ant_pos,senegal_set2_small"
						+ " WHERE senegal_ant_pos.site_id = senegal_set2_small.antenna_id"
						+ " AND time BETWEEN (DATE('"
						+ lefttime
						+ "') + INTERVAL '"
						+ (day + offsetday)
						+ " hours') AND (DATE('"
						+ lefttime
						+ "') + INTERVAL '"
						+ (day + offsetday + dayspan )
						+ " hours') AND senegal_set2_small.user_id = '"
						+ user_id
						+ "' order by time";

				pst = con.prepareStatement(sql);
				rs2 = pst.executeQuery();

				while (rs2.next()) {

					id = rs2.getInt("user_id");
					time = rs2.getTimestamp("time");
					lat = rs2.getDouble("lat");
					lon = rs2.getDouble("lon");
					hour = rs2.getInt("td");

					System.out.println(id + " " + time + " " + hour + " ("
							+ lat + "," + lon + ")");
					if (hour != prev_hour) {

						if (prev_hour > 0
								&& !(lon == vx[prev_hour][id].lastElement() && lat == vy[prev_hour][id]
										.lastElement())) {
							vx[prev_hour][id].add(lon);
							vy[prev_hour][id].add(lat);
							System.out.print("added to previous - ");
							System.out.println(id + " " + time + " "
									+ prev_hour + " (" + lat + "," + lon + ")");
						}
						vx[hour][id].add(lon);
						vy[hour][id].add(lat);
						System.out.println("init new slice - added");
					}

					if ((hour == prev_hour) && id == prev_id
							&& !(lon == prev_lon && lat == prev_lat)) {
						vx[hour][id].add(lon);
						vy[hour][id].add(lat);
						System.out.println("added");
					}

					// Check if position is same,

					prev_id = id;
					prev_lon = lon;
					prev_lat = lat;
					prev_hour = hour;
				}
			}

			writerLocations.print("var jsonStruct = {\nlatlng: [");
			boolean first = true;
			for (int i = 0; i <= dayspan; i++) {
				for (int k = 1; k <= maxid; k++) {
					if (vx[i][k].size() > 1) {
						for (int j = 0; j < vx[i][k].size(); j++) {
							if (!(first)) {
								writerLocations.print(","
										+ vx[i][k].elementAt(j));
							} else {
								writerLocations.print(""
										+ vx[i][k].elementAt(j));
								first = false;
							}
							writerLocations.print("," + vy[i][k].elementAt(j));
						}
					}
				}
			}

			writerLocations.print("],\nsegments: [");
			int points = 0;
			first = true;
			for (int i = 0; i <= dayspan; i++) {
				for (int k = 1; k <= maxid; k++) {
					if (vx[i][k].size() > 1) {
						points++;
					}
				}

				if (!(first)) {
					writerLocations.print("," + points);
				} else {
					writerLocations.print("" + points);
				}
				first = false;
			}

			writerLocations.print("],\nsegmentlength: [0,");
			// List the index (position) of the segments (in order)
			first = true;
			boolean add = false;
			points = 0;
			for (int i = 0; i <= dayspan; i++) {
				for (int k = 1; k <= maxid; k++) {
					if (vx[i][k].size() > 1) {
						points += (vx[i][k].size());
						add = true;
					} else {
						points += 0;
					}
					if (add) {
						if (!(first)) {
							writerLocations.print("," + points);
						} else {
							writerLocations.print("" + points);
						}
						add = false;
						first = false;
					}
				}
			}
			writerLocations.println("]");
			writerLocations.println("};");

		} catch (Exception ex) {
			Logger lgr = Logger.getLogger(process.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);

		} finally {

			try {
				if (writerLocations != null) {
					writerLocations.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (rs2 != null) {
					rs2.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				Logger lgr = Logger.getLogger(process.class.getName());
				lgr.log(Level.WARNING, ex.getMessage(), ex);
			}
		}

	}
}
