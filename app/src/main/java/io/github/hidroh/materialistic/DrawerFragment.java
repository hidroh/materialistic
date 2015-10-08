package io.github.hidroh.materialistic;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FeedbackClient;

public class DrawerFragment extends BaseFragment {

    private static final long DRAWER_SLIDE_DURATION_MS = 250;
    @Inject FeedbackClient mFeedbackClient;
    @Inject AlertDialogBuilder mAlertDialogBuilder;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
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
        view.findViewById(R.id.drawer_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DrawerActivity) getActivity()).closeDrawers();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showFeedbackDialog(inflater.inflate(R.layout.dialog_feedback, null, false));
                    }
                }, DRAWER_SLIDE_DURATION_MS);
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

    private void showFeedbackDialog(View dialogView) {
        final TextInputLayout titleLayout = (TextInputLayout)
                dialogView.findViewById(R.id.textinput_title);
        final TextInputLayout bodyLayout = (TextInputLayout)
                dialogView.findViewById(R.id.textinput_body);
        final EditText title = (EditText) dialogView.findViewById(R.id.edittext_title);
        final EditText body = (EditText) dialogView.findViewById(R.id.edittext_body);
        final View sendButton = dialogView.findViewById(R.id.feedback_button);
        final Dialog dialog = mAlertDialogBuilder
                .setView(dialogView)
                .create();
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titleLayout.setErrorEnabled(false);
                bodyLayout.setErrorEnabled(false);
                if (title.length() == 0) {
                    titleLayout.setError(getString(R.string.title_required));
                }
                if (body.length() == 0) {
                    bodyLayout.setError(getString(R.string.comment_required));
                }
                if (title.length() == 0 || body.length() == 0) {
                    return;
                }
                sendButton.setEnabled(false);
                mFeedbackClient.send(title.getText().toString(), body.getText().toString(),
                        new FeedbackClient.Callback() {
                            @Override
                            public void onSent(boolean success) {
                                if (getActivity() == null) {
                                    return;
                                }
                                Toast.makeText(getActivity(),
                                        success ? R.string.feedback_sent : R.string.feedback_failed,
                                        Toast.LENGTH_SHORT)
                                        .show();
                                if (!dialog.isShowing()) {
                                    return;
                                }
                                if (success) {
                                    dialog.dismiss();
                                } else {
                                    sendButton.setEnabled(true);
                                }
                            }
                        });
            }
        });
        dialog.show();
    }

}
