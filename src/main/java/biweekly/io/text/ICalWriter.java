package biweekly.io.text;

import static biweekly.io.DataModelConverter.convert;
import static biweekly.util.IOUtils.utf8Writer;

import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import biweekly.ICalDataType;
import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.ICalComponent;
import biweekly.component.VAlarm;
import biweekly.component.VTimezone;
import biweekly.io.SkipMeException;
import biweekly.io.StreamWriter;
import biweekly.io.scribe.component.ICalComponentScribe;
import biweekly.io.scribe.property.ICalPropertyScribe;
import biweekly.parameter.ICalParameters;
import biweekly.property.Attendee;
import biweekly.property.Created;
import biweekly.property.Daylight;
import biweekly.property.ICalProperty;
import biweekly.property.Organizer;
import biweekly.property.VCalAlarmProperty;
import biweekly.property.Version;

/*
 Copyright (c) 2013-2014, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * <p>
 * Writes {@link ICalendar} objects to an iCalendar data stream.
 * </p>
 * <p>
 * <b>Example:</b>
 * 
 * <pre class="brush:java">
 * List&lt;ICalendar&gt; icals = ... 
 * OutputStream out = ...
 * ICalWriter icalWriter = new ICalWriter(out, ICalVersion.V2_0);
 * for (ICalendar ical : icals){
 *   icalWriter.write(ical);
 * }
 * icalWriter.close();
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * <b>Changing the timezone settings:</b>
 * 
 * <pre class="brush:java">
 * ICalWriter writer = new ICalWriter(...);
 * 
 * //format all date/time values in a specific timezone instead of UTC
 * //note: this makes an HTTP call to the "tzurl.org" website
 * writer.getTimezoneInfo().setDefaultTimeZone(TimeZone.getDefault());
 * 
 * //format the value of a particular date/time property in a specific timezone instead of UTC
 * //note: this makes an HTTP call to the "tzurl.org" website
 * DateStart dtstart = ...
 * writer.getTimezoneInfo().setTimeZone(dtstart, TimeZone.getDefault());
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * <b>Changing the line folding settings:</b>
 * 
 * <pre class="brush:java">
 * ICalWriter writer = new ICalWriter(...);
 * 
 * //disable line folding
 * writer.getRawWriter().getFoldedLineWriter().setLineLength(null);
 * 
 * //change line length
 * writer.getRawWriter().getFoldedLineWriter().setLineLength(50);
 * 
 * //change folded line indent string
 * writer.getRawWriter().getFoldedLineWriter().setIndent("\t");
 * 
 * //change newline character
 * writer.getRawWriter().getFoldedLineWriter().setNewline("**");
 * </pre>
 * 
 * </p>
 * @author Michael Angstadt
 * @see <a href="http://tools.ietf.org/html/rfc5545">RFC 5545</a>
 */
public class ICalWriter extends StreamWriter implements Flushable {
	private final ICalRawWriter writer;

	/**
	 * Creates an iCalendar writer that writes to an output stream.
	 * @param outputStream the output stream to write to
	 * @param version the iCalendar version to adhere to
	 */
	public ICalWriter(OutputStream outputStream, ICalVersion version) {
		this((version == ICalVersion.V1_0) ? new OutputStreamWriter(outputStream) : utf8Writer(outputStream), version);
	}

	/**
	 * Creates an iCalendar writer that writes to a file.
	 * @param file the file to write to
	 * @param version the iCalendar version to adhere to
	 * @throws IOException if the file cannot be written to
	 */
	public ICalWriter(File file, ICalVersion version) throws IOException {
		this(file, false, version);
	}

	/**
	 * Creates an iCalendar writer that writes to a file.
	 * @param file the file to write to
	 * @param version the iCalendar version to adhere to
	 * @param append true to append to the end of the file, false to overwrite
	 * it
	 * @throws IOException if the file cannot be written to
	 */
	public ICalWriter(File file, boolean append, ICalVersion version) throws IOException {
		this((version == ICalVersion.V1_0) ? new FileWriter(file, append) : utf8Writer(file, append), version);
	}

	/**
	 * Creates an iCalendar writer that writes to a writer.
	 * @param writer the output stream to write to
	 * @param version the iCalendar version to adhere to
	 */
	public ICalWriter(Writer writer, ICalVersion version) {
		this.writer = new ICalRawWriter(writer, version);
	}

	/**
	 * Gets the writer object that is used internally to write to the output
	 * stream.
	 * @return the raw writer
	 */
	public ICalRawWriter getRawWriter() {
		return writer;
	}

	/**
	 * Gets the version that the written iCalendar objects will adhere to.
	 * @return the iCalendar version
	 */
	@Override
	public ICalVersion getTargetVersion() {
		return writer.getVersion();
	}

	/**
	 * Sets the version that the written iCalendar objects will adhere to.
	 * @param targetVersion the iCalendar version
	 */
	public void setTargetVersion(ICalVersion targetVersion) {
		writer.setVersion(targetVersion);
	}

