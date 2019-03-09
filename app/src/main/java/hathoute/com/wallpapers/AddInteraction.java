package hathoute.com.wallpapers;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class AddInteraction extends AsyncTask<Void, Void, Void> {
    private final String interaction;
    private final String interactionUrl = AppHelper.wallpapersLink +
            "insertUserInteraction.php?type=";

    AddInteraction(String interaction) {
        this.interaction = interaction;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(interactionUrl + interaction);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            // This line is added so that the connection is achieved (I guess),
            // I removed it first but no record could've been saved.
            connection.getInputStream();
        } catch (IOException ignored) {
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return null;
    }
}
