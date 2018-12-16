package main.java.aftermath.locale;

import java.lang.reflect.Field;

public class DEBUG extends LocaleBase {
	public DEBUG() throws IllegalArgumentException, IllegalAccessException {
		super();

		// Implement reflection method for crawling through all variables and calling
		// the variable values the same as the code value.
		this.SERVER_TITLE = "Aftermath";

		Field[] fields = this.getClass().getFields();

		for (Field f : fields) {
			f.set(this, (Object) ("##" + f.getName() + "##"));
		}
	}
}
