package hathoute.com.wallpapers;

import android.graphics.Bitmap;

public class Category {

    final String name;
    final String fullName;
    final String thumbLink;
    final boolean isParent;
    Bitmap thumbnail;

    public Category(String name, String fullName, String thumbId, boolean isParent) {
        this.name = name;
        this.fullName = fullName;
        this.isParent = isParent;
        this.thumbLink = AppHelper.wallpapersLink + "thumbnail/" + thumbId + ".jpg";
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }
}
