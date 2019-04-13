package hathoute.com.wallpapers;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CustomGridAdapter extends BaseAdapter {

    private int[][] myApps;
    private Context context;
    private LayoutInflater thisInflater;

    public CustomGridAdapter(Context context, int[][] myApps) {
        this.context = context;
        this.thisInflater = LayoutInflater.from(context);
        this.myApps = myApps;
    }

    @Override
    public int getCount() {
        return myApps.length;
    }

    @Override
    public Object getItem(int position) {
        return myApps[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = thisInflater.inflate( R.layout.grid_item, parent, false );
        }

        TextView tv = convertView.findViewById(R.id.text);
        AppCompatImageView iv = convertView.findViewById(R.id.picture);

        tv.setText(context.getResources().getString(myApps[position][1]));
        iv.setImageResource(myApps[position][0]);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            convertView.setElevation(10);

        return convertView;
    }
}