package hathoute.com.wallpapers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

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
    private CategoriesAdapter mAdapter;
    private JSONArray JSONresult;
    private String parentCategory;
    private boolean isLoading = false;
    private boolean rowsEnd = false;
    private ConfigureData configureData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        AdView adBanner = findViewById(R.id.adBanner);
        adBanner.loadAd(new AdRequest.Builder().build());

        setupFAB();

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
        mAdapter = new CategoriesAdapter(this, categoryList);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        rvCategories.setLayoutManager(mLayoutManager);
        rvCategories.setItemAnimator(new DefaultItemAnimator());
        rvCategories.setAdapter(mAdapter);

        new JsonTask().execute(AppHelper.wallpapersLink + "getcategoriesJSON.php");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(configureData != null)
            configureData.cancel(true);
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CategoriesActivity.this, OfflineActivity.class));
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

        configureData = new ConfigureData();
        configureData.execute();
        addRecyclerViewListener();
    }

    private void addRecyclerViewListener() {
        rvCategories.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && !isLoading && !rowsEnd) {
                    configureData = new ConfigureData();
                    configureData.execute();
                }
            }
        });
    }

    int curRow = 0;

    private class ConfigureData extends AsyncTask<Void, Category, Boolean> {
        private CategoriesAdapter categoriesAdapter;
        int lastRow = curRow;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoading = true;
            categoriesAdapter = (CategoriesAdapter) rvCategories.getAdapter();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            while(curRow < lastRow+10) {
                try {
                    String[] row = categoryLinks.get(curRow);
                    Category category = new Category(row[0], row[1], row[3], row[2].equals("main"));
                    Bitmap thumbnail = getBitmapFromURL(category.thumbLink);
                    if(thumbnail == null) {
                        System.out.println("IS NULL");
                    }
                    category.setThumbnail(thumbnail);
                    publishProgress(category);
                } catch (IndexOutOfBoundsException ignored) {
                    return true;
                } catch (Exception ignored) {
                    categoryLinks.remove(curRow);
                    lastRow--;
                    continue;
                }
                curRow++;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Category... categories) {
            categoryList.add(categories[0]);
            categoriesAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            rowsEnd = result;
            isLoading = false;
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

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(CategoriesActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
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
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            if(result == null) {
                new DialogHelper(CategoriesActivity.this,
                        R.string.offline_notice, R.string.dialog_yes, R.string.dialog_refresh,
                        new DialogHelper.OnChoiceListener() {
                            @Override
                            public void onChoice(boolean accepted) {
                                if(accepted) {
                                    startActivity(new Intent(
                                            CategoriesActivity.this, OfflineActivity.class));
                                    CategoriesActivity.this.finish();
                                } else
                                    new JsonTask().execute(AppHelper.wallpapersLink + "getcategoriesJSON.php");

                            }
                        }).show();
                return;
            }

            try {
                JSONObject object = new JSONObject(result);
                JSONresult = object.getJSONArray("result");
                populateCategoryList();
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showInterstitial() {
        final InterstitialAd mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-9871252548902893/7480166558");
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
