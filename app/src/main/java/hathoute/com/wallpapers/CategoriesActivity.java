package hathoute.com.wallpapers;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

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

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private ProgressDialog pd;
    private List<Category> categoryList;
    private List<String[]> categoryLinks;
    private JSONArray JSONresult;
    private String parentCategory;
    private boolean isLoading = false;
    private boolean rowsEnd = false;
    private ConfigureData configureData;
    private JsonTask jsonTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        // The purpose of toolbar is just to add Instagram
        // button so that users could follow me easily.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Setting toolbar as SupportActionBar
        setSupportActionBar(toolbar);

        // Execute asyncTask.
        new checkDevMessage(this).execute();

        AdView adBanner = findViewById(R.id.adBanner);
        adBanner.loadAd(new AdRequest.Builder().build());

        setupFAB();
        setupFABApps();

        String parentName = "Categories";
        try {
            parentCategory = getIntent().getStringExtra("category");
            parentName = getIntent().getStringExtra("categoryName");
        } catch (Exception ignored) {}

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null && (parentCategory == null || parentCategory.equals("main"))) {
            actionBar.setTitle("Categories");
        } else if(actionBar != null) {
            actionBar.setTitle(parentName);
        }

        showInterstitial();

        rvCategories = findViewById(R.id.rvCategories);
        categoryList = new ArrayList<>();
        categoryLinks = new ArrayList<>();
        CategoriesAdapter mAdapter = new CategoriesAdapter(this, categoryList);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        rvCategories.setLayoutManager(mLayoutManager);
        rvCategories.setItemAnimator(new DefaultItemAnimator());
        rvCategories.setAdapter(mAdapter);

        jsonTask = new JsonTask(this);
        jsonTask.execute(AppHelper.wallpapersLink + "getcategoriesJSON.php");
    }

    // Toolbar buttons Overrides starts here ---------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items
        // to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Used if statement in case I wanted
        // to add more buttons. Who knows ;).
        if (id == R.id.bInstagram) {
            Uri uri = Uri.parse(AppHelper.instagramLink);
            Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

            likeIng.setPackage("com.instagram.android");

            try {
                startActivity(likeIng);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(AppHelper.instagramLink)));
            }

            final SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
            // Check if the user already pressed the button,
            // so we don't count him again.
            if(sharedPref.getInt("instaInteracted", 0) == 0) {
                new AddInteraction("instagram").execute();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("instaInteracted", 1);
                editor.apply();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // Toolbar buttons Overrides ends here ------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(configureData != null)
            configureData.cancel(true);
        if(jsonTask != null)
            jsonTask.cancel(true);
    }

    private void showDevDialog(final String message, final int msgV) {
        final SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);

        // Check if the message was already shown to the user.
        if(sharedPref.getInt("messageCode", 0) == msgV)
            return;

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_dev_message);
        Window window = dialog.getWindow();
        if(window != null)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

        // Replace message into TextView
        TextView tvDevMessage = dialog.findViewById(R.id.tvDevMessage);
        tvDevMessage.setText(message);

        dialog.findViewById(R.id.bDismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exit dialog
                dialog.dismiss();

                // Save current messageCode so that the user wont be shown
                // the same message again.
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("messageCode", msgV);
                editor.apply();
            }
        });

        dialog.show();
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        CategoriesActivity.this, OfflineActivity.class));
            }
        });
    }

    private void setupFABApps() {
        FloatingActionButton fab = findViewById(R.id.fabApps);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        CategoriesActivity.this, MyAppsActivity.class));
            }
        });
    }

    private void populateCategoryList() {
        for(int i = 0; i < JSONresult.length(); i++) {
            try {
                JSONObject object = JSONresult.getJSONObject(i);
                String parent = object.getString("parent");
                if(parentCategory == null && !(parent.equals("main") || parent.isEmpty()))
                    continue;
                else if(parentCategory != null && !parentCategory.equals(parent))
                    continue;

                String name = object.getString("abv");
                String fullName = object.getString("name_EN");
                String thumbnailId = object.getString("thumbnail");
                categoryLinks.add(new String[]{name, fullName, parent, thumbnailId});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        configureData = new ConfigureData(this);
        configureData.execute();
        addRecyclerViewListener();
    }

    private void addRecyclerViewListener() {
        rvCategories.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && !isLoading && !rowsEnd) {
                    configureData = new ConfigureData(CategoriesActivity.this);
                    configureData.execute();
                }
            }
        });
    }

    int curRow = 0;

    private static class ConfigureData extends AsyncTask<Void, Category, Boolean> {
        private final CategoriesAdapter categoriesAdapter;
        private final WeakReference<CategoriesActivity> activityWeakReference;
        int lastRow;

        private ConfigureData(CategoriesActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
            lastRow = activity.curRow;
            categoriesAdapter = (CategoriesAdapter) activity.rvCategories.getAdapter();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            CategoriesActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            activity.isLoading = true;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            CategoriesActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return false;
            }

            while(activity.curRow < lastRow+10) {
                try {
                    String[] row = activity.categoryLinks.get(activity.curRow);
                    Category category = new Category(row[0], row[1], row[3], row[2].equals("main"));
                    Bitmap thumbnail = getBitmapFromURL(category.thumbLink);
                    if(thumbnail == null)
                        continue;

                    category.setThumbnail(thumbnail);
                    publishProgress(category);
                } catch (IndexOutOfBoundsException ignored) {
                    return true;
                } catch (Exception ignored) {
                    activity.categoryLinks.remove(activity.curRow);
                    lastRow--;
                    continue;
                }
                activity.curRow++;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Category... categories) {
            CategoriesActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            activity.categoryList.add(categories[0]);
            categoriesAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            CategoriesActivity activity = activityWeakReference.get();
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
        final WeakReference<CategoriesActivity> weakReference;

        JsonTask(CategoriesActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        protected void onPreExecute() {
            super.onPreExecute();

            CategoriesActivity activity = weakReference.get();
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
            final CategoriesActivity activity = weakReference.get();
            if(activity == null || activity.isFinishing())
                return;

            if (activity.pd != null && activity.pd.isShowing()){
                activity.pd.dismiss();
            }

            if(result == null) {
                new DialogHelper(activity,
                        R.string.offline_notice, R.string.dialog_yes, R.string.dialog_refresh,
                        new DialogHelper.OnChoiceListener() {
                            @Override
                            public void onChoice(boolean accepted) {
                                if(accepted) {
                                    activity.startActivity(new Intent(
                                            activity, OfflineActivity.class));
                                    activity.finish();
                                } else {
                                    activity.jsonTask = new JsonTask(activity);
                                    activity.jsonTask.execute(AppHelper.wallpapersLink
                                            + "getcategoriesJSON.php");
                                }
                            }
                        }).show();
                return;
            }

            try {
                JSONObject object = new JSONObject(result);
                activity.JSONresult = object.getJSONArray("result");
                activity.populateCategoryList();
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static class checkDevMessage extends AsyncTask<Void, Void, String> {
        private final String htmlMessageFile = AppHelper.wallpapersLink + "developer_message.html";
        private final WeakReference<CategoriesActivity> activityWeakReference;

        private checkDevMessage(CategoriesActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        protected String doInBackground(Void... voids) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(htmlMessageFile);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                // Result must contain one line, this is not
                // an obligation but rather a personal choice.
                return reader.readLine();


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

            // Get activity from WeakReference object and check if
            // the activity is still valid.
            CategoriesActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()) {
                return;
            }

            // It is unnecessary to continue without a message to show.
            if(result == null) {
                return;
            }

            // Get version and message from result.
            String[] tokens = result.split("//");
            try {
                int version = Integer.valueOf(tokens[0]);
                String message = tokens[1].replace("\\n", "\n");
                // Show developer message Dialog
                activity.showDevDialog(message, version);
            } catch (Exception ignored) {}
        }
    }

    private void showInterstitial() {
        final InterstitialAd mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(BuildConfig.AD_MAIN);
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (AdsBackgroundService.canShowAds())
                    mInterstitialAd.show();
            }
        });
    }
}
