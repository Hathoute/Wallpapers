package hathoute.com.wallpapers;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.ads.MobileAds;

import java.util.Random;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        MobileAds.initialize(this, "ca-app-pub-9871252548902893~4870863822");
        startService(new Intent(this, AdsBackgroundService.class));

        final ImageView spLogo = findViewById(R.id.ivLogo);
        final ProgressBar spLoad = findViewById(R.id.progressBar);
        Animation splashanim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in);
        spLoad.startAnimation(splashanim);
        Thread splashTimer = new Thread() {
            public void run() {
                Random ran = new Random();
                int sleeptime = 2000 + ran.nextInt(2000);
                try {
                    sleep(sleeptime);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity_Launch(spLogo, spLoad);
                    }
                });
            }
        };
        splashTimer.start();
    }

    public void MainActivity_Launch(ImageView spLogo, ProgressBar spLoad) {
        Animation splashanimback = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.splash_fade_out);
        spLogo.startAnimation(splashanimback);
        spLoad.startAnimation(splashanimback);
        final Context packageContext = this;
        final Intent activityMain = new Intent(packageContext, CategoriesActivity.class);
        Thread splashendTimer = new Thread() {
            public void run() {
                try {
                    sleep(1500);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                finally {
                    startActivity(activityMain);
                    finish();
                }
            }
        };

        splashendTimer.start();
    }
}