package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private DataSource dataSource;
    private static final Logger LOG = LoggerFactory.getLogger(OrderRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(OrderRepository.class);

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public List<Food> getMenu(int id) {
        List<Food> menu = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM food WHERE restaurantId=" + id;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sqlQuery)) {
            while (rs.next()) {
                menu.add(createFood(rs));
            }
        } catch (SQLException e) {
            LOG.error("Unable to fetch menu for restaurant with ID: " + id);
        }

        return menu;
    }

    private Food createFood(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        Food food = new Food(id, name);

        auditLogger.audit("Created new food with ID: " + id);

        return food;
    }

    public void insertNewOrder(NewOrder newOrder, int userId) {
        LocalDate date = LocalDate.now();
        String sqlQuery = "INSERT INTO delivery (isDone, userId, restaurantId, addressId, date, comment)" + "values (FALSE, " + userId + ", ?, ?, " + "'" + date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth() + "', ?)";
        try {
            Connection connection = dataSource.getConnection();

            // Create the prepared statement to mitigate SQL injection
            PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
            preparedStatement.setInt(1, newOrder.getRestaurantId());
            preparedStatement.setInt(2, newOrder.getAddress());
            preparedStatement.setString(3, newOrder.getComment());

            preparedStatement.executeUpdate();

            auditLogger.audit("order.create: " + newOrder.toString());

            sqlQuery = "SELECT MAX(id) FROM delivery";
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sqlQuery);

            if (rs.next()) {
                int deliveryId = rs.getInt(1);
                sqlQuery = "INSERT INTO delivery_item (amount, foodId, deliveryId)" + "values";
                for (int i = 0; i < newOrder.getItems().length; i++) {
                    FoodItem item = newOrder.getItems()[i];
                    String deliveryItem = "";
                    if (i > 0) {
                        deliveryItem = ",";
                    }
                    deliveryItem += "(" + item.getAmount() + ", " + item.getFoodId() + ", " + deliveryId + ")";
                    sqlQuery += deliveryItem;
                }
                System.out.println(sqlQuery);
                statement.executeUpdate(sqlQuery);
            }
        } catch (SQLException e) {
            LOG.error("Unable to insert new order", e);
        }
    }

    public Object getAddresses(int userId) {
        List<Address> addresses = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM address WHERE userId=" + userId;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sqlQuery)) {
            while (rs.next()) {
                addresses.add(createAddress(rs));
            }
        } catch (SQLException e) {
            LOG.error("Unable to get addresses for user with ID: " + userId, e);
        }

        return addresses;
    }

    private Address createAddress(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);

        Address address = new Address(id, name);

        auditLogger.audit("Created new address with ID: " + id);

        return address;
    }
}
