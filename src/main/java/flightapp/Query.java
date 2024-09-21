package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private static final String USERNAME_SQL = "SELECT * FROM USERS_khalisam WHERE username = ?";
  private static final String CREATE_USER_SQL = "INSERT INTO USERS_khalisam VALUES(?, ?, ?)";
  private static final String DIRECT_FLIGHT_SQL =
  "SELECT TOP (?) " +
  "day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price, fid " +
  "FROM FLIGHTS " +
  "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 " +
  "ORDER BY actual_time, fid ASC";
  private static final String INDIRECT_FLIGHT_SQL =
  "SELECT TOP (?) " +
  "F1.fid AS fid," +
  "F2.fid AS fid_two," +
  "F1.day_of_month AS day_of_month," +
  "F1.carrier_id AS carrier1," +
  "F1.flight_num AS flight1," +
  "F2.carrier_id AS carrier2," +
  "F2.flight_num AS flight2," +
  "F1.origin_city AS origin_city," +
  "F2.dest_city AS dest_city, " +
  "F2.origin_city AS origin_city_two," +
  "F1.actual_time AS time_one," +
  "F2.actual_time AS time_two," +
  "(F1.actual_time + F2.actual_time) AS total_time," +
  "F1.capacity AS capacity1," +
  "F2.capacity AS capacity2," +
  "F1.price AS price_one," +
  "F2.price AS price_two," +
  "(F1.price + F2.price) AS total_price " +
  "FROM FLIGHTS AS F1 " +
  "JOIN FLIGHTS AS F2 ON F1.dest_city = F2.origin_city " +
  "WHERE F1.origin_city = ? AND F2.dest_city = ? " +
  "AND F1.day_of_month = ? AND F2.day_of_month = ? " +
  "AND F1.fid != F2.fid AND F1.canceled = 0 AND F2.canceled = 0 " +
  "ORDER BY F1.actual_time + F2.actual_time, fid, fid_two ASC;";
  private static final String CHECK_EXISTING_SQL =
  "SELECT R.unique_id FROM RESERVATIONS_khalisam R JOIN FLIGHTS F ON R.flight_one = F.fid OR R.flight_two = F.fid WHERE R.username = ? AND F.day_of_month = ?";
  private static final String CHECK_FLIGHT_CAPACITY_SQL =
    "SELECT COUNT(R.unique_id) AS available_seats FROM FLIGHTS F LEFT JOIN RESERVATIONS_khalisam R ON R.flight_one = F.fid OR R.flight_two = F.fid WHERE F.fid = ? GROUP BY F.capacity";
  private static final String NEXT_RESERVATION_SQL = "SELECT COUNT(*) + 1 AS next_id FROM RESERVATIONS_khalisam";
  private static final String INSERT_NEW_RESERVATION_SQL =
  "INSERT INTO RESERVATIONS_khalisam (unique_id, paid, username, flight_one, flight_two) VALUES (?, 0, ?, ?, ?)";
  private static final String ID_SQL = "SELECT COUNT(*) AS count "
  + "FROM RESERVATIONS_khalisam AS R "
  + "WHERE R.unique_id = ? AND R.username = ? AND R.paid = 0";
  private static final String USER_BALANCE_SQL = "SELECT balance FROM USERS_khalisam WHERE username = ?";
  private static final String RESERVATION_PRICE_SQL = "SELECT SUM(price) AS itineraryCost "
  + "FROM (SELECT flight_one, flight_two FROM RESERVATIONS_khalisam WHERE unique_id = ? AND username = ?) AS id "
  + "JOIN Flights AS F ON id.flight_one = F.fid OR id.flight_two = F.fid";
  private static final String USER_PAYMENT_SQL = "UPDATE USERS_khalisam SET balance = ? WHERE username = ?";
  private static final String CHECK_PAID_SQL = "UPDATE RESERVATIONS_khalisam SET paid = 1 WHERE unique_id = ? AND username = ?";
  private static final String RESERVE_SQL = "SELECT unique_id, flight_one, flight_two, paid FROM RESERVATIONS_khalisam WHERE username = ?";
  private static final String FLIGHT_SQL = "SELECT day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price FROM FLIGHTS WHERE fid = ?";
  private static final String DELETE_RES_SQL = "DELETE FROM RESERVATIONS_khalisam";
  private static final String DELETE_USE_SQL = "DELETE FROM USERS_khalisam";


    //
  // Instance variables
  //
  private PreparedStatement delete_res_;
  private PreparedStatement delet_use_;
  private PreparedStatement flightCapacityStmt;
  private PreparedStatement username_;
  private PreparedStatement create_user_;
  private String curr;
  private boolean if_searched = false;
  private PreparedStatement check_existing_;
  private PreparedStatement check_flight_capacity_;
  private PreparedStatement next_reservation_;
  private PreparedStatement insert_new_reservation_;
  private PreparedStatement direct_flight_statement;
  private PreparedStatement id_;
  private PreparedStatement user_balance_;
  private PreparedStatement reservation_price_;
  private PreparedStatement user_payment_;
  private PreparedStatement check_paid_;
  private PreparedStatement reserve_;
  private PreparedStatement flight_;
  private String login;
  private ArrayList<Itinerary> flight_information = new ArrayList<>();




  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      delete_res_.execute();
      delet_use_.execute();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    delete_res_ = conn.prepareStatement(DELETE_RES_SQL);
    delet_use_ = conn.prepareStatement(DELETE_USE_SQL);
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    username_ = conn.prepareStatement(USERNAME_SQL);
    create_user_ = conn.prepareStatement(CREATE_USER_SQL);
    id_ = conn.prepareStatement(ID_SQL);
    user_balance_ = conn.prepareStatement(USER_BALANCE_SQL);
    reservation_price_ = conn.prepareStatement(RESERVATION_PRICE_SQL);
    user_payment_ = conn.prepareStatement(USER_PAYMENT_SQL);
    check_paid_ = conn.prepareStatement(CHECK_PAID_SQL);
    reserve_ = conn.prepareStatement(RESERVE_SQL);
    flight_ = conn.prepareStatement(FLIGHT_SQL);
    check_existing_ = conn.prepareStatement(CHECK_EXISTING_SQL);
    check_flight_capacity_ = conn.prepareStatement(CHECK_FLIGHT_CAPACITY_SQL);
    next_reservation_ = conn.prepareStatement(NEXT_RESERVATION_SQL);
    insert_new_reservation_ = conn.prepareStatement(INSERT_NEW_RESERVATION_SQL);
    direct_flight_statement = conn.prepareStatement(DIRECT_FLIGHT_SQL);

  }


  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // Check if the user already logged in
    if(login != null) {
      return "User already logged in\n";
    }

    try {
      username_.clearParameters();
      username_.setString(1, username);

      try (ResultSet res = username_.executeQuery()){
        byte[] hashedPassord = null;

        if (res.next()) {
          hashedPassord = res.getBytes("password");
        }

        if (hashedPassord != null && PasswordUtils.plaintextMatchesSaltedHash(password, hashedPassord)) {
          curr = username;
          login = username;
          flight_information.clear();
          return "Logged in as " + curr + "\n";
        } else {
          return "Login failed\n";
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Login failed\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) {
      return "Failed to create user\n";
    }

    try {
      conn.setAutoCommit(false);
      username_.setString(1, username);


      ResultSet res = username_.executeQuery();
      if (res.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Failed to create user\n";
      }
      res.close();

      create_user_.setString(1,username);
      create_user_.setBytes(2, PasswordUtils.saltAndHashPassword(password));
      create_user_.setInt(3, initAmount);
      create_user_.executeUpdate();
      conn.commit();
      conn.setAutoCommit(true);

      return "Created user " + username + "\n";

    } catch (SQLException e) {
      e.printStackTrace();
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        if (isDeadlock(e)) {
          return transaction_createCustomer(username, password, initAmount);
        }
        return "Failed to create user\n";
      } catch(SQLException e_) {
        return "Failed to create user\n";
      }
    }
  }


  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity,
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    if (numberOfItineraries < 0) {
      return "Failed to search\n";
    }
    flight_information.clear();
    StringBuilder sb = new StringBuilder();
    int count = 0;
    int itineraryIdCounter = 0;
    try {
      direct_flight_statement.setInt(1, numberOfItineraries);
      direct_flight_statement.setString(2, originCity);
      direct_flight_statement.setString(3, destinationCity);
      direct_flight_statement.setInt(4, dayOfMonth);

      ResultSet res = direct_flight_statement.executeQuery();
      while (res.next() && count < numberOfItineraries) {
        count++;
        int dayofmonth = res.getInt("day_of_month");
        String carrierid = res.getString("carrier_id");
        String flightnum = res.getString("flight_num");
        String origincity = res.getString("origin_city");
        String destcity = res.getString("dest_city");
        int actualtime = res.getInt("actual_time");
        int capacity = res.getInt("capacity");
        int price = res.getInt("price");
        int id = res.getInt("fid");

        Flight flight = new Flight(id, dayofmonth, carrierid, flightnum, origincity, destcity, actualtime, capacity, price);
        Itinerary iten_one = new Itinerary(flight, itineraryIdCounter);

        itineraryIdCounter++;
        flight_information.add(iten_one);
      }
      res.close();
       if(!directFlight){
        PreparedStatement indirect_flight_Statement = conn.prepareStatement(INDIRECT_FLIGHT_SQL);
        indirect_flight_Statement.setInt(1, numberOfItineraries - count);
        indirect_flight_Statement.setString(2, originCity);
        indirect_flight_Statement.setString(3, destinationCity);
        indirect_flight_Statement.setInt(4, dayOfMonth);
        indirect_flight_Statement.setInt(5, dayOfMonth);

        ResultSet res_two = indirect_flight_Statement.executeQuery();
        while (res_two.next() && count < numberOfItineraries) {
          count++;
          int dayofmonth = res_two.getInt("day_of_month");
          String carrier1 = res_two.getString("carrier1");
          String flight1 = res_two.getString("flight1");
          String carrier2 = res_two.getString("carrier2");
          String flight2 = res_two.getString("flight2");
          String origincity = res_two.getString("origin_city");
          String origincitytwo = res_two.getString("origin_city_two");
          String destCity = res_two.getString("dest_city");
          int timeone = res_two.getInt("time_one");
          int timetwo = res_two.getInt("time_two");
          int capacity1 = res_two.getInt("capacity1");
          int capacity2 = res_two.getInt("capacity2");
          int priceone = res_two.getInt("price_one");
          int pricetwo = res_two.getInt("price_two");
          int id = res_two.getInt("fid");
          int idtwo = res_two.getInt("fid_two");

          Flight flight_one = new Flight(id, dayofmonth, carrier1, flight1, origincity, origincitytwo, timeone, capacity1, priceone);
          Flight flight_two = new Flight(idtwo, dayofmonth, carrier2, flight2, origincitytwo, destCity, timetwo, capacity2, pricetwo);
          Itinerary iten = new Itinerary(flight_one, flight_two, itineraryIdCounter);
          itineraryIdCounter++;
          flight_information.add(iten);
        }
        res_two.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to search\n";
    }

    if (flight_information.isEmpty()) {
      return "No flights match your selection\n";
    }

    Collections.sort(flight_information, new ItineraryComparator());
    for(int i = 0; i < flight_information.size(); i++) {
      Itinerary it_two = flight_information.get(i);
      sb.append("Itinerary ").append(i).append(": ");
      if(it_two.direct) {
        sb.append("1 flight(s), ").append(it_two.time).append(" minutes\n");
        sb.append(it_two.first_flight.toString()).append("\n");
      } else {
        sb.append("2 flight(s), ").append(it_two.time).append(" minutes\n");
        sb.append(it_two.first_flight.toString()).append("\n");
        sb.append(it_two.second_flight.toString()).append("\n");
      }
      it_two.id = i;
    }
    if_searched = true;
    return sb.toString();
  }

  public class Itinerary{
    public Flight first_flight;
    public Flight second_flight;
    public boolean direct;
    public int time;
    public int id;

    public Itinerary(Flight first_flight, int id) {
      this.first_flight = first_flight;
      this.direct = true;
      this.time = first_flight.time;
      this.id = id;
    }

    public Itinerary(Flight first_flight, Flight second_flight, int id){
      this.first_flight = first_flight;
      this.second_flight = second_flight;
      this.direct = false;
      this.time = first_flight.time + second_flight.time;
      this.id = id;
    }
  }

  class ItineraryComparator implements Comparator<Itinerary> {
    @Override
    public int compare(Itinerary i1, Itinerary i2) {
      if (i1.time != i2.time) {
        return Integer.compare(i1.time, i2.time);
      }
      int first_compare = Integer.compare(i1.first_flight.fid, i2.first_flight.fid);
      if (first_compare != 0) {
        return first_compare;
      }
      if (!i1.direct && !i2.direct) {
        return Integer.compare(i1.second_flight.fid, i2.second_flight.fid);
      }
      return 0;
    }
  }


