package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class CsvReader {

    public static final WKTReader WKT_READER = new WKTReader();

    public static Geometry[] readAllEntities(String delimiter, String inputFilePath) throws IOException {
        final List<Geometry> loadedEntities = new ArrayList<>();

        final BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        String line = reader.readLine();
        while (line != null) {
            String[] tokens = line.split(delimiter);
            Geometry geometry = null;
            try {
                if (2 <= tokens.length) {
                    geometry = WKT_READER.read(tokens[1].trim());
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
            }

            if (geometry != null) {
                loadedEntities.add(geometry);
            }

            line = reader.readLine();
        }
        reader.close();
        System.out.println("Total source entities\t:\t" + loadedEntities.size());

        return loadedEntities.toArray(new Geometry[loadedEntities.size()]);
    }

}
