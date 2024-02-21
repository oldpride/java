package tian.example;

import com.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
public class App 
{    
    public static void main(String[] args) throws FileNotFoundException, Exception {
        CSVReader reader = new CSVReader(new FileReader(args[0]));
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            // nextLine[] is an array of values from the line
            System.out.println(nextLine[0] + nextLine[1] + "etc...");
        }
    }
}
