package com.dev.infinitoz.trip;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.util.Utility;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.math.BigInteger;

public class EarnCoinsActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private RewardedVideoAd mAd;
    private TextView coinsText;
    private User currentUser;
    private Button startVideoBT;
//    private BigInteger coins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earn_coins);

        coinsText = findViewById(R.id.coinsText);
        startVideoBT = findViewById(R.id.startVideo);
        currentUser = (User) TripContext.getValue(Constants.CURRENT_USER);
        coinsText.setText(Constants.AVLBL_COINS + currentUser.getCoins());
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");

        mAd = MobileAds.getRewardedVideoAdInstance(this);
        mAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAd();

    }


    private void loadRewardedVideoAd() {
        if (!mAd.isLoaded()) {
            mAd.loadAd("ca-app-pub-3940256099942544/5224354917", new AdRequest.Builder().build());
        }
    }

    public void startVideo(View view) {
        if (mAd.isLoaded()) {
            mAd.show();
        }
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        startVideoBT.setEnabled(true);
    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

        loadRewardedVideoAd();

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {

        BigInteger credits = new BigInteger(currentUser.getCoins());
        credits = credits.add(BigInteger.valueOf(5));
        currentUser.setCoins(credits.toString());
        coinsText.setText(Constants.AVLBL_COINS + credits.toString());
        Utility.updateCoinsToUser(currentUser.getuId(), credits.toString());
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }

    @Override
    public void onRewardedVideoCompleted() {

    }

    @Override
    protected void onPause() {
        mAd.pause(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        mAd.resume(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mAd.destroy(this);
        super.onDestroy();
    }
}
