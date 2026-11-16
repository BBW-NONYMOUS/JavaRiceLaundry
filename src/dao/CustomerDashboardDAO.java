package dao;

import java.sql.SQLException;
import model.DashboardData;

public interface CustomerDashboardDAO {
    DashboardData load(int customerId) throws SQLException;
}
