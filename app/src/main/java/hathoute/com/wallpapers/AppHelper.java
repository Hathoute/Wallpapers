package hathoute.com.wallpapers;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

import eu.janmuller.android.simplecropimage.CropImage;

public class AppHelper {

    public final static int PIC_CROP = 1;
    public final static String wallpapersLink = "http://80.211.97.124/wallpapers/CSGO/";

    public static boolean setWallpaper(Context context, Bitmap bitmap) {
        WallpaperManager myWallpaperManager
                = WallpaperManager.getInstance(context);
        try {
            myWallpaperManager.setBitmap(bitmap);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void performCrop(Activity activity, Uri picUri) {
        try {
            Intent cropIntent = new Intent(activity, CropImage.class);

            cropIntent.putExtra(CropImage.IMAGE_PATH, picUri.toString());
            // get user aspect ratio
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int height = metrics.heightPixels;
            int width = metrics.widthPixels;
            int[] ratio = ratio(height, width);

            // allow CropImage activity to rescale image
            cropIntent.putExtra(CropImage.SCALE, true);

            // indicate aspect of desired crop
            cropIntent.putExtra(CropImage.ASPECT_X, ratio[1]);
            cropIntent.putExtra(CropImage.ASPECT_Y, ratio[0]);

            //cropIntent.putExtra(CropImage.RETURN_DATA, true);

            // start the activity - we handle returning in onActivityResult
            activity.startActivityForResult(cropIntent, PIC_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException e) {
            // display an error message
            String errorMessage = "Whoops - something went wrong!";
            Toast toast = Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public static void showImage(Context context, Wallpaper wallpaper) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider",
                wallpaper.getFile(context));
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static String getAppropriateSize(int bytes) {
        float convertedBytes = bytes;
        String sizePrefix;
        if(bytes < 1024) {
            sizePrefix = "bytes";
        }
        else if(bytes < 1024*1024) {
            convertedBytes /= 1024;
            sizePrefix = "KB";
        }
        else {
            convertedBytes /= (1024*1024);
            sizePrefix = "MB";
        }

        return String.format(Locale.ENGLISH, "%.2f", convertedBytes) + " " + sizePrefix;
    }

    public static Bitmap cropBitmapFromCenter(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int croph, cropw;
        if(height >= width) {
            cropw = (height - width)/2;
            croph = 0;
            height = width;
        } else {
            cropw = 0;
            croph = (width - height)/2;
            width = height;
        }

        return Bitmap.createBitmap(bitmap, croph, cropw, width, height);
    }

    private static int gcd(int p, int q) {
        if (q == 0) return p;
        else return gcd(q, p % q);
    }

    private static int[] ratio(int height, int width) {
        final int gcd = gcd(height,width);
        int h, w;
        if(height > width) {
            h = height/gcd;
            w = width/gcd;
        } else {
            h = width/gcd;
            w = height/gcd;
        }
        return new int[]{h, w};
    }
}
