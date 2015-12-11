package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class DrawerFragment extends BaseFragment {
    @Inject AlertDialogBuilder mLogoutAlertDialogBuilder;
    @Inject AlertDialogBuilder mAccountAlertDialogBuilder;
    @Inject AccountManager mAccountManager;
    private TextView mDrawerAccount;
    private View mDrawerLogout;
    private final SharedPreferences.OnSharedPreferenceChangeListener mLoginListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getActivity() == null) {
                return;
            }
            if (TextUtils.equals(key, getActivity().getString(R.string.pref_username))) {
                setUsername();
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(mLoginListener);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_drawer, container, false);
        mDrawerAccount = (TextView) view.findViewById(R.id.drawer_account);
        mDrawerAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account[] accounts = mAccountManager.getAccountsByType(BuildConfig.APPLICATION_ID);
                if (accounts.length == 0) {
                    navigate(LoginActivity.class);
                } else {
                    showAccountChooser(accounts);
                }
            }
        });
        mDrawerLogout = view.findViewById(R.id.drawer_logout);
        mDrawerLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogoutAlertDialogBuilder.setMessage(R.string.logout_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Preferences.setUsername(getActivity(), null);
                            }
                        })
                        .show();
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUsername();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mLoginListener);
    }

    private void navigate(final Class<? extends Activity> activityClass) {
        ((DrawerActivity) getActivity()).navigate(activityClass);
    }

    private void setUsername() {
        if (getView() == null) {
            return;
        }
        String username = Preferences.getUsername(getActivity());
        if (!TextUtils.isEmpty(username)) {
            mDrawerAccount.setText(username);
            mDrawerLogout.setVisibility(View.VISIBLE);
        } else {
            mDrawerAccount.setText(R.string.login);
            mDrawerLogout.setVisibility(View.GONE);
        }
    }

    private void showAccountChooser(Account[] accounts) {
        final String[] items = new String[accounts.length + 1];
        int checked = -1;
        for (int i = 0; i < accounts.length; i++) {
            String accountName = accounts[i].name;
            items[i] = accountName;
            if (TextUtils.equals(accountName, mDrawerAccount.getText())) {
                checked = i;
            }
        }
        items[items.length - 1] = getString(R.string.add_account);
        mAccountAlertDialogBuilder.setTitle(R.string.choose_account)
                .setSingleChoiceItems(items, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == items.length - 1) {
                            navigate(LoginActivity.class);
                        } else {
                            Preferences.setUsername(getActivity(), items[which]);
                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