	/**
	 * <p>
	 * Gets whether the writer will apply circumflex accent encoding on
	 * parameter values (disabled by default). This escaping mechanism allows
	 * for newlines and double quotes to be included in parameter values.
	 * </p>
	 * 
	 * <p>
	 * When disabled, the writer will replace newlines with spaces and double
	 * quotes with single quotes.
	 * </p>
	 * @return true if circumflex accent encoding is enabled, false if not
	 * @see ICalRawWriter#isCaretEncodingEnabled()
	 */
	public boolean isCaretEncodingEnabled() {
		return writer.isCaretEncodingEnabled();
	}

	/**
	 * <p>
	 * Sets whether the writer will apply circumflex accent encoding on
	 * parameter values (disabled by default). This escaping mechanism allows
	 * for newlines and double quotes to be included in parameter values.
	 * </p>
	 * 
	 * <p>
	 * When disabled, the writer will replace newlines with spaces and double
	 * quotes with single quotes.
	 * </p>
	 * @param enable true to use circumflex accent encoding, false not to
	 * @see ICalRawWriter#setCaretEncodingEnabled(boolean)
	 */
	public void setCaretEncodingEnabled(boolean enable) {
		writer.setCaretEncodingEnabled(enable);
	}

	@Override
	protected void _write(ICalendar ical) throws IOException {
		writeComponent(ical, null);
	}

	/**
	 * Writes a component to the data stream.
	 * @param component the component to write
	 * @param parent the parent component
	 * @throws IOException if there's a problem writing to the data stream
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void writeComponent(ICalComponent component, ICalComponent parent) throws IOException {
		switch (writer.getVersion()) {
		case V1_0:
			//VTIMEZONE component => DAYLIGHT properties
			if (component instanceof VTimezone) {
				VTimezone timezone = (VTimezone) component;
				List<Daylight> daylights = convert(timezone);
				for (Daylight daylight : daylights) {
					writeProperty(daylight);
				}
				return;
			}

			//VALARM component => vCal alarm property
			if (component instanceof VAlarm) {
				VAlarm valarm = (VAlarm) component;
				VCalAlarmProperty vcalAlarm = convert(valarm, component);
				if (vcalAlarm != null) {
					writeProperty(vcalAlarm);
					return;
				}
			}

			break;

		default:
			//empty
			break;
		}

		ICalComponentScribe componentScribe = index.getComponentScribe(component);
		writer.writeBeginComponent(componentScribe.getComponentName());

		List propertyObjs = componentScribe.getProperties(component);
		if (component instanceof ICalendar && component.getProperty(Version.class) == null) {
			propertyObjs.add(0, new Version(writer.getVersion()));
		}

		for (Object propertyObj : propertyObjs) {
			context.setParent(component); //set parent here incase a scribe resets the parent
			ICalProperty property = (ICalProperty) propertyObj;
			writeProperty(property);
		}

		Collection subComponents = componentScribe.getComponents(component);
		if (component instanceof ICalendar) {
			//add the VTIMEZONE components that were auto-generated
			Collection<VTimezone> timezones = tzinfo.getComponents();
			for (VTimezone timezone : timezones) {
				if (!subComponents.contains(timezone)) {
					subComponents.add(timezone);
				}
			}
		}

		for (Object subComponentObj : subComponents) {
			ICalComponent subComponent = (ICalComponent) subComponentObj;
			writeComponent(subComponent, component);
		}

		writer.writeEndComponent(componentScribe.getComponentName());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void writeProperty(ICalProperty property) throws IOException {
		switch (writer.getVersion()) {
		case V1_0:
			//ORGANIZER property => ATTENDEE with role of "organizer" 
			if (property instanceof Organizer) {
				Organizer organizer = (Organizer) property;
				Attendee attendee = convert(organizer);
				writeProperty(attendee);
				return;
			}
			break;

		default:
			//empty
			break;
		}

		ICalPropertyScribe scribe = index.getPropertyScribe(property);

		//marshal property
		String value;
		try {
			value = scribe.writeText(property, context);
		} catch (SkipMeException e) {
			return;
		}

		//get parameters
		ICalParameters parameters = scribe.prepareParameters(property, context);

		//set the data type
		ICalDataType dataType = scribe.dataType(property, writer.getVersion());
		if (dataType != null && dataType != scribe.defaultDataType(writer.getVersion())) {
			//only add a VALUE parameter if the data type is (1) not "unknown" and (2) different from the property's default data type
			parameters = new ICalParameters(parameters);
			parameters.setValue(dataType);
		}

		//get the property name
		String propertyName;
		if (writer.getVersion() == ICalVersion.V1_0 && property instanceof Created) {
			//the vCal DCREATED property is the same as the iCal CREATED property
			propertyName = "DCREATED";
		} else {
			propertyName = scribe.getPropertyName();
		}

		//write property to data stream
		writer.writeProperty(propertyName, parameters, value);
	}

	/**
	 * Flushes the stream.
	 * @throws IOException if there's a problem flushing the stream
	 */
	public void flush() throws IOException {
		writer.flush();
	}

	/**
	 * Closes the underlying {@link Writer} object.
	 */
	public void close() throws IOException {
		writer.close();
	}
}
