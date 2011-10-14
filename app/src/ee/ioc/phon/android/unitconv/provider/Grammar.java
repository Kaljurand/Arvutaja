/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.unitconv.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class Grammar {

	public Grammar() {
	}

	public static final class Columns implements BaseColumns {
		private Columns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AppsContentProvider.AUTHORITY + "/queries");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ee.ioc.phon.android.unitconv";

		/*
		public static final String NAME = "NAME";
		public static final String DESC = "DESC";
		public static final String LANG = "LANG";
		public static final String URL = "URL";
*/
		public static final String TIMESTAMP = "TIMESTAMP";
		public static final String UTTERANCE = "UTTERANCE";
		public static final String TRANSLATION = "TRANSLATION";
		public static final String EVALUATION = "EVALUATION";
	}
}