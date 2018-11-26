package hathoute.com.wallpapers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.List;

public class WallpapersAdapter extends RecyclerView.Adapter<WallpapersAdapter.MyViewHolder> {

    private List<Wallpaper> wallpaperList;
    private final Activity mActivity;
    private InterstitialAd mInterstitialAd;
    private int showAd = 0;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public SquareImageView sivWallpaper;
        public ImageButton ibDownload, ibView, ibSet, ibDelete;
        public LinearLayout llContainer;

        public MyViewHolder(View view) {
            super(view);
            sivWallpaper = view.findViewById(R.id.sivWallpaper);
            ibDownload = view.findViewById(R.id.ibDownload);
            ibSet = view.findViewById(R.id.ibSet);
            ibView = view.findViewById(R.id.ibView);
            ibDelete = view.findViewById(R.id.ibDelete);
            llContainer = view.findViewById(R.id.llContainer);
        }
    }

    public WallpapersAdapter(Activity activity, List<Wallpaper> wallpaperList) {
        mActivity = activity;
        this.wallpaperList = wallpaperList;
    }

    public WallpapersAdapter(Activity activity, List<Wallpaper> wallpaperList,
                             InterstitialAd mInterstitialAd) {
        mActivity = activity;
        this.wallpaperList = wallpaperList;
        this.mInterstitialAd = mInterstitialAd;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_wallpaper, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final Wallpaper wallpaper = wallpaperList.get(position);
        holder.sivWallpaper.setImageBmp(wallpaper.thumbnail);
        Palette.from(wallpaper.thumbnail).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                int dominantColor = 0;
                if(palette != null)
                    dominantColor = palette.getDominantColor(0);

                // Makes colour darker
                float[] hsv = new float[3];
                Color.colorToHSV(dominantColor, hsv);
                hsv[2] *= 0.8f;

                // Setting the bmp as bg with an alpha (0xFF - 0xAA)
                holder.llContainer.setBackgroundColor(Color.HSVToColor(hsv) - 0x77000000);
            }
        });

        if(!wallpaper.isDownloaded(mActivity)) {
            holder.ibDelete.setVisibility(View.GONE);
            holder.ibView.setVisibility(View.GONE);
            holder.ibSet.setVisibility(View.GONE);
            holder.ibDownload.setVisibility(View.VISIBLE);
        } else {
            holder.ibDownload.setVisibility(View.GONE);
            holder.ibDelete.setVisibility(View.VISIBLE);
            holder.ibView.setVisibility(View.VISIBLE);
            holder.ibSet.setVisibility(View.VISIBLE);
        }

        holder.ibDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DownloadsManager(mActivity, wallpaper, new DownloadsManager.OnDownloadCallback() {
                    @Override
                    public void onDownloadSuccessful() {
                        holder.ibDelete.setVisibility(View.VISIBLE);
                        holder.ibView.setVisibility(View.VISIBLE);
                        holder.ibSet.setVisibility(View.VISIBLE);
                        holder.ibDownload.setVisibility(View.GONE);
                        showInterstitial();
                    }

                    @Override
                    public void onDownloadFailed() {

                    }
                }).execute();
            }
        });

        holder.ibView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppHelper.showImage(mActivity, wallpaper);
                System.out.print(Uri.parse(wallpaper.getFile(mActivity).toString()));
            }
        });

        holder.ibSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppHelper.performCrop(mActivity, Uri.parse(wallpaper.getFile(mActivity).toString()));
                showInterstitial();
            }
        });

        holder.ibDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean bResult = wallpaper.getFile(mActivity).delete();
                String result = bResult ?
                        "Wallpaper deleted successfully" : "Something went wrong!";
                Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();
                if(bResult) {
                    holder.ibDelete.setVisibility(View.GONE);
                    holder.ibView.setVisibility(View.GONE);
                    holder.ibSet.setVisibility(View.GONE);
                    holder.ibDownload.setVisibility(View.VISIBLE);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return wallpaperList.size();
    }

    private void showInterstitial() {
        if(mInterstitialAd == null || showAd < 2) {
            showAd++;
            return;
        }

        if(mInterstitialAd.isLoaded())
            mInterstitialAd.show();
        else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }
            });
        }
        showAd = 0;
    }
}
