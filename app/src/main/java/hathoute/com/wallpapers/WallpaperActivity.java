package hathoute.com.wallpapers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.codemybrainsout.ratingdialog.RatingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import eu.janmuller.android.simplecropimage.CropImage;

public class WallpaperActivity extends AppCompatActivity {

    private String wallpaperName;
    private ProgressDialog pd;
    private TextView tvDownloads, tvAuthor, tvCategory;
    private LinearLayout llDelete, llReport, llDownload, llView, llSet;
    private SquareImageView sivWallpaper;
    private Wallpaper wallpaper;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);

        preferences = getPreferences(MODE_PRIVATE);

        try {
            wallpaperName = getIntent().getStringExtra("wallpaper_name");
        } catch (Exception ignored) {}
        wallpaper = new Wallpaper(wallpaperName);

        tvDownloads = findViewById(R.id.tvWpDownloads);
        tvAuthor = findViewById(R.id.tvWpAuthor);
        tvCategory = findViewById(R.id.tvWpCategory);

        llDelete = findViewById(R.id.llDelete);
        llReport = findViewById(R.id.llReport);
        llDownload = findViewById(R.id.llDownload);
        llView = findViewById(R.id.llView);
        llSet = findViewById(R.id.llSet);

        sivWallpaper = findViewById(R.id.sivWallpaper);

        applyWallpaper();
        configureButtons();

        // wallpaperName contains the wallpaperId and the extension,
        // so we split the name and we take the wallpaperId.
        String wallpaperId = wallpaperName.split("\\.")[0];
        new ConfigureViews(wallpaperId).execute();
    }

    private void applyWallpaper() {
        Bitmap bmp;

        if(wallpaper.isDownloaded(this)) {
            // If wallpaper is already downloaded, show it to the user.
            File file = wallpaper.getFile(this);
            bmp = BitmapFactory.decodeFile(file.getPath());
            // Apply wallpaper to the Square Image View.
            sivWallpaper.setImageBmp(bmp);
        } else {
            // Wallpaper isn't downloaded, show thumbnail instead.
            // This asyncTask does all the work needed.
            new SetBitmapToWallpaper(sivWallpaper, wallpaper.thumbnailLink).execute();
        }
    }

    private void configureButtons() {
        // Only show the buttons that the user needs (hide Download
        // button when the user has already the wallpaper downloaded,
        // etc...).
        if(wallpaper.isDownloaded(this)) {
            // Wallpaper is downloaded, hide the download button.
            llDownload.setVisibility(View.GONE);
        } else {
            // Wallpaper isn't downloaded, hide Delete & other buttons.
            llDelete.setVisibility(View.GONE);
            llSet.setVisibility(View.GONE);
            llView.setVisibility(View.GONE);
        }

        // Configure triggers to buttons.
        llDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get file object and delete it.
                File file = wallpaper.getFile(WallpaperActivity.this);
                file.delete();
                // We need to show the Download button now,
                // and hide other buttons
                llDelete.setVisibility(View.GONE);
                llSet.setVisibility(View.GONE);
                llView.setVisibility(View.GONE);
                llDownload.setVisibility(View.VISIBLE);
            }
        });
        llReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String key = "reported_" + wallpaperName.split("\\.")[0];
                boolean prevReported = preferences.getBoolean(key, false);
                if(prevReported) {
                    Toast.makeText(WallpaperActivity.this,
                            R.string.error_already_reported, Toast.LENGTH_LONG).show();
                    return;
                }

                new ReportDialog(WallpaperActivity.this, new ReportDialog.OnSubmitListener() {
                    @Override
                    public void onSubmit() {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(key, true);
                        editor.apply();
                        Toast.makeText(WallpaperActivity.this,
                                R.string.report_successful, Toast.LENGTH_LONG).show();
                    }
                }).show();
            }
        });
        llView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Image in Gallery app.
                AppHelper.showImage(WallpaperActivity.this, wallpaper);
            }
        });
        llSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to the edit & set wallpaper activity.
                File file = wallpaper.getFile(WallpaperActivity.this);
                AppHelper.performCrop(WallpaperActivity.this, Uri.parse(file.toString()));
            }
        });
        llDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch download.
                new DownloadsManager(WallpaperActivity.this, wallpaper,
                        new DownloadsManager.OnDownloadCallback() {
                    @Override
                    public void onDownloadSuccessful() {
                        // Download is successful, remember to show & hide buttons.
                        llDelete.setVisibility(View.VISIBLE);
                        llView.setVisibility(View.VISIBLE);
                        llSet.setVisibility(View.VISIBLE);
                        llDownload.setVisibility(View.GONE);
                    }

                    @Override
                    public void onDownloadFailed() {
                        // Download failed, do nothing.
                    }
                }).execute();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK)
            return;

        if (requestCode == AppHelper.PIC_CROP) {
            String path = data.getStringExtra(CropImage.IMAGE_PATH);
            if(path == null)
                return;

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if(AppHelper.setWallpaper(this, bitmap)) {
                int count = preferences.getInt("rate_count", 0);
                int maxCount = preferences.getInt("rate_maxCount", 2);
                boolean submitted = preferences.getBoolean("rate_submitted", false);

                if(submitted)
                    return;

                //Build rating dialog
                if(count > maxCount) {
                    final RatingDialog ratingDialog = new RatingDialog.Builder(this)
                            .threshold(4)
                            .onRatingBarFormSumbit(new RatingDialog.Builder.RatingDialogFormListener() {
                                @Override
                                public void onFormSubmitted(String feedback) {
                                    new AddFeedback(feedback).execute();
                                    Toast.makeText(WallpaperActivity.this,
                                            "Thank you for your time :)",
                                            Toast.LENGTH_SHORT)
                                            .show();

                                    //User submitted feedback, let's remember him.
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putBoolean("rate_submitted", true);
                                    editor.apply();
                                }
                            })
                            .onThresholdCleared(new RatingDialog.Builder.RatingThresholdClearedListener() {
                                @Override
                                public void onThresholdCleared(RatingDialog ratingDialog, float rating, boolean thresholdCleared) {
                                    //Redirect user to Play Store.
                                    Toast.makeText(WallpaperActivity.this, "Thank you for your time :)", Toast.LENGTH_SHORT).show();
                                    AppHelper.openRatePage(WallpaperActivity.this);

                                    //User rated, let's remember him
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putBoolean("rate_submitted", true);
                                    editor.apply();
                                }
                            }).build();

                    //Show dialog
                    ratingDialog.show();

                    //MaxCount should now be high, let's use 10
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("rate_maxCount", 10);
                    editor.putInt("rate_count", 0);
                    editor.apply();

                } else {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("rate_count", ++count);
                    editor.apply();
                }
            }
        }
    }

    private class ConfigureViews extends AsyncTask<Void, String, String> {

        final String phpScriptLink;

        ConfigureViews(String wallpaperId) {
            phpScriptLink = AppHelper.wallpapersLink + "getWallpaperInfo.php?wp_id=" + wallpaperId;
        }

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(WallpaperActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(phpScriptLink);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                String line;

                while ((line = reader.readLine()) != null) {
                    if(!line.contains("result"))
                        continue;

                    return line;
                }

                return null;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            // Result is null, no need to continue;
            // Maybe the user is in offline mode,
            // or the server is temporary unavailable.
            if(result == null)
                return;

            try {
                Resources resources = getResources();
                JSONArray JSONResult = new JSONObject(result).getJSONArray("result");
                JSONObject object = JSONResult.getJSONObject(0);

                // Apply result to Views.
                String data = object.getString("author");
                String tvText = resources.getString(R.string.wp_author)
                        .replace("$", "<b>").replace("£", "</b>")
                        .replace("%author", data);
                tvAuthor.setText(Html.fromHtml(tvText));

                data = object.getString("dl_times");
                tvText = resources.getString(R.string.wp_downloads)
                        .replace("$", "<b>").replace("£", "</b>")
                        .replace("%downloads", data);
                tvDownloads.setText(Html.fromHtml(tvText));

                data = object.getString("category");
                tvText = resources.getString(R.string.wp_category)
                        .replace("$", "<b>").replace("£", "</b>")
                        .replace("%category", data);
                tvCategory.setText(Html.fromHtml(tvText));

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class SetBitmapToWallpaper extends AsyncTask<Void, Void, Bitmap> {
        final WeakReference<SquareImageView> sivWeakReference;
        final String wallpaperLink;

        SetBitmapToWallpaper(SquareImageView siv, String link) {
            sivWeakReference = new WeakReference<>(siv);
            wallpaperLink = link;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            // Return the bitmap from link.
            return getBitmapFromURL(wallpaperLink);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // Get the View object from Weak Reference.
            SquareImageView siv = sivWeakReference.get();
            if(siv != null && bitmap != null)
                siv.setImageBmp(bitmap);
        }

        private Bitmap getBitmapFromURL(String url) {
            Bitmap bitmap = null;

            try {
                URL imageURL = new URL(url);
                bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }
    }
}
