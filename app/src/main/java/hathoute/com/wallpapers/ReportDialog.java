package hathoute.com.wallpapers;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class ReportDialog {
    private final Context mContext;
    private final OnSubmitListener listener;
    private EditText etUserMail, etReason;
    private Button bDismiss;

    public ReportDialog(Context context, OnSubmitListener l) {
        listener = l;
        mContext = context;
    }

    public void show() {
        final Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_report);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        etUserMail = dialog.findViewById(R.id.etUserMail);
        etReason = dialog.findViewById(R.id.etReason);
        bDismiss = dialog.findViewById(R.id.bDismiss);

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        bDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canSumbit() == null) {
                    new AddReport(etUserMail.getText().toString(), etReason.getText().toString())
                            .execute();
                    listener.onSubmit();
                    dialog.dismiss();
                }
            }
        });
    }

    private View canSumbit() {
        Resources resources = mContext.getResources();
        String mail = etUserMail.getText().toString();
        String report = etUserMail.getText().toString();
        View request = null;

        if(!mail.contains("@") || !mail.contains(".")) {
            etUserMail.setError(resources.getString(R.string.error_mail_incorrect));
            request = etUserMail;
        }
        if(report.length() == 0) {
            etReason.setError(resources.getString(R.string.error_report_short));
            request = etReason;
        } else if(report.length() < 8) {
            etReason.setError(resources.getString(R.string.error_report_short));
            request = etReason;
        } else if(report.length() > 30) {
            etReason.setError(resources.getString(R.string.error_report_long));
            request = etReason;
        }
        return request;
    }


    public OnSubmitListener mOnSubmitListener;

    public interface OnSubmitListener {

        void onSubmit();
    }
}