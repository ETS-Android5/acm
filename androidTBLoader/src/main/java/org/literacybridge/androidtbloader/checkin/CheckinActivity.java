package org.literacybridge.androidtbloader.checkin;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.SingleFragmentActivity;
import org.literacybridge.androidtbloader.main.MainFragment;
import org.literacybridge.androidtbloader.util.Constants;

/**
 * Created by bill on 12/23/16.
 */

public class CheckinActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + CheckinActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        Log.d(TAG, "Create checkin fragment");
        return new CheckinFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
