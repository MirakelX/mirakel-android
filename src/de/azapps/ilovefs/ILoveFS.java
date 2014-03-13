package de.azapps.ilovefs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

public class ILoveFS {
	/**
	 * The address where to send the mail to
	 */
	public String email = null;
	/**
	 * The package name of the app (to show the app store)
	 */
	public String package_name = null;
	/**
	 * Context where to open the alertdialog
	 */
	private Context context;

	/**
	 * Simple Constructor.
	 * 
	 * @param context
	 */
	public ILoveFS(Context context) {
		this.context = context;
	}

	/**
	 * Show only the email-button
	 * 
	 * @param context
	 * @param email
	 */
	public ILoveFS(Context context, String email) {
		this.email = email;
		this.context = context;
	}

	/**
	 * Show the email and donate button
	 * 
	 * @param context
	 * @param email
	 * @param package_name
	 */
	public ILoveFS(Context context, String email, String package_name) {
		this.email = email;
		this.package_name = package_name;
		this.context = context;
	}

	public boolean isILFSDay() {
		Calendar today = new GregorianCalendar();
		return today.get(Calendar.MONTH) == Calendar.FEBRUARY
				&& today.get(Calendar.DAY_OF_MONTH) == 14;
	}

	/**
	 * Show the AlertDialog if today is I love Free Software day
	 */
	public void show() {
		final TextView message = new TextView(context);
		message.setTextAppearance(context,
				android.R.style.TextAppearance_Medium);
		message.setText(Html.fromHtml(context
				.getString(R.string.ilovefs_message)));
		if (Build.VERSION.SDK_INT < 11)
			message.setTextColor(context.getResources().getColor(R.color.white));

		int padding = 10;
		message.setPadding(padding, padding, padding, padding);
		message.setMovementMethod(LinkMovementMethod.getInstance());
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle(R.string.ilovefs_title).setView(message);
		if (rateListener != null && package_name != null)
			builder.setPositiveButton(R.string.ilovefs_rate, rateListener);
		if (emailListener != null && email != null)
			builder.setNegativeButton(R.string.ilovefs_email, emailListener);
		if (donateListener != null)
			builder.setNeutralButton(R.string.ilovefs_donate, donateListener);
		builder.show();
	}

	/**
	 * Set the Listener for the rating-button
	 */
	public DialogInterface.OnClickListener rateListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			try {
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://details?id=" + package_name)));
			} catch (android.content.ActivityNotFoundException anfe) {
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://play.google.com/store/apps/details?id="
								+ package_name)));
			}
		}
	};
	/**
	 * Set the Listener for the email-button
	 */
	public DialogInterface.OnClickListener emailListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL, new String[] { email });
			i.putExtra(Intent.EXTRA_SUBJECT,
					context.getString(R.string.ilovefs_email_title));
			try {
				Intent ci = Intent.createChooser(i,
						context.getString(R.string.ilovefs_email));
				ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(ci);
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(context,
						context.getString(R.string.ilovefs_email_no_client),
						Toast.LENGTH_SHORT).show();
			}

		}
	};
	/**
	 * Set the Listener for the donate-button
	 */
	public DialogInterface.OnClickListener donateListener = null;
}
