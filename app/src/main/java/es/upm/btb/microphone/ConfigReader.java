package es.upm.btb.microphone;

import android.content.Context;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private Context context;

    public ConfigReader(Context context) {
        this.context = context;
    }

    public String getProperty(String key) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
