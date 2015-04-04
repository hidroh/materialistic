package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Base fragment which performs injection using parent's activity object graphs if any
 */
public abstract class BaseFragment extends Fragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof Injectable) {
            ((Injectable) getActivity()).inject(this);
        }
    }
}
