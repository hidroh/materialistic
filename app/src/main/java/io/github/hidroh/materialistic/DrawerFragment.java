package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DrawerFragment extends Fragment {

    private static final long DRAWER_SLIDE_DURATION_MS = 250;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_drawer, container, false);
        view.findViewById(R.id.drawerList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(ListActivity.class);
            }
        });
        view.findViewById(R.id.drawerSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    navigate(ActionBarSettingsActivity.class);
                } else {
                    navigate(SettingsActivity.class);
                }
            }
        });
        view.findViewById(R.id.drawerMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(MainActivity.class);
            }
        });
        return view;
    }

    private void navigate(final Class<? extends Activity> activityClass) {
        ((BaseActivity) getActivity()).closeDrawers();
        if (!getActivity().getClass().equals(activityClass)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final Intent intent = new Intent(getActivity(), activityClass);
                    getActivity().startActivity(intent);
                }
            }, DRAWER_SLIDE_DURATION_MS);
        }
    }

}
