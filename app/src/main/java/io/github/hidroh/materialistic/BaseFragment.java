package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Base fragment which performs injection using parent's activity object graphs if any
 */
public abstract class BaseFragment extends Fragment {
    protected final MenuTintDelegate mMenuTintDelegate = new MenuTintDelegate();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof Injectable) {
            ((Injectable) getActivity()).inject(this);
        }
        mMenuTintDelegate.onActivityCreated(getActivity());
    }

    @CallSuper
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuTintDelegate.onOptionsMenuCreated(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
