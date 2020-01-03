package de.recondita.heizung.ical;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentSequenceComparator;


/**
 * Google Calender doesn't increase the sequence number, if only the end date was changed.
 */
public class PatchedComponentSequenceComparator extends ComponentSequenceComparator {

	@Override
	public int compare(Component o1, Component o2) {
		int retVal = super.compare(o1, o2);
		if (retVal != 0)
			return retVal;

		if (o1.getProperty("RECURRENCE-ID") != null)
			retVal = 1;
		if (o2.getProperty("RECURRENCE-ID") != null)
			retVal--;
		return retVal;
	}
}
