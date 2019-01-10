package hathoute.com.wallpapers;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class AddReport extends AsyncTask<Void, Void, Void> {
    private final String reportString;
    private final String userMail;
    private final String reportUrl = AppHelper.wallpapersLink + "insertUserReport.php?";

    AddReport(String userMail, String reportString) {
        this.reportString = reportString.replace("&", ",");
        this.userMail = userMail.replace("&", ",");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        HttpURLConnection connection = null;

        try {
            String fullReportUrl = reportUrl + "mail=" + userMail + "&report=" + reportString;
            URL url = new URL(fullReportUrl);
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