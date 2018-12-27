package hathoute.com.wallpapers;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class AddFeedback extends AsyncTask<Void, Void, Void> {
    private final String feedback;
    private final String feedbackUrl = AppHelper.wallpapersLink + "insertUserFeedback.php?feedback=";

    AddFeedback(String feedback) {
        this.feedback = feedback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        HttpURLConnection connection = null;

        try {
            Log.i(AppHelper.APP_TAG, feedbackUrl + feedback);
            URL url = new URL(feedbackUrl + feedback);
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
