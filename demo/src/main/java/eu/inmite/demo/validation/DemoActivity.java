package eu.inmite.demo.validation;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.*;
import eu.inmite.android.lib.dialogs.SimpleDialogFragment;
import eu.inmite.android.lib.validations.form.FormValidator;
import eu.inmite.android.lib.validations.form.annotations.DateInFuture;
import eu.inmite.android.lib.validations.form.annotations.MinValue;
import eu.inmite.android.lib.validations.form.annotations.NotEmpty;
import eu.inmite.android.lib.validations.form.annotations.RegExp;
import eu.inmite.android.lib.validations.form.callback.SimpleErrorPopupCallback;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static eu.inmite.android.lib.validations.form.annotations.RegExp.EMAIL;

/**
 * @author Tomas Vondracek
 */
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class DemoActivity extends FragmentActivity implements DatePickerDialog.OnDateSetListener {

	@NotEmpty(messageId = R.string.validation_name)
	private EditText mEditName;

	@NotEmpty
	@MinValue(value = 1l, messageId = R.string.valition_participants)
	private EditText mEditNumberOfParticipants;

	@NotEmpty(messageId = R.string.validation_valid_email)
	@RegExp(value = EMAIL, messageId = R.string.validation_valid_email)
	private EditText mEditEmail;

	@DateInFuture(messageId = R.string.validation_date)
	private Button mBtnDate;

	@NotEmpty(messageId = R.string.validation_type)
	private Spinner mSpinner;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_demo);

		mEditName = (EditText) findViewById(R.id.demo_name);
		mEditNumberOfParticipants = (EditText) findViewById(R.id.demo_participants);
		mBtnDate = (Button) findViewById(R.id.demo_date);
		mSpinner = (Spinner) findViewById(R.id.demo_spinner);

		mSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.demo_types, android.R.layout.simple_dropdown_item_1line));

		mBtnDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				DatePickerFragment fragment = new DatePickerFragment();
				fragment.show(getSupportFragmentManager(), "date");
			}
		});
		setDate(new GregorianCalendar());

		final Button btnOk = (Button) findViewById(R.id.demo_ok);
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				validate();
			}
		});
	}

	private void validate() {
		final boolean isValid = FormValidator.validate(this, new SimpleErrorPopupCallback(this, true));
		if (isValid) {
			SimpleDialogFragment.createBuilder(this, getSupportFragmentManager())
					.setMessage(R.string.validation_success)
					.setPositiveButtonText(android.R.string.ok)
					.show();
		}
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar cal = new GregorianCalendar(year, month, day);
		setDate(cal);
	}

	private void setDate(final Calendar cal) {
		final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
		mBtnDate.setText(dateFormat.format(cal.getTime()));
	}

	private static class DatePickerFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
		}

	}
}