package io.github.hidroh.materialistic;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ListView;

/**
 * Injectable alert dialog builder, allowing swapping between
 * {@link android.support.v7.app.AlertDialog.Builder} and {@link android.app.AlertDialog.Builder}
 * @param <T> type of created alert dialog, extends from {@link Dialog}
 */
public interface AlertDialogBuilder<T extends Dialog> {
    /**
     * Set the message to display using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    AlertDialogBuilder setMessage(@StringRes int messageId);

    /**
     * Sets a custom view to be the contents of the alert dialog.
     * <p>
     * When using a pre-Holo theme, if the supplied view is an instance of
     * a {@link ListView} then the light background will be used.
     * <p>
     * <strong>Note:</strong> To ensure consistent styling, the custom view
     * should be inflated or constructed using the alert dialog's themed
     * context
     *
     * @param view the view to use as the contents of the alert dialog
     * @return this Builder object to allow for chaining of calls to set
     *         methods
     */
    AlertDialogBuilder setView(View view);

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param textId   The resource id of the text to display in the negative button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    AlertDialogBuilder setNegativeButton(@StringRes int textId, DialogInterface.OnClickListener listener);

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param textId   The resource id of the text to display in the positive button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    AlertDialogBuilder setPositiveButton(@StringRes int textId, DialogInterface.OnClickListener listener);

    /**
     * Creates a {@link Dialog} with the arguments supplied to this builder. It does not
     * {@link Dialog#show()} the dialog. This allows the user to do any extra processing
     * before displaying the dialog. Use {@link #show()} if you don't have any other processing
     * to do and want this to be created and displayed.
     */
    T create();

    /**
     * Creates a {@link Dialog} with the arguments supplied to this builder and
     * {@link Dialog#show()}'s the dialog.
     */
    T show();

    /**
     * {@link android.support.v7.app.AlertDialog.Builder} wrapper
     */
    class Impl implements AlertDialogBuilder<AlertDialog> {
        private final AlertDialog.Builder mBuilder;

        public Impl(Context context) {
            mBuilder = new AlertDialog.Builder(context);
        }

        @Override
        public AlertDialogBuilder setMessage(@StringRes int messageId) {
            mBuilder.setMessage(messageId);
            return this;
        }

        @Override
        public AlertDialogBuilder setView(View view) {
            mBuilder.setView(view);
            return this;
        }

        @Override
        public AlertDialogBuilder setNegativeButton(@StringRes int textId,
                                                    DialogInterface.OnClickListener listener) {
            mBuilder.setNegativeButton(textId, listener);
            return this;
        }

        @Override
        public AlertDialogBuilder setPositiveButton(@StringRes int textId,
                                                    DialogInterface.OnClickListener listener) {
            mBuilder.setPositiveButton(textId, listener);
            return this;
        }

        @Override
        public AlertDialog create() {
            return mBuilder.create();
        }

        @Override
        public AlertDialog show() {
            return mBuilder.show();
        }
    }
}