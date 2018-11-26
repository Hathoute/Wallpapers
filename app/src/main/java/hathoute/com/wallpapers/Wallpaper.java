package hathoute.com.wallpapers;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;

public class Wallpaper {
    final String name;
    final String link;
    final String thumbnailLink;
    Bitmap thumbnail;

    public Wallpaper(String name) {
        this.name = name;
        this.link = AppHelper.wallpapersLink + name;
        this.thumbnailLink = AppHelper.wallpapersLink + "thumbnail/" + name;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public boolean isDownloaded(Context context) {
        File wpFile = getFile(context);
        if(!wpFile.exists())
            return false;

        if(wpFile.length() < 10000) {
            //Something went wrong while downloading?
            wpFile.delete();
            return false;
        }

        return true;
    }

    public File getFile(Context context) {
        return new File(context.getFilesDir(), name);
    }
}
