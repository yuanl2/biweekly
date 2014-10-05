package biweekly.parameter;

import biweekly.ICalVersion;

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

 The views and conclusions contained in the software and documentation are those
 of the authors and should not be interpreted as representing official policies, 
 either expressed or implied, of the FreeBSD Project.
 */

/**
 * Represents a parameter whose values are supported by a variety of different
 * vCard versions.
 * @author Michael Angstadt
 */
public class VersionedEnumParameterValue extends EnumParameterValue {
	private static final ICalVersion allVersions[] = ICalVersion.values();
	protected final ICalVersion supportedVersions[];

	public VersionedEnumParameterValue(String value, ICalVersion... supportedVersions) {
		super(value);
		this.supportedVersions = (supportedVersions.length == 0) ? allVersions : supportedVersions;
	}

	/**
	 * Determines if the parameter value is supported by the given vCard
	 * version.
	 * @param version the vCard version
	 * @return true if it is supported, false if not
	 */
	public boolean isSupported(ICalVersion version) {
		for (ICalVersion supportedVersion : supportedVersions) {
			if (supportedVersion == version) {
				return true;
			}
		}
		return false;
	}
}
