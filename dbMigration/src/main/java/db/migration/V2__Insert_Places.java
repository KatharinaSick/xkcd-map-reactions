package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads in a JSON file containing all US towns and cities and stores those places to the database.
 * <p>
 * The JSON file is located in the resources folder, but not pushed to Git as it is too large. It was downloaded from
 * http://download.geonames.org/export/dump/
 */
public class V2__Insert_Places extends BaseJavaMigration {

    public void migrate(Context context) throws Exception {

        // Prepare SQL connection and statement
        String insertStatement = "INSERT INTO places (name, latitude, longitude) VALUES  (?,?,?)";
        PreparedStatement preparedStatement = context.getConnection().prepareStatement(insertStatement);

        context.getConnection().setAutoCommit(false);

        // Iterate over the file line by line to fetch the cities
        // WARNING indices are hardcoded - be aware if the file changes!
        int cityIndex = 1;
        int latitudeIndex = 4;
        int longitudeIndex = 5;

        // Read the file line by line
        Path filePath = Paths.get(getClass().getClassLoader().getResource("US.txt").toURI());

        AtomicInteger currentLine = new AtomicInteger();

        Files.lines(filePath).forEach(line -> {
            // Get the place and insert it into the database, fields are separated by a tab
            String[] cityData = line.split("\t");
            try {
                preparedStatement.setString(1, cityData[cityIndex]);
                preparedStatement.setDouble(2, Double.parseDouble(cityData[latitudeIndex]));
                preparedStatement.setDouble(3, Double.parseDouble(cityData[longitudeIndex]));

                preparedStatement.addBatch();

                System.out.println("Line " + currentLine);
                currentLine.getAndIncrement();
            } catch (SQLException e) {
                System.out.println("Failed to insert $line to database because of $e");
            }
        });

        preparedStatement.executeBatch();
        context.getConnection().commit();

        preparedStatement.close();
    }

}
