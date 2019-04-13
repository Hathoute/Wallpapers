package hathoute.com.wallpapers;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import eu.janmuller.android.simplecropimage.CropImage;

public class AppHelper {

    public final static int PIC_CROP = 1;
    public final static String wallpapersLink = "http://" + BuildConfig.SERVER_IP +
            "/wallpapers/" + BuildConfig.APP_PREFIX + "/";
    public final static String instagramLink = "https://www.instagram.com/the.whitesmith/";
    public final static String APP_TAG = BuildConfig.APP_PREFIX + "WP";
    public final static String FOLDER_NAME = BuildConfig.APP_PREFIX + "Wallpapers";
    public final static int PERMISSION_WRITE = 1;


    public static void openStorePage(Context context, String packageName) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    public static void addImageGallery(Context context, File file) {
        // Get image path and update Gallery database.
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static boolean canSave(Context context) {
        int result = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // Using 'or' since we don't have to ask for Permissions in versions older than API 23.
        return (result == PackageManager.PERMISSION_GRANTED) ||
                (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    public static void askForWritePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE);
    }

    public static void saveImage(Context context, Wallpaper wallpaper) {
        File sourceLocation = wallpaper.getFile(context);
        File targetLocation = new File(Environment.getExternalStorageDirectory(),
                AppHelper.FOLDER_NAME);
        if(!targetLocation.exists())
            if(!targetLocation.mkdir())
                return;

        targetLocation = new File(targetLocation, wallpaper.name);

        try {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from inStream to outStream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            // We are here, It means that the file got saved.
            // Insert image data to gallery database.
            addImageGallery(context, targetLocation);
        } catch(IOException ignored) {
        }
    }

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

    public static void openRatePage(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
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
