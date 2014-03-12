package org.myeslib.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TimeUnitHelper {

	private static final Pattern pattern = Pattern.compile("([0-9]+)(s|m|h)");

	private final String duration;

	public TimeUnit getDurationTime() {
		Matcher m = pattern.matcher(duration.trim());
		if (m.matches()){
			if ("s".equals(m.group(2))){
				return TimeUnit.SECONDS;
			} else if ("m".equals(m.group(2))){
				return TimeUnit.MINUTES;
			} if ("h".equals(m.group(2))){
				return TimeUnit.HOURS;	
			} else {
				throw new IllegalArgumentException(String.format("1 Bad format for interval [%s]", duration));
			}
		} else {
			throw new IllegalArgumentException(String.format("2 Bad format for interval [%s]", duration));
		}
	}

	public Long getDurationAsNumber() {
		Matcher m = pattern.matcher(duration.trim());
		if (m.matches()){
			return new Long(m.group(1));
		} else {
			throw new IllegalArgumentException(String.format("3 Bad format for interval [%s]", duration));
		}
	}
	
}
