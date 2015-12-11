package io.github.hidroh.materialistic;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DrawerFragment extends BaseFragment {

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_drawer, container, false);
        view.findViewById(R.id.drawer_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(LoginActivity.class);
            }
        });

        view.findViewById(R.id.drawer_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(ListActivity.class);
            }
        });

        view.findViewById(R.id.drawer_popular).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(PopularActivity.class);
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
        view.findViewById(R.id.drawer_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DrawerActivity) getActivity()).showFeedback();
            }
        });
        return view;
    }

    private void navigate(final Class<? extends Activity> activityClass) {
        ((DrawerActivity) getActivity()).navigate(activityClass);
    }
}
