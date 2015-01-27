package io.github.hidroh.materialistic;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

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
    }

    private void setTextWithLinks(@IdRes int textViewResId, String htmlText) {
        final TextView textView = (TextView) findViewById(textViewResId);
        textView.setText(Html.fromHtml(htmlText));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
