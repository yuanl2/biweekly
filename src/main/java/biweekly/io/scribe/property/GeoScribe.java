package biweekly.io.scribe.property;

import java.util.Iterator;

import biweekly.ICalDataType;
import biweekly.io.CannotParseException;
import biweekly.io.ParseContext;
import biweekly.io.WriteContext;
import biweekly.io.json.JCalValue;
import biweekly.io.xml.XCalElement;
import biweekly.parameter.ICalParameters;
import biweekly.property.Geo;
import biweekly.util.ICalFloatFormatter;

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
 * Marshals {@link Geo} properties.
 * @author Michael Angstadt
 */
public class GeoScribe extends ICalPropertyScribe<Geo> {
	public GeoScribe() {
		super(Geo.class, "GEO", ICalDataType.FLOAT);
	}

	@Override
	protected String _writeText(Geo property, WriteContext context) {
		ICalFloatFormatter formatter = new ICalFloatFormatter();

		Double latitude = property.getLatitude();
		if (latitude == null) {
			latitude = 0.0;
		}
		String latitudeStr = formatter.format(latitude);

		Double longitude = property.getLongitude();
		if (longitude == null) {
			longitude = 0.0;
		}
		String longitudeStr = formatter.format(longitude);

		switch (context.getVersion()) {
		case V1_0:
			return list(latitudeStr, longitudeStr);
		default:
			return structured(latitudeStr, longitudeStr);
		}
	}

	@Override
	protected Geo _parseText(String value, ICalDataType dataType, ICalParameters parameters, ParseContext context) {
		String latitudeStr, longitudeStr;
		switch (context.getVersion()) {
		case V1_0:
			Iterator<String> it1 = list(value).iterator();
			latitudeStr = it1.hasNext() ? it1.next() : null;
			longitudeStr = it1.hasNext() ? it1.next() : null;
			break;

		default:
			SemiStructuredIterator it2 = semistructured(value, true);
			latitudeStr = it2.next();
			longitudeStr = it2.next();
			break;
		}

		if (latitudeStr == null || longitudeStr == null) {
			throw new CannotParseException(20);
		}

		return parse(latitudeStr, longitudeStr);
	}

	@Override
	protected void _writeXml(Geo property, XCalElement element, WriteContext context) {
		ICalFloatFormatter formatter = new ICalFloatFormatter();

		Double latitude = property.getLatitude();
		if (latitude == null) {
			latitude = 0.0;
		}
		element.append("latitude", formatter.format(latitude));

		Double longitude = property.getLongitude();
		if (longitude == null) {
			longitude = 0.0;
		}
		element.append("longitude", formatter.format(longitude));
	}

	@Override
	protected Geo _parseXml(XCalElement element, ICalParameters parameters, ParseContext context) {
		String latitudeStr = element.first("latitude");
		String longitudeStr = element.first("longitude");
		if (latitudeStr == null && longitudeStr == null) {
			throw missingXmlElements("latitude", "longitude");
		}
		if (latitudeStr == null) {
			throw missingXmlElements("latitude");
		}
		if (longitudeStr == null) {
			throw missingXmlElements("longitude");
		}

		return parse(latitudeStr, longitudeStr);
	}

	@Override
	protected JCalValue _writeJson(Geo property, WriteContext context) {
		Double latitude = property.getLatitude();
		if (latitude == null) {
			latitude = 0.0;
		}

		Double longitude = property.getLongitude();
		if (longitude == null) {
			longitude = 0.0;
		}

		return JCalValue.structured(latitude, longitude);
	}

	@Override
	protected Geo _parseJson(JCalValue value, ICalDataType dataType, ICalParameters parameters, ParseContext context) {
		StructuredIterator it = structured(value);
		String latitudeStr = it.nextString();
		String longitudeStr = it.nextString();
		return parse(latitudeStr, longitudeStr);
	}

	private Geo parse(String latitudeStr, String longitudeStr) {
		Double latitude = null;
		if (latitudeStr != null) {
			try {
				latitude = Double.valueOf(latitudeStr);
			} catch (NumberFormatException e) {
				throw new CannotParseException(21, latitudeStr);
			}
		}

		Double longitude = null;
		if (longitudeStr != null) {
			try {
				longitude = Double.valueOf(longitudeStr);
			} catch (NumberFormatException e) {
				throw new CannotParseException(22, longitudeStr);
			}
		}

		return new Geo(latitude, longitude);
	}
}
