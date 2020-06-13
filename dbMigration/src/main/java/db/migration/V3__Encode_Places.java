package db.migration;

import org.apache.commons.codec.language.Nysiis;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.bm.BeiderMorseEncoder;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encodes all stored places with the Beider-Morse and the Nysiis Phonetic Matching Algorithm. Then it stores
 * all codes and their matching place ids into new databases.
 */
public class V3__Encode_Places extends BaseJavaMigration {

    public void migrate(Context context) throws Exception {

        Connection connection = context.getConnection();

        // Create the statement to execute SQL code
        Statement statement = connection.createStatement();

        // Create a table for the Beider-Morse codes
        statement.execute("CREATE SEQUENCE seq_beider_morse_encoded_places");
        statement.execute("CREATE TABLE beider_morse_encoded_places (" +
                "id BIGINT default nextval('seq_beider_morse_encoded_places') PRIMARY KEY, " +
                "code VARCHAR, " +
                "place_id BIGINT)");

        // Create a table for the Nysiis codes
        statement.execute("CREATE SEQUENCE seq_nysiis_encoded_places");
        statement.execute("CREATE TABLE nysiis_encoded_places (" +
                "id BIGINT default nextval('seq_nysiis_encoded_places') PRIMARY KEY, " +
                "code VARCHAR, " +
                "place_id BIGINT)");

        // Create a table for the Soundex codes
        statement.execute("CREATE SEQUENCE seq_soundex_encoded_places");
        statement.execute("CREATE TABLE soundex_encoded_places (" +
                "id BIGINT default nextval('seq_soundex_encoded_places') PRIMARY KEY, " +
                "code VARCHAR, " +
                "place_id BIGINT)");

        // Prepare SQL connections and statements for both tables
        String beiderMorseInsertStatement = "INSERT INTO beider_morse_encoded_places (code, place_id) VALUES  (?,?)";
        PreparedStatement beiderMorsePreparedStatement = context.getConnection().prepareStatement(beiderMorseInsertStatement);

        String nysiisInsertStatement = "INSERT INTO nysiis_encoded_places (code, place_id) VALUES  (?,?)";
        PreparedStatement nysiisPreparedStatement = context.getConnection().prepareStatement(nysiisInsertStatement);

        String soundexInsertStatement = "INSERT INTO soundex_encoded_places (code, place_id) VALUES  (?,?)";
        PreparedStatement soundexPreparedStatement = context.getConnection().prepareStatement(soundexInsertStatement);

        // Disable auto commit because otherwise preparedStatement.addBatch(..) won't work as expected
        connection.setAutoCommit(false);

        // Iterate over all places, encode them and store the codes into the newly created tables
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, name FROM places")) {

                AtomicInteger currentLine = new AtomicInteger(0);

                while (rows.next()) {
                    long id = rows.getLong(1);
                    String name = rows.getString(2);

                    String[] beiderMorseCodes = new BeiderMorseEncoder().encode(name).split("|");
                    for (String code : beiderMorseCodes) {
                        beiderMorsePreparedStatement.setString(1, code);
                        beiderMorsePreparedStatement.setLong(2, id);
                        beiderMorsePreparedStatement.addBatch();
                    }

                    String nysiisCode = new Nysiis().encode(name);
                    nysiisPreparedStatement.setString(1, nysiisCode);
                    nysiisPreparedStatement.setLong(2, id);
                    nysiisPreparedStatement.addBatch();

                    try {
                        String soundexCode = new Soundex().encode(name);
                        soundexPreparedStatement.setString(1, soundexCode);
                        soundexPreparedStatement.setLong(2, id);
                        soundexPreparedStatement.addBatch();
                    } catch (IllegalArgumentException e) {
                        // Soundex can't map this name, but beider morse and nysiis are able to handle it -> just move on
                        System.out.println("No Soundex mapping for " + name);
                    }

                    if (currentLine.get() % 2500 == 0) {
                        beiderMorsePreparedStatement.executeBatch();
                        nysiisPreparedStatement.executeBatch();
                        soundexPreparedStatement.executeBatch();
                        System.out.println("[" + System.currentTimeMillis() + "] -- Done with line " + currentLine);
                    }

                    currentLine.getAndIncrement();

                }
            }
        }

        beiderMorsePreparedStatement.executeBatch();
        nysiisPreparedStatement.executeBatch();
        soundexPreparedStatement.executeBatch();

        connection.commit();

        beiderMorsePreparedStatement.close();
        nysiisPreparedStatement.close();
        soundexPreparedStatement.close();
    }
}
