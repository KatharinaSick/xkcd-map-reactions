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

                AtomicInteger currentLine = new AtomicInteger(1);
                BeiderMorseEncoder beiderMorseEncoder = new BeiderMorseEncoder();
                Nysiis nysiisEncoder = new Nysiis();
                Soundex soundexEncoder = new Soundex();

                long timerHelper;
                long totalTime = System.currentTimeMillis();
                long beiderMorseTime = 0;
                long nysiisTime = 0;
                long soundexTime = 0;
                while (rows.next()) {
                    long id = rows.getLong(1);
                    String name = rows.getString(2);

                    timerHelper = System.currentTimeMillis();
                    String[] beiderMorseCodes = beiderMorseEncoder.encode(name).split("\\|");
                    for (String code : beiderMorseCodes) {
                        beiderMorsePreparedStatement.setString(1, code);
                        beiderMorsePreparedStatement.setLong(2, id);
                        beiderMorsePreparedStatement.addBatch();
                    }
                    beiderMorseTime += System.currentTimeMillis() - timerHelper;

                    timerHelper = System.currentTimeMillis();
                    String nysiisCode = nysiisEncoder.encode(name);
                    nysiisPreparedStatement.setString(1, nysiisCode);
                    nysiisPreparedStatement.setLong(2, id);
                    nysiisPreparedStatement.addBatch();
                    nysiisTime += System.currentTimeMillis() - timerHelper;

                    timerHelper = System.currentTimeMillis();
                    try {
                        String soundexCode = soundexEncoder.encode(name);
                        soundexPreparedStatement.setString(1, soundexCode);
                        soundexPreparedStatement.setLong(2, id);
                        soundexPreparedStatement.addBatch();
                    } catch (IllegalArgumentException e) {
                        // Soundex can't map this name, but beider morse and nysiis are able to handle it -> just move on
                        System.out.println("No Soundex mapping for " + name);
                    }
                    soundexTime += System.currentTimeMillis() - timerHelper;

                    if (currentLine.get() % 1000 == 0) {
                        long commitTime = System.currentTimeMillis();
                        beiderMorsePreparedStatement.executeBatch();
                        nysiisPreparedStatement.executeBatch();
                        soundexPreparedStatement.executeBatch();
                        connection.commit();
                        commitTime = System.currentTimeMillis() - commitTime;
                        System.out.println("Done with line: " + currentLine.get() + "\n" +
                                "Total Time: " + (System.currentTimeMillis() - totalTime) + "ms\n" +
                                "BeiderMorse Time: " + beiderMorseTime + "ms\n" +
                                "Nyiis Time: " + nysiisTime + "ms\n" +
                                "Soundex Time: " + soundexTime + "ms\n" +
                                "Commit Time: " + commitTime + "ms\n");
                        totalTime = System.currentTimeMillis();
                        beiderMorseTime = 0;
                        nysiisTime = 0;
                        soundexTime = 0;
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
