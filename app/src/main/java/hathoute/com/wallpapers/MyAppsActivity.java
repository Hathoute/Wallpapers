package hathoute.com.wallpapers;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class MyAppsActivity extends AppCompatActivity {

    private int[][] wpApps = {
            {R.drawable.icon_csgo, R.string.app_csgo},
            {R.drawable.icon_rl, R.string.app_rl}
    };
    private int[] appPackageNames = {
            R.string.csgo_package,
            R.string.rl_package
    };

    GridView gridApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_apps);

        gridApps = findViewById(R.id.gvApps);
        final CustomGridAdapter gridAdapter = new CustomGridAdapter(this, wpApps);
        gridApps.setAdapter(gridAdapter);
        gridApps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppHelper.openStorePage(MyAppsActivity.this,
                        getResources().getString(appPackageNames[position]));
            }
        });
    }
}
