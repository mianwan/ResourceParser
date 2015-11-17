package edu.usc.sql.transform;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mianwan on 11/9/15.
 */
public class RemoveItems {
    public static void main(String args[]) throws DocumentException, IOException {
        String stylePath = "/home/mianwan/Desktop/backup/abrc.mf.td/res/values/sdkstyles.xml";
        String errorLog = "/home/mianwan/Desktop/backup/error.txt";
        Set<String> itemSet = new HashSet<String>();
        try {
            FileReader fr = new FileReader(errorLog);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("/home")) {
                    System.out.println(line);
                    String item = line.split(" attr ")[1];
                    item = item.substring(1, item.length() - 2);
                    itemSet.add(item);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        SAXReader reader = new SAXReader();
        File styleFile = new File(stylePath);
        Document doc = reader.read(styleFile);
        Element root = doc.getRootElement();

        for (Element e : root.elements()) {

            for (Element it : e.elements()) {
                String name = it.attributeValue("name");
                if (itemSet.contains(name)) {
                    e.remove(it);
                }

                String alteredColorXml = "/home/mianwan/Desktop/backup/abrc.mf.td" + File.separator + "res" + File.separator + "values" + File.separator + "sdkstyles.xml";
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "UTF-8"), format);
                writer.write(doc);
                writer.close();
            }

        }

    }
}
