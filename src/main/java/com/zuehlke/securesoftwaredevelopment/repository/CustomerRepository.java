package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerRepository.class);

    private DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Person createPersonFromResultSet(ResultSet rs) throws SQLException {
        Person person = null;
        try {
            int id = rs.getInt(1);

            String firstName = rs.getString(2);
            String lastName = rs.getString(3);
            String personalNumber = rs.getString(4);
            String address = rs.getString(5);

            person = new Person(id, firstName, lastName, personalNumber, address);

            auditLogger.auditChange(new Entity("person.create", String.valueOf(id), "", person.toString()));
        } catch (SQLException e) {
            LOG.error("Unable to create new person", e);
        }

        return person;
    }

    public List<Customer> getCustomers() {
        List<com.zuehlke.securesoftwaredevelopment.domain.Customer> customers = new ArrayList<com.zuehlke.securesoftwaredevelopment.domain.Customer>();
        String query = "SELECT id, username FROM users";
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                customers.add(createCustomer(rs));
            }
        } catch (SQLException e) {
            LOG.error("Unable to execute customer search", e);
        }

        return customers;
    }

    private com.zuehlke.securesoftwaredevelopment.domain.Customer createCustomer(ResultSet rs) throws SQLException {
        return new com.zuehlke.securesoftwaredevelopment.domain.Customer(rs.getInt(1), rs.getString(2));
    }

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<Restaurant>();
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id ";
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                restaurants.add(createRestaurant(rs));
            }
        } catch (SQLException e) {
            LOG.error("Unable to execute restaurant search");
        }
        return restaurants;
    }

    private Restaurant createRestaurant(ResultSet rs) throws SQLException {
        Restaurant restaurant = null;
        try {
            int id = rs.getInt(1);
            String name = rs.getString(2);
            String address = rs.getString(3);
            String type = rs.getString(4);

            restaurant = new Restaurant(id, name, address, type);
        } catch (SQLException e) {
            LOG.error("Unable to create new restaurant", e);
        }

        return restaurant;
    }


    public Object getRestaurant(String id) {
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id WHERE r.id=" + id;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return createRestaurant(rs);
            }
        } catch (SQLException e) {
            LOG.error("Unable to fetch restaurant with ID " + id, e);
        }

        LOG.warn("Restaurant with ID not found: " + id);

        return null;
    }

    public void deleteRestaurant(int id) {
        String query = "DELETE FROM restaurant WHERE id=" + id;
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Unable to delete restaurant with ID " + id);
            } else {
                auditLogger.audit("Restaurant with ID: " + id + " deleted");
            }
        } catch (SQLException e) {
            LOG.error("Unable to delete restaurant with ID " + id, e);
        }
    }

    public void updateRestaurant(RestaurantUpdate restaurantUpdate) {
        String query = "UPDATE restaurant SET name = '" + restaurantUpdate.getName() + "', address='" + restaurantUpdate.getAddress() + "', typeId =" + restaurantUpdate.getRestaurantType() + " WHERE id =" + restaurantUpdate.getId();
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            Restaurant oldRestaurantData = (Restaurant) getRestaurant(String.valueOf(restaurantUpdate.getId()));

            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to update non-existing restaurant with ID " + restaurantUpdate.getId());
            } else {
                auditLogger.auditChange(new Entity("restaurant.update", String.valueOf(restaurantUpdate.getId()), oldRestaurantData.toString(), restaurantUpdate.toString()));
            }
        } catch (SQLException e) {
            LOG.error("Unable to execute restaurant update with ID " + restaurantUpdate.getId(), e);
        }
    }

    public Customer getCustomer(String id) {
        String sqlQuery = "SELECT id, username, password FROM users WHERE id=" + id;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sqlQuery)) {

            if (rs.next()) {
                return createCustomerWithPassword(rs);
            }
        } catch (SQLException e) {
            LOG.error("Unable to fetch customer with id " + id, e);
        }
        return null;
    }

    private Customer createCustomerWithPassword(ResultSet rs) throws SQLException {
        Customer newCustomer = null;
        try {
            int id = rs.getInt(1);
            String username = rs.getString(2);
            String password = rs.getString(3);

            newCustomer = new Customer(id, username, password);
            auditLogger.audit("Created a new user with username: " + username + " and ID: " + id);
        } catch (SQLException e) {
            LOG.error("Unable to create customer", e);
        }

        return newCustomer;
    }


    public void deleteCustomer(String id) {
        String query = "DELETE FROM users WHERE id=" + id;
        int updated = 0;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to delete an unknown user with ID: " + id);
            } else {
                auditLogger.audit("Deleted custoemr with ID: " + id);
            }
        } catch (SQLException e) {
            LOG.error("Unable to delete customer with ID: " + id, e);
        }
    }

    public void updateCustomer(CustomerUpdate customerUpdate) {
        String query = "UPDATE users SET username = '" + customerUpdate.getUsername() + "', password='" + customerUpdate.getPassword() + "' WHERE id =" + customerUpdate.getId();
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            Customer oldCustomerData = getCustomer(String.valueOf(customerUpdate.getId()));

            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to update an unknown customer with ID: " + customerUpdate.getId() + " username: " + customerUpdate.getUsername());
            } else {
                auditLogger.auditChange(new Entity("customer.update", String.valueOf(oldCustomerData.getId()), oldCustomerData.toString(), customerUpdate.toString()));
            }
        } catch (SQLException e) {
            LOG.error("Unable to update customer with ID: " + customerUpdate.getId(), e);
        }
    }

    public List<Address> getAddresses(String id) {
        String sqlQuery = "SELECT id, name FROM address WHERE userId=" + id;
        List<Address> addresses = new ArrayList<Address>();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sqlQuery)) {
            while (rs.next()) {
                addresses.add(createAddress(rs));
            }
        } catch (SQLException e) {
            LOG.error("Unable to fetch addresses for user with ID: " + id);
        }

        return addresses;
    }

    private Address createAddress(ResultSet rs) throws SQLException {
        Address address = null;
        try {
            int id = rs.getInt(1);
            String name = rs.getString(2);

            address = new Address(id, name);
            auditLogger.audit("Created a new address: " + address.toString());
        } catch (SQLException e) {
            LOG.error("Unable to create address for user", e);
        }

        return address;
    }

    public void deleteCustomerAddress(int id) {
        String query = "DELETE FROM address WHERE id=" + id;
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to delete an unknown address with ID: " + id);
            } else {
                auditLogger.audit("Deleted address with ID: " + id);
            }
        } catch (SQLException e) {
            LOG.error("Unable to delete address with ID: " + id, e);
        }
    }

    public void updateCustomerAddress(Address address) {
        String query = "UPDATE address SET name = '" + address.getName() + "' WHERE id =" + address.getId();
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to update an unknown address with ID: " + address.getId());
            } else {
                auditLogger.audit("Updated address with ID: " + address.getId() + ", " + address.getName());
            }
        } catch (SQLException e) {
            LOG.error("Unable to update customer address with ID: " + address.getId(), e);
        }
    }

    public void putCustomerAddress(NewAddress newAddress) {
        String query = "INSERT INTO address (name, userId) VALUES ('" + newAddress.getName() + "' , " + newAddress.getUserId() + ")";
        int updated = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            updated = statement.executeUpdate(query);

            if (updated < 1) {
                LOG.warn("Attempted to add a new address: " + newAddress.getName() + " userID: " + newAddress.getUserId());
            } else {
                auditLogger.audit("Added new address: " + newAddress.getName());
            }
        } catch (SQLException e) {
            LOG.error("Unable to add new address: " + newAddress.getName() + " for userID: " + newAddress.getUserId(), e);
        }
    }
}
