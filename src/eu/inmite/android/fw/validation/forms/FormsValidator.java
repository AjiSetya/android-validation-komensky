/*
 * Copyright (c) 2013, Inmite s.r.o. (www.inmite.eu).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Inmite s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package eu.inmite.android.fw.validation.forms;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;
import eu.inmite.android.fw.validation.exception.FormsValidationException;
import eu.inmite.android.fw.validation.forms.annotations.Condition;
import eu.inmite.android.fw.validation.forms.iface.ICondition;
import eu.inmite.android.fw.validation.forms.iface.IFieldAdapter;
import eu.inmite.android.fw.validation.forms.iface.IValidator;
import eu.inmite.android.fw.validation.forms.validators.ValidatorFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomas Vondracek
 */
public class FormsValidator {

	public static interface IValidationCallback {
		void validationComplete(boolean result, List<ValidationResult> failedValidations);
	}

	/**
	 * register custom validator
	 */
	@SuppressWarnings("unchecked")
	public static void registerValidator(Class<? extends IValidator<?>> validator) {
		if (validator == null) {
			throw new IllegalArgumentException("validator cannot be null");
		}

		ValidatorFactory.registerValidatorClasses(validator);
	}

	/**
	 * perform validation over the activity
	 * @param activity activity with views to validate
	 * @param callback callback the will receive result of validation
	 * @return whether the validation succeeded
	 */
	public static boolean validate(Activity activity, IValidationCallback callback) {
		return validate(activity, activity, callback);
	}

	/**
	 * perform validation over the given fragment
	 * @param fragment fragment with views to validate
	 * @param callback callback the will receive result of validation
	 * @return whether the validation succeeded
	 */
	public static boolean validate(Fragment fragment, IValidationCallback callback) {
		return validate(fragment.getActivity(), fragment, callback);
	}

	/**
	 * perform validation over the target object
	 * @return whether the validation succeeded
	 * @throws FormsValidationException
	 */
	@SuppressWarnings("unchecked")
	public synchronized static boolean validate(Context context, Object target, IValidationCallback callback) throws FormsValidationException {
		if (context == null) {
			throw new IllegalArgumentException("context cannot ben null");
		}
		if (target == null) {
			throw new IllegalArgumentException("target cannot be null");
		}

		final List<ValidationResult> failedValidations = new ArrayList<ValidationResult>();
		boolean result = true;
		final Field[] fields = target.getClass().getDeclaredFields();
		for (Field field : fields) {
			final Condition conditionAnnotation = field.getAnnotation(Condition.class);
			if (conditionAnnotation != null && conditionAnnotation.validationAnnotation().equals(Condition.class)) {
				// condition is applied to all validations
				boolean evaluation = evaluateCondition(target, conditionAnnotation);

				if (! evaluation) {
					// go to next field
					continue;
				}
			}

			Annotation[] annotations = field.getDeclaredAnnotations();
			if (annotations.length > 0) {
				if (! View.class.isAssignableFrom(field.getType())) {
					// next field
					continue;
				}
				final View view;
				try {
					field.setAccessible(true);
					view = (View) field.get(target);
				} catch (IllegalAccessException e) {
					throw new FormsValidationException(e);
				}

				for (Annotation annotation : annotations) {
					try {
						IValidator validator = ValidatorFactory.getValidator(annotation);
						if (validator == null) {
							continue;
						}
						if (conditionAnnotation != null && conditionAnnotation.validationAnnotation().equals(annotation.annotationType())) {
							boolean evaluation = evaluateCondition(target, conditionAnnotation);

							if (! evaluation) {
								// continue to next annotation
								continue;
							}
						}
						IFieldAdapter adapter = FieldAdapterFactory.getAdapterForField(view, annotation);

						Object value = adapter.getFieldValue(annotation, target, view);
						boolean isValid = validator.validate(annotation, value);
						result &= isValid;

						if (! isValid) {
							String message = validator.getMessage(context, annotation, value);
							failedValidations.add(new ValidationResult(view, message));

							// go to next field
							break;
						}
					} catch (IllegalAccessException e) {
						throw new FormsValidationException(e);
					} catch (InstantiationException e) {
						throw new FormsValidationException(e);
					}
				}
			}
		}
		if (callback != null) {
			callback.validationComplete(result, failedValidations);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static boolean evaluateCondition(Object target, Condition conditionAnnotation) {
		final View conditionView;
		final int viewId = conditionAnnotation.viewId();

		if (target instanceof Activity) {
			conditionView = ((Activity) target).findViewById(viewId);
		} else if (target instanceof Fragment) {
			conditionView = ((Fragment) target).getView().findViewById(viewId);
		} else if (target instanceof View) {
			conditionView = ((View) target).findViewById(viewId);
		} else {
			throw new FormsValidationException("unknown target " + target);
		}

		Class<? extends ICondition> clazz = conditionAnnotation.value();
		IFieldAdapter adapter = FieldAdapterFactory.getAdapterForField(conditionView, null);
		Object value = adapter.getFieldValue(null, target, conditionView);
		try {
			return clazz.newInstance().evaluate(value);
		} catch (InstantiationException e) {
			throw new FormsValidationException(e);
		} catch (IllegalAccessException e) {
			throw new FormsValidationException(e);
		}
	}

	public static final class ValidationResult {
		public final View view;
		public final String message;

		private ValidationResult(View view, String message) {
			this.view = view;
			this.message = message;
		}
	}
}