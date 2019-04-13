package hathoute.com.wallpapers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.codemybrainsout.ratingdialog.RatingDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import eu.janmuller.android.simplecropimage.CropImage;

public class MainActivity extends AppCompatActivity {

    private List<Wallpaper> wallpaperList;
    private List<String> wallpaperLinks;
    private String category;
    private WallpapersAdapter mAdapter;
    private RecyclerView recyclerView;
    private ProgressDialog pd;
    private JSONArray JSONresult;
    private boolean isLoading = false;
    private boolean rowsEnd = false;
    private InterstitialAd mInterstitialAd;
    private AdRequest mAdRequest;
    private ConfigureData configureData;
    private JsonTask jsonTask;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdRequest = new AdRequest.Builder().build();
        AdView adBanner = findViewById(R.id.adBanner);
        adBanner.loadAd(mAdRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(BuildConfig.AD_MAIN);
        mInterstitialAd.loadAd(mAdRequest);

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (AdsBackgroundService.canShowAds()) {
                    mInterstitialAd.show();
                    mInterstitialAd.loadAd(mAdRequest);
                }
            }
        });

        try {
            category = getIntent().getStringExtra("category");
        } catch (Exception ignored) {}

        recyclerView = findViewById(R.id.rvWallpapers);

        wallpaperList = new ArrayList<>();
        wallpaperLinks = new ArrayList<>();
        mAdapter = new WallpapersAdapter(this, wallpaperList, mInterstitialAd);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        preferences = getPreferences(MODE_PRIVATE);

        jsonTask = new JsonTask(this);
        jsonTask.execute(AppHelper.wallpapersLink + "getwallpapersJSON.php");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pd.isShowing())
            return;

        wallpaperList.clear();
        wallpaperLinks.clear();
        mAdapter.notifyDataSetChanged();
        curRow = 0;
        jsonTask = new JsonTask(this);
        jsonTask.execute(AppHelper.wallpapersLink + "getwallpapersJSON.php");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(configureData != null)
            configureData.cancel(true);
        if(jsonTask != null)
            jsonTask.cancel(true);
    }

    private void populateWallpaperList() {
        for(int i = 0; i < JSONresult.length(); i++) {
            try {
                JSONObject object = JSONresult.getJSONObject(i);
                String name = object.getString("id") + "."
                        + object.getString("type");
                String parent = object.getString("parent");

                if(category == null || parent.equals(category)) {
                    wallpaperLinks.add(name);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        configureData = new ConfigureData(MainActivity.this);
        configureData.execute();

        addRecyclerViewListener();
    }

    private void addRecyclerViewListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && !isLoading && !rowsEnd) {
                    configureData = new ConfigureData(MainActivity.this);
                    configureData.execute();
                }
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
                                    Toast.makeText(MainActivity.this, "Thank you for your time :)", Toast.LENGTH_SHORT).show();

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
                                    Toast.makeText(MainActivity.this, "Thank you for your time :)", Toast.LENGTH_SHORT).show();
                                    AppHelper.openRatePage(MainActivity.this);

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

    private int curRow = 0;

    private static class ConfigureData extends AsyncTask<Void, Wallpaper, Boolean> {
        private WallpapersAdapter wallpapersAdapter;
        private final WeakReference<MainActivity> activityWeakReference;
        int lastRow;

        ConfigureData(MainActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
            lastRow = activity.curRow;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            activity.isLoading = true;
            wallpapersAdapter = (WallpapersAdapter) activity.recyclerView.getAdapter();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            MainActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return false;
            }
            while(activity.curRow < lastRow+10) {
                try {
                    Wallpaper wallpaper = new Wallpaper(activity.wallpaperLinks.get(activity.curRow));
                    Bitmap thumbnail = getBitmapFromURL(wallpaper.thumbnailLink);
                    if(thumbnail == null) {
                        System.out.println("IS NULL");
                    }
                    wallpaper.setThumbnail(thumbnail);
                    publishProgress(wallpaper);
                } catch (IndexOutOfBoundsException ignored) {
                    return true;
                } catch (Exception ignored) {
                    activity.wallpaperLinks.remove(activity.curRow);
                    lastRow--;
                    continue;
                }
                activity.curRow++;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Wallpaper... wallpaper) {
            MainActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            activity.wallpaperList.add(wallpaper[0]);
            wallpapersAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            MainActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            activity.rowsEnd = result;
            activity.isLoading = false;
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

    private static class JsonTask extends AsyncTask<String, String, String> {
        final WeakReference<MainActivity> weakReference;

        JsonTask(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        protected void onPreExecute() {
            MainActivity activity = weakReference.get();

            activity.pd = new ProgressDialog(activity);
            activity.pd.setMessage("Please wait");
            activity.pd.setCancelable(false);
            activity.pd.show();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
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
            } catch(NullPointerException e) {
                new AddFeedback("null_Main_2").execute();
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
            MainActivity activity = weakReference.get();
            if(activity == null || activity.isFinishing())
                return;

            if (activity.pd != null && activity.pd.isShowing()){
                activity.pd.dismiss();
            }

            try {
                JSONObject object = new JSONObject(result);
                activity.JSONresult = object.getJSONArray("result");
                activity.populateWallpaperList();
            } catch(JSONException e) {
                e.printStackTrace();
            } catch(NullPointerException e) {
                new AddFeedback("null_Main_1").execute();
            }
        }
    }
}


