package hathoute.com.wallpapers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import eu.janmuller.android.simplecropimage.CropImage;

public class OfflineActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Wallpaper> wallpaperList;
    private ConfigureData configureData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle("Downloaded Wallpapers");
        }

        recyclerView = findViewById(R.id.rvWallpapers);

        wallpaperList = new ArrayList<>();

        WallpapersAdapter mAdapter = new WallpapersAdapter(this, wallpaperList);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        configureData = new ConfigureData(this);
        configureData.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(configureData != null)
            configureData.cancel(true);
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
            AppHelper.setWallpaper(this, bitmap);
        }
    }

    private static class ConfigureData extends AsyncTask<Void, Wallpaper, Integer> {
        private WallpapersAdapter wallpapersAdapter;
        private List<Wallpaper> wpList;
        private WeakReference<OfflineActivity> activityReference;

        ConfigureData(OfflineActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            OfflineActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            wpList = getDownloadedWallpapers(activity);
            wallpapersAdapter = (WallpapersAdapter) activity.recyclerView.getAdapter();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            OfflineActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                this.cancel(true);
                return null;
            }

            for(Wallpaper wallpaper : wpList) {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    Bitmap thumbnail = BitmapFactory.decodeFile(
                            activity.getFilesDir().toString() + "/" + wallpaper.name, options);
                    wallpaper.setThumbnail(thumbnail);
                    publishProgress(wallpaper);
                } catch (Exception ignored) {
                    wpList.remove(wallpaper);
                }
            }
            return wpList.size();
        }

        @Override
        protected void onProgressUpdate(Wallpaper... wallpaper) {
            OfflineActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }

            activity.wallpaperList.add(wallpaper[0]);
            wallpapersAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer result) {
            OfflineActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                this.cancel(true);
                return;
            }
            Toast.makeText(activity,
                    "Successfully loaded " + result + " wallpapers.", Toast.LENGTH_SHORT).show();
        }

        private List<Wallpaper> getDownloadedWallpapers(Context context) {
            List<Wallpaper> list = new ArrayList<>();
            File drc = context.getFilesDir();
            for(File file : drc.listFiles()) {
                if(file.isDirectory() || !file.getName().contains(".jpg"))
                    continue;

                list.add(new Wallpaper(file.getName()));
            }

            return list;
        }
    }
}