public String transaction_book(int itineraryId) {
  if (!is_logged_in()) {
      return "Cannot book reservations, not logged in\n";
  } else if (!if_searched || itineraryId < 0 || itineraryId >= flight_information.size()) {
      return "No such itinerary " + itineraryId + "\n";
  }

  try {
    conn.setAutoCommit(false);
    Itinerary itinerary = flight_information.get(itineraryId);

    check_existing_.clearParameters();
    check_existing_.setString(1, login);
    check_existing_.setInt(2, itinerary.first_flight.dayOfMonth);
    ResultSet rs = check_existing_.executeQuery();

    if (rs.next()) {
      conn.rollback();
      conn.setAutoCommit(true);
      return "You cannot book two flights in the same day\n";
    }

    check_flight_capacity_.clearParameters();
    check_flight_capacity_.setInt(1, itinerary.first_flight.fid);
    int flightCapacity = checkFlightCapacity(itinerary.first_flight.fid);
    ResultSet rs_two = check_flight_capacity_.executeQuery();
    if (rs_two.next()) {
      if ((flightCapacity - rs_two.getInt("available_seats")) == 0) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Booking failed\n";
      }
    }

    if (itinerary.second_flight != null) {
      check_flight_capacity_.clearParameters();
      check_flight_capacity_.setInt(1, itinerary.second_flight.fid);
      int flightCapacity_two = checkFlightCapacity(itinerary.second_flight.fid);
      ResultSet rs_three = check_flight_capacity_.executeQuery();
      if (rs_three.next()) {
        if ((flightCapacity_two - rs_three.getInt("available_seats")) == 0) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        }
      }
    }

    next_reservation_.clearParameters();
    ResultSet rs_four = next_reservation_.executeQuery();
    int nextId = 1;
    if (rs_four.next()) {
      nextId = rs_four.getInt("next_id");
    }

    if (itinerary.second_flight == null) {
      insert_new_reservation_.clearParameters();
      insert_new_reservation_.setInt(1, nextId++);
      insert_new_reservation_.setString(2, login);
      insert_new_reservation_.setInt(3, itinerary.first_flight.fid);
      insert_new_reservation_.setNull(4, Types.INTEGER);
      insert_new_reservation_.executeUpdate();
    } else if (itinerary.second_flight != null){
      insert_new_reservation_.clearParameters();
      insert_new_reservation_.setInt(1, nextId++);
      insert_new_reservation_.setString(2, login);
      insert_new_reservation_.setInt(3, itinerary.first_flight.fid);
      insert_new_reservation_.setInt(4, itinerary.second_flight.fid);
      insert_new_reservation_.executeUpdate();
    }

    conn.commit();
    conn.setAutoCommit(true);
    return "Booked flight(s), reservation ID: " + (nextId - 1) + "\n";
  } catch (SQLException e) {
    e.getStackTrace();
    try {
      conn.rollback();
      conn.setAutoCommit(true);
      if (isDeadlock(e)) {
        return transaction_book(itineraryId);
      }
      return "Booking failed\n";
    } catch (SQLException e_) {
      return "Booking failed\n";
    }
  }
}

  public String transaction_pay(int reservationId) {
    if (!is_logged_in()) {
      return "Cannot pay, not logged in\n";
    }

    try {
      if (!unpaid_reservations(reservationId)) {
        return "Cannot find unpaid reservation " + reservationId + " under user: " + login + "\n";
      }

      int itineraryCost = get_itinerary_cost(reservationId);
      int userBalance = get_user_balance();

      if (userBalance < itineraryCost) {
        return "User has only " + userBalance + " in account but itinerary costs " + itineraryCost + "\n";
      }

      update_payement_status(reservationId);
      update_user_balance(userBalance - itineraryCost);

      return "Paid reservation: " + reservationId + " remaining balance: " + (userBalance - itineraryCost) + "\n";
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to pay for reservation " + reservationId + "\n";
    }
  }


  private boolean unpaid_reservations(int reservationId) throws SQLException {
    id_.clearParameters();
    id_.setInt(1, reservationId);
    id_.setString(2, login);
    try (ResultSet res = id_.executeQuery()) {
      if (res.next()) {
        return res.getInt("count") > 0;
      }
    }
    return false;
  }

  private int get_itinerary_cost(int reservationId) throws SQLException {
    reservation_price_.clearParameters();
    reservation_price_.setInt(1, reservationId);
    reservation_price_.setString(2, login);
    try (ResultSet res = reservation_price_.executeQuery()) {
      if (res.next()) {
        return res.getInt("itineraryCost");
      }
    }
    return 0;
  }

  private int get_user_balance() throws SQLException {
    user_balance_.clearParameters();
    user_balance_.setString(1, login);
    try (ResultSet res = user_balance_.executeQuery()) {
      if (res.next()) {
        return res.getInt("balance");
      }
    }
    return 0;
  }

  private void update_payement_status(int reservationId) throws SQLException {
    check_paid_.clearParameters();
    check_paid_.setInt(1, reservationId);
    check_paid_.setString(2, login);
    check_paid_.executeUpdate();
  }

  private void update_user_balance(int newBalance) throws SQLException {
    user_payment_.clearParameters();
    user_payment_.setInt(1, newBalance);
    user_payment_.setString(2, login);
    user_payment_.executeUpdate();
  }

  public String transaction_reservations() {
    if (!is_logged_in()) {
      return "Cannot view reservations, not logged in\n";
    }
    try {
      StringBuffer sb = new StringBuffer();
      reserve_.clearParameters();
      reserve_.setString(1, login.toLowerCase());
      ResultSet res = reserve_.executeQuery();

      if (!res.isBeforeFirst()) { // from ed
        return "No reservation found\n";
      }

      while (res.next()) {
        int flightOne = res.getInt("flight_one");
        int flightTwo = res.getInt("flight_two");
        int paid = res.getInt("paid");
        int uniqueId = res.getInt("unique_id");
        boolean ifPaid;
        if (paid == 1) {
          ifPaid = true;
        } else {
          ifPaid = false;
        }
        get_flight_details(flightOne, uniqueId, ifPaid, sb);

        if (flightTwo > 0) {
          get_flight_details(flightTwo, uniqueId, ifPaid, sb);
        }
      }
      res.close();
      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to retrieve reservations\n";
    }
  }

  private void get_flight_details(int flightId, int uniqueId, boolean ifPaid, StringBuffer sb) throws SQLException {
    flight_.clearParameters();
    flight_.setInt(1, flightId);
    try (ResultSet res = flight_.executeQuery()) {
        if (res.next()) {
            Flight flight = new Flight(
                  flightId,
                  res.getInt("day_of_month"),
                  res.getString("carrier_id"),
                  res.getString("flight_num"),
                  res.getString("origin_city"),
                  res.getString("dest_city"),
                  res.getInt("actual_time"),
                  res.getInt("capacity"),
                  res.getInt("price"));
            sb.append("Reservation " + uniqueId + " paid: " + ifPaid + ":\n" + flight.toString() + "\n");
        }
    }
  }

  private boolean is_logged_in() {
    return login != null;
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}