package com.gruutnetworks.gruutsigner.ui.dashboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.gruutnetworks.gruutsigner.R;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, DashboardFragment.newInstance())
                    .commitNow();
        }
    }
}
