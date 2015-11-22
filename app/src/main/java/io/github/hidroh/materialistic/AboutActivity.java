package io.github.hidroh.materialistic;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class AboutActivity extends DrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);

        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
        setTextWithLinks(R.id.text_application_info, getString(R.string.application_info_text, versionName));
        setTextWithLinks(R.id.text_developer_info, getString(R.string.developer_info_text));
        setTextWithLinks(R.id.text_libraries, getString(R.string.libraries_text));
        setTextWithLinks(R.id.text_license, getString(R.string.license_text));
        setTextWithLinks(R.id.text_3rd_party_licenses, getString(R.string.third_party_licenses_text));
    }

    private void setTextWithLinks(@IdRes int textViewResId, String htmlText) {
        AppUtils.setTextWithLinks((TextView) findViewById(textViewResId), htmlText);
    }
}
