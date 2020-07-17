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
 * Reads in text files containing all AT, DE and CH towns and cities and stores those places to the database.
 * <p>
 * The JSON file is located in the resources folder, but not pushed to Git as it is too large. It was downloaded from
 * http://download.geonames.org/export/dump/
 */
public class V7__Insert_Dach_Places extends BaseJavaMigration {

    PreparedStatement preparedStatement;

    // WARNING indices are hardcoded - be aware if the file changes!
    int cityIndex = 1;
    int latitudeIndex = 4;
    int longitudeIndex = 5;

    public void migrate(Context context) throws Exception {
        // Prepare SQL connection and statement
        String insertStatement = "INSERT INTO dach_places (name, latitude, longitude) VALUES  (?,?,?)";
        preparedStatement = context.getConnection().prepareStatement(insertStatement);

        context.getConnection().setAutoCommit(false);

        // Read the file line by line
        readAndInsertLinesFromFile(Paths.get(getClass().getClassLoader().getResource("AT.txt").toURI()), context);
        readAndInsertLinesFromFile(Paths.get(getClass().getClassLoader().getResource("DE.txt").toURI()), context);
        readAndInsertLinesFromFile(Paths.get(getClass().getClassLoader().getResource("CH.txt").toURI()), context);
        
        preparedStatement.close();
    }


    private void readAndInsertLinesFromFile(Path filePath, Context context) throws Exception {
        AtomicInteger currentLine = new AtomicInteger();

        Files.lines(filePath).forEach(line -> {
            try {
                String[] cityData = line.split("\t");
                preparedStatement.setString(1, cityData[cityIndex]);
                preparedStatement.setDouble(2, Double.parseDouble(cityData[latitudeIndex]));
                preparedStatement.setDouble(3, Double.parseDouble(cityData[longitudeIndex]));

                preparedStatement.addBatch();
                int lineCount = currentLine.getAndIncrement();
                if (lineCount % 1000 == 0) {
                    System.out.println("Line " + lineCount);
                    preparedStatement.executeBatch();
                    context.getConnection().commit();

                }
            } catch (SQLException e) {
                System.out.println("Failed to insert $line to database because of $e");
            }
        });

        preparedStatement.executeBatch();
        context.getConnection().commit();
    }
}
