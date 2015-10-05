package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.Intent;
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
        view.findViewById(R.id.drawer_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(ListActivity.class);
            }
        });

        view.findViewById(R.id.drawer_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(NewActivity.class);
            }
        });

        view.findViewById(R.id.drawer_show).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(ShowActivity.class);
            }
        });

        view.findViewById(R.id.drawer_ask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(AskActivity.class);
            }
        });

        view.findViewById(R.id.drawer_job).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(JobsActivity.class);
            }
        });

        view.findViewById(R.id.drawer_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(SettingsActivity.class);
            }
        });
        view.findViewById(R.id.drawer_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(AboutActivity.class);
            }
        });
        view.findViewById(R.id.drawer_favorite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(FavoriteActivity.class);
            }
        });
        return view;
    }

    private void navigate(final Class<? extends Activity> activityClass) {
        ((DrawerActivity) getActivity()).closeDrawers();
        if (!getActivity().getClass().equals(activityClass)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final Intent intent = new Intent(getActivity(), activityClass);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    getActivity().startActivity(intent);
                }
            }, DRAWER_SLIDE_DURATION_MS);
        }
    }

}
