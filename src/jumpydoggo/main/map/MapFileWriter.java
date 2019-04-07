package jumpydoggo.main.map;

import jumpydoggo.main.Main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MapFileWriter {
    public static void saveAreaDataArray()  {
        BufferedWriter writer = null;
        try {

            writer = new BufferedWriter(new FileWriter("mapdata/" + Main.bw.getBWMap().mapFileName() + ".dat"));
            for (int x = 0; x< Main.areaDataArray.length; x++ ) {
                for (int y = 0; y < Main.areaDataArray[x].length; y++) {
                    String tx = String.format("%" + 3 + "s", String.valueOf(Main.areaDataArray[x][y]+"|") );
                    //String tx = String.format("%" + 2 +Main.areaDataArray[x][y] + "|");
                    writer.append(tx);

                }
                writer.append("\n");
            }
        writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
