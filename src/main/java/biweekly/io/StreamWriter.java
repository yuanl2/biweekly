package biweekly.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.ICalComponent;
import biweekly.component.RawComponent;
import biweekly.io.scribe.ScribeIndex;
import biweekly.io.scribe.component.ICalComponentScribe;
import biweekly.io.scribe.property.ICalPropertyScribe;
import biweekly.property.ICalProperty;
import biweekly.property.RawProperty;

/*
 Copyright (c) 2013, Michael Angstadt
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
 * Writes iCalendar objects to a data stream.
 * @author Michael Angstadt
 */
public abstract class StreamWriter implements Closeable {
	protected ScribeIndex index = new ScribeIndex();
	protected WriteContext context;
	protected TimezoneInfo tzinfo = new TimezoneInfo();

	/**
	 * Writes an iCalendar object to the data stream.
	 * @param ical the iCalendar object to write
	 * @throws IllegalArgumentException if the scribe class for a component or
	 * property object cannot be found (only happens when an experimental
	 * property/component scribe is not registered with the
	 * {@code registerScribe} method.)
	 * @throws IOException if there's a problem writing to the data stream
	 */
	public void write(ICalendar ical) throws IOException {
		Collection<Class<? extends Object>> unregistered = findScribeless(ical);
		if (!unregistered.isEmpty()) {
			throw new IllegalArgumentException("No scribes were found the following component/property classes: " + unregistered);
		}

		context = new WriteContext(getTargetVersion(), tzinfo);
		_write(ical);
	}

	/**
	 * Gets the version that the next iCalendar object will be written as.
	 * @return the version
	 */
	protected abstract ICalVersion getTargetVersion();

	/**
	 * Writes an iCalendar object to the data stream.
	 * @param ical the iCalendar object to write
	 * @throws IOException if there's a problem writing to the data stream
	 */
	protected abstract void _write(ICalendar ical) throws IOException;

	/**
	 * Gets the timezone-related info for this writer.
	 * @return the timezone-related info
	 */
	public TimezoneInfo getTimezoneInfo() {
		return tzinfo;
	}

	/**
	 * Sets the timezone-related info for this writer.
	 * @param tzinfo the timezone-related info
	 */
	public void setTimezoneInfo(TimezoneInfo tzinfo) {
		this.tzinfo = tzinfo;
	}

	/**
	 * <p>
	 * Registers an experimental property scribe. Can also be used to override
	 * the scribe of a standard property (such as DTSTART). Calling this method
	 * is the same as calling:
	 * </p>
	 * <p>
	 * {@code getScribeIndex().register(scribe)}.
	 * </p>
	 * @param scribe the scribe to register
	 */
	public void registerScribe(ICalPropertyScribe<? extends ICalProperty> scribe) {
		index.register(scribe);
	}

	/**
	 * <p>
	 * Registers an experimental component scribe. Can also be used to override
	 * the scribe of a standard component (such as VEVENT). Calling this method
	 * is the same as calling:
	 * </p>
	 * <p>
	 * {@code getScribeIndex().register(scribe)}.
	 * </p>
	 * @param scribe the scribe to register
	 */
	public void registerScribe(ICalComponentScribe<? extends ICalComponent> scribe) {
		index.register(scribe);
	}

	/**
	 * Gets the object that manages the component/property scribes.
	 * @return the scribe index
	 */
	public ScribeIndex getScribeIndex() {
		return index;
	}

	/**
	 * Sets the object that manages the component/property scribes.
	 * @param scribe the scribe index
	 */
	public void setScribeIndex(ScribeIndex scribe) {
		this.index = scribe;
	}

	/**
	 * Gets the component/property classes that don't have scribes associated
	 * with them.
	 * @param ical the iCalendar object
	 * @return the component/property classes
	 */
	private Collection<Class<? extends Object>> findScribeless(ICalendar ical) {
		Set<Class<?>> unregistered = new HashSet<Class<?>>();
		LinkedList<ICalComponent> components = new LinkedList<ICalComponent>();
		components.add(ical);

		while (!components.isEmpty()) {
			ICalComponent component = components.removeLast();

			Class<? extends ICalComponent> componentClass = component.getClass();
			if (componentClass != RawComponent.class && index.getComponentScribe(componentClass) == null) {
				unregistered.add(componentClass);
			}

			for (Map.Entry<Class<? extends ICalProperty>, List<ICalProperty>> entry : component.getProperties()) {
				List<ICalProperty> properties = entry.getValue();
				if (properties.isEmpty()) {
					continue;
				}

				Class<? extends ICalProperty> clazz = entry.getKey();
				if (clazz != RawProperty.class && index.getPropertyScribe(clazz) == null) {
					unregistered.add(clazz);
				}
			}

			components.addAll(component.getComponents().values());
		}

		return unregistered;
	}
}