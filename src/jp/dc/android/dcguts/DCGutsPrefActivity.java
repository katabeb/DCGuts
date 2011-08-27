package jp.dc.android.dcguts;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class DCGutsPrefActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}
