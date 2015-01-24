package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DrawerFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_drawer, container, false);
        view.findViewById(R.id.drawer_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(ListActivity.class);
            }
        });
        return view;
    }

    private void navigate(Class<? extends BaseActivity> activityClass) {
        if (getActivity().getClass().equals(activityClass)) {
            ((BaseActivity) getActivity()).mDrawerLayout.closeDrawers();
        } else {
            final Intent intent = new Intent(getActivity(), activityClass);
            getActivity().startActivity(intent);
        }
    }
}
