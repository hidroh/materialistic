package io.github.hidroh.materialistic.widget.preference;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ScrollView;

import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;

public class HelpLazyLoadView extends ScrollView {
    public HelpLazyLoadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(LayoutInflater.from(context).inflate(R.layout.include_help_lazy_load, this, false));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.comments));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.article));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.readability));
        Preferences.StoryViewMode defaultView = Preferences.getDefaultStoryView(getContext());
        int defaultTab;
        switch (defaultView) {
            case Comment:
            default:
                defaultTab = 0;
                break;
            case Article:
                defaultTab = 1;
                break;
            case Readability:
                defaultTab = 2;
                break;
        }
        tabLayout.getTabAt(defaultTab).select();
    }
}
